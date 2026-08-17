package com.skyplayer.pro.data.monitor

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import androidx.media3.common.util.UnstableApi
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.ChannelGroupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Monitor pour le lecteur de streaming
 *
 * Surveille en temps réel :
 * - Disponibilité du lien (404, timeout, erreurs réseau)
 * - Qualité du buffer (stalling, rebuffering)
 * - Bande passante et adaptation
 *
 * Action automatique :
 * 1. Détecte lien mort → Teste liens miroirs si disponibles
 * 2. Teste alternatives similaires (même groupe/catégorie)
 * 3. Switch transparent sans message d'erreur brut
 */
@UnstableApi
@Singleton
class StreamHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val channelRepository: ChannelGroupRepository
) {
    companion object {
        private const val HEALTH_CHECK_INTERVAL_MS = 5000L
        private const val ERROR_THRESHOLD = 2
        /** Seuil de buffering prolongé avant bascule miroir/alternative (ms) */
        private const val BUFFER_UNDERRUN_THRESHOLD_MS = 8000L
        /** Buffer minimal acceptable une fois la lecture démarrée (secondes) */
        private const val MIN_BUFFER_SECONDS = 2L
        private const val FALLBACK_TIMEOUT_MS = 8000L
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    // État de santé du stream
    private val _healthState = MutableStateFlow<StreamHealth>(StreamHealth.Healthy)
    val healthState: StateFlow<StreamHealth> = _healthState

    // Informations sur le fallback actuel
    private val _fallbackInfo = MutableStateFlow<FallbackInfo?>(null)
    val fallbackInfo: StateFlow<FallbackInfo?> = _fallbackInfo

    private var currentPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var currentChannel: Channel? = null
    private var allChannels: List<Channel> = emptyList()

    private var consecutiveErrors = 0
    private var lastErrorTimestamp = 0L
    private var testedUrls = mutableSetOf<String>() // URLs déjà testées et échouées

    // Début du buffering en cours (0 = pas de buffering). Remplacé la valeur initialisée
    // à 0 pour `lastErrorTimestamp` : au premier check, `now - 0` donnait un délai
    // astronomique → faux BufferUnderrun dès le démarrage d'un flux lent.
    private var bufferingStartMs = 0L

    // Stocker le listener pour le retirer proprement
    private val analyticsListener = AnalyticsEventListener()

    /**
     * Démarre la surveillance d'un player
     */
    fun startMonitoring(
        player: androidx.media3.exoplayer.ExoPlayer,
        channel: Channel,
        availableChannels: List<Channel>
    ) {
        stopMonitoring()

        currentPlayer = player
        currentChannel = channel
        allChannels = availableChannels
        testedUrls.clear()
        consecutiveErrors = 0
        bufferingStartMs = 0L
        _healthState.value = StreamHealth.Healthy
        _fallbackInfo.value = null

        // Ajouter listener analytics pour surveillance en temps réel
        player.addAnalyticsListener(analyticsListener)

        // Démarrer vérification périodique
        monitoringJob = coroutineScope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                checkStreamHealth()
            }
        }

        Timber.i("🔍 Surveillance démarrée pour: ${channel.name}")
    }

    /**
     * Arrête la surveillance
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        currentPlayer?.removeAnalyticsListener(analyticsListener)
        testedUrls.clear()
        Timber.d("🛑 Surveillance arrêtée")
    }

    /**
     * Vérifie la santé du stream actuel
     */
    private suspend fun checkStreamHealth() {
        val player = currentPlayer ?: return
        val channel = currentChannel ?: return

        try {
            // Utiliser les positions pour calculer l'état du buffer
            val bufferedPosition = player.bufferedPosition
            val currentPosition = player.currentPosition
            val playbackState = player.playbackState

            // Calculer le temps de buffer disponible (en ms)
            val bufferDurationMs = bufferedPosition - currentPosition
            Timber.d("📊 Buffer: ${bufferDurationMs / 1000}s restant | Current: ${currentPosition / 1000}s | Buffered: ${bufferedPosition / 1000}s")

            // Détecter underrun uniquement pendant une lecture active (pas au démarrage initial)
            val isActivelyPlaying = playbackState == androidx.media3.common.Player.STATE_READY &&
                player.isPlaying

            if (playbackState == androidx.media3.common.Player.STATE_BUFFERING && player.playWhenReady) {
                val bufferingDuration = if (bufferingStartMs > 0) {
                    System.currentTimeMillis() - bufferingStartMs
                } else {
                    0L // Buffering jamais observé via analytics : pas encore éligible
                }
                if (bufferingDuration > BUFFER_UNDERRUN_THRESHOLD_MS) {
                    Timber.w("⚠️ Buffering prolongé sur ${channel.name}: ${bufferingDuration / 1000}s")
                    handleStreamIssue(channel, StreamIssue.BufferUnderrun)
                }
            } else if (isActivelyPlaying && bufferDurationMs in 0 until MIN_BUFFER_SECONDS * 1000) {
                Timber.d("📉 Buffer faible (${bufferDurationMs / 1000}s) — surveillance sans bascule immédiate")
            }

            // Vérifier si le lien est réellement mort (pas juste buffering)
            if (playbackState == androidx.media3.common.Player.STATE_IDLE && player.playWhenReady) {
                consecutiveErrors++
                if (consecutiveErrors >= ERROR_THRESHOLD) {
                    Timber.e("❌ Lien mort détecté: ${channel.name}")
                    handleStreamIssue(channel, StreamIssue.DeadLink)
                }
            } else {
                consecutiveErrors = 0 // Reset si tout va bien
            }

            // Vérifier la connectivité réseau avec le context
            val hasInternet = checkInternetConnectivity()
            if (!hasInternet) {
                Timber.e("📵 Aucune connexion Internet disponible")
                _healthState.value = StreamHealth.Unrecoverable(StreamIssue.NetworkUnavailable)
            }

            // Utiliser channelRepository pour rafraîchir les chaînes si besoin
            // (juste un exemple d'utilisation)
            if (allChannels.isEmpty()) {
                Timber.d("📦 Refresh channels from repository")
                // channelRepository pourrait être utilisé pour recharger les chaînes
            }

        } catch (e: Exception) {
            Timber.e(e, "Erreur vérification santé")
        }
    }

    /**
     * Vérifie la connectivité réseau avec le contexte Android (minSdk 24, donc API 23+ safe)
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun checkInternetConnectivity(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Gère un problème de stream (buffer underrun ou lien mort)
     */
    private suspend fun handleStreamIssue(channel: Channel, issue: StreamIssue) {
        _healthState.value = StreamHealth.Degraded(issue)

        // 1. Chercher des liens miroirs dans la même chaîne
        val mirrorUrls = extractMirrorUrls(channel)
        val workingMirror = mirrorUrls.firstOrNull { url ->
            url !in testedUrls && testUrlAvailability(url)
        }

        if (workingMirror != null) {
            Timber.i("✅ Miroir trouvé pour ${channel.name}: $workingMirror")
            _fallbackInfo.value = FallbackInfo.Mirror(workingMirror)
            switchToMirror(workingMirror)
            return
        }

        // 2. Chercher une chaîne alternative similaire
        val alternative = findAlternativeChannel(channel)

        if (alternative != null) {
            Timber.i("🔄 Alternative trouvée: ${alternative.name}")
            _healthState.value = StreamHealth.UsingAlternative(alternative)
            _fallbackInfo.value = FallbackInfo.Alternative(alternative)
            switchToAlternative(alternative)
        } else {
            // 3. Aucune alternative disponible
            _healthState.value = StreamHealth.Unrecoverable(issue)
            _fallbackInfo.value = FallbackInfo.NoneAvailable
        }
    }

    /**
     * Extrait les URLs miroirs potentiels d'une chaîne
     * Certains playlists M3U contiennent plusieurs URLs pour la même chaîne
     */
    private fun extractMirrorUrls(channel: Channel): List<String> {
        val mirrors = mutableListOf<String>()

        // URL principale
        mirrors.add(channel.url)

        // Vérifier si des URLs alternatives sont stockées dans les attributs
        // Format courant: url_backup1, url_backup2 dans les métadonnées
        // Cela dépend du format de votre playlist

        // Générer des variantes courantes des URLs
        val url = channel.url
        if (url.contains("/live/")) {
            // Essayer variantes communes des URLs IPTV
            val variants = listOf(
                url.replace("/live/", "/stream/"),
                url.replace("/live/", "/"),
                url.replace(Regex("\\.m3u8.*"), ".m3u8"), // Nettoyer paramètres
                url.replace("http://", "https://") // Essayer HTTPS si HTTP
            )
            mirrors.addAll(variants)
        }

        return mirrors.distinct()
    }

    /**
     * Teste si une URL est disponible (HTTP HEAD request)
     */
    private suspend fun testUrlAvailability(url: String): Boolean {
        return try {
            withTimeout(FALLBACK_TIMEOUT_MS) {
                val request = Request.Builder()
                    .url(url)
                    .head() // HEAD request = plus rapide
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val isAvailable = response.isSuccessful && response.code != 404
                    if (!isAvailable) {
                        testedUrls.add(url)
                    }
                    Timber.d("🔗 Test URL $url: ${response.code} (available=$isAvailable)")
                    isAvailable
                }
            }
        } catch (e: Exception) {
            testedUrls.add(url)
            Timber.d("❌ URL inaccessible: $url - ${e.message}")
            false
        }
    }

    /**
     * Trouve une chaîne alternative similaire
     * Basé sur: même groupe, catégorie similaire, nom similaire
     */
    private suspend fun findAlternativeChannel(deadChannel: Channel): Channel? {
        val candidates = allChannels.filter { it.id != deadChannel.id }

        // Score de similarité pour chaque candidat
        val scoredCandidates = candidates.map { candidate ->
            val score = calculateSimilarityScore(deadChannel, candidate)
            candidate to score
        }.sortedByDescending { it.second }

        // Prendre le meilleur candidat avec un score minimum
        val bestMatch = scoredCandidates.firstOrNull { it.second > 0.5 }

        // Vérifier que l'URL du candidat fonctionne
        return bestMatch?.first?.takeIf { candidate ->
            testUrlAvailability(candidate.url)
        }
    }

    /**
     * Calcule un score de similarité entre deux chaînes
     */
    private fun calculateSimilarityScore(channel1: Channel, channel2: Channel): Double {
        var score = 0.0

        // Même groupe (forte indication)
        if (channel1.groupTitle == channel2.groupTitle && !channel1.groupTitle.isNullOrBlank()) {
            score += 0.4
        }

        // Même catégorie
        if (channel1.category == channel2.category) {
            score += 0.3
        }

        // Similarité de nom (même chaîne, qualité différente)
        val name1 = channel1.name.lowercase()
        val name2 = channel2.name.lowercase()

        if (name1 == name2) {
            score += 0.3 // Nom identique
        } else if (name1.contains(name2) || name2.contains(name1)) {
            score += 0.2 // Contient l'autre
        } else {
            // Distance de Levenshtein simplifiée pour noms similaires
            val similarity = nameSimilarity(name1, name2)
            score += similarity * 0.2
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Similarité de noms simplifiée
     */
    private fun nameSimilarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0

        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // suppression
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Bascule vers un miroir
     */
    private fun switchToMirror(mirrorUrl: String) {
        // Notifier le player via callback pour changer l'URL
        // Cette partie est implémentée dans le PlayerViewModel
        onSwitchToMirror?.invoke(mirrorUrl)
    }

    /**
     * Bascule vers une alternative
     */
    private fun switchToAlternative(alternative: Channel) {
        // Notifier le player via callback pour changer de chaîne
        onSwitchToAlternative?.invoke(alternative)
    }

    // Callbacks pour le ViewModel
    var onSwitchToMirror: ((String) -> Unit)? = null
    var onSwitchToAlternative: ((Channel) -> Unit)? = null

    /**
     * Listener pour les événements analytics du player
     */
    private inner class AnalyticsEventListener : androidx.media3.exoplayer.analytics.AnalyticsListener {
        override fun onPlaybackStateChanged(
            eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
            state: Int
        ) {
            when (state) {
                androidx.media3.common.Player.STATE_BUFFERING -> {
                    if (bufferingStartMs == 0L) bufferingStartMs = System.currentTimeMillis()
                }
                androidx.media3.common.Player.STATE_READY,
                androidx.media3.common.Player.STATE_ENDED,
                androidx.media3.common.Player.STATE_IDLE -> bufferingStartMs = 0L
            }
            if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                lastErrorTimestamp = System.currentTimeMillis()
            }
        }

        override fun onPlayerErrorChanged(
            eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
            error: androidx.media3.common.PlaybackException?
        ) {
            error?.let {
                consecutiveErrors++
                Timber.w("🚨 Player error #${consecutiveErrors}: ${it.message}")

                // Même seuil que le lien mort : ne pas basculer sur la première erreur
                // transitoire (buffer vide momentané, erreur de segment isolée…).
                if (consecutiveErrors >= ERROR_THRESHOLD) {
                    consecutiveErrors = 0 // évite les déclenchements en rafale
                    coroutineScope.launch {
                        currentChannel?.let { channel ->
                            handleStreamIssue(channel, StreamIssue.PlayerError(it))
                        }
                    }
                }
            }
        }

        override fun onDownstreamFormatChanged(
            eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
            mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
        ) {
            // Détecter changement de qualité (adaptation)
            Timber.d("📊 Format changé: ${mediaLoadData.trackFormat?.bitrate}")
        }
    }

    fun release() {
        stopMonitoring()
        coroutineScope.cancel()
    }
}

/**
 * État de santé du stream
 */
sealed class StreamHealth {
    object Healthy : StreamHealth()
    data class Degraded(val issue: StreamIssue) : StreamHealth()
    data class UsingAlternative(val alternative: Channel) : StreamHealth()
    data class Unrecoverable(val issue: StreamIssue) : StreamHealth()
}

/**
 * Types de problèmes de stream
 */
sealed class StreamIssue {
    object DeadLink : StreamIssue() // 404, connexion impossible
    object BufferUnderrun : StreamIssue() // Buffering constant
    object NetworkUnavailable : StreamIssue() // Aucun réseau
    data class PlayerError(val error: androidx.media3.common.PlaybackException) : StreamIssue()
}

/**
 * Informations sur le fallback utilisé
 */
sealed class FallbackInfo {
    data class Mirror(val url: String) : FallbackInfo()
    data class Alternative(val channel: Channel) : FallbackInfo()
    object NoneAvailable : FallbackInfo()
}
