package com.skyplayer.pro.data.prefetch

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.skyplayer.pro.data.model.Channel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de pré-chargement des flux pour zapping instantané
 *
 * Stratégie :
 * - Pré-charge silencieuse des flux voisins (précédent + suivant)
 * - Buffering agressif des segments initiaux (3-5s)
 * - Libération automatique des ressources hors scope
 * - Objectif : zapping < 500ms même sur 3G/4G instable
 */
@UnstableApi
@Singleton
class StreamPrefetchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFETCH_BUFFER_MS = 3000L // 3 secondes de buffer
        private const val MAX_PREFETCHED_STREAMS = 3 // Précédent, Courant, Suivant
        private const val PREFETCH_TIMEOUT_MS = 8000L // Timeout pré-chargement
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Cache des players en pré-chargement
    private val prefetchedPlayers = ConcurrentHashMap<String, PrefetchedStream>()
    
    // État du pré-chargement
    private val _prefetchState = MutableStateFlow<Map<String, PrefetchStatus>>(emptyMap())
    val prefetchState: StateFlow<Map<String, PrefetchStatus>> = _prefetchState

    // Canal actuellement visible
    private var currentChannelId: String? = null
    private var currentChannelList: List<Channel> = emptyList()

    /**
     * Met à jour la position courante et lance le pré-chargement des voisins
     */
    fun updateCurrentPosition(channel: Channel, allChannels: List<Channel>) {
        currentChannelId = channel.id
        currentChannelList = allChannels
        
        coroutineScope.launch {
            // Déterminer les voisins
            val currentIndex = allChannels.indexOfFirst { it.id == channel.id }
            if (currentIndex == -1) return@launch
            
            val neighbors = mutableListOf<Channel>()
            
            // Chaîne précédente (avec wrap-around)
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else allChannels.size - 1
            neighbors.add(allChannels[prevIndex])
            
            // Chaîne suivante (avec wrap-around)
            val nextIndex = if (currentIndex < allChannels.size - 1) currentIndex + 1 else 0
            neighbors.add(allChannels[nextIndex])
            
            Timber.d("🎯 Pré-chargement voisins de '${channel.name}':")
            neighbors.forEach { Timber.d("   → ${it.name}") }
            
            // Pré-charger les voisins
            neighbors.forEach { neighbor ->
                if (!prefetchedPlayers.containsKey(neighbor.id)) {
                    prefetchStream(neighbor)
                }
            }
            
            // Nettoyer les streams hors scope
            cleanupOutOfScopeStreams(channel.id, neighbors.map { it.id })
        }
    }

    /**
     * Pré-charge un flux en arrière-plan
     */
    private fun prefetchStream(channel: Channel) {
        // Limiter le nombre de streams pré-chargés
        if (prefetchedPlayers.size >= MAX_PREFETCHED_STREAMS) {
            cleanupOldestStream()
        }
        
        coroutineScope.launch {
            try {
                withTimeout(PREFETCH_TIMEOUT_MS) {
                    _prefetchState.value = _prefetchState.value + (channel.id to PrefetchStatus.LOADING)
                    
                    Timber.d("⏳ Pré-chargement démarré: ${channel.name}")
                    val startTime = System.currentTimeMillis()
                    
                    // Créer player silencieux
                    val player = createSilentPlayer(channel.url)
                    
                    // Attendre que le buffer soit suffisant
                    var bufferedMs = 0L
                    while (bufferedMs < PREFETCH_BUFFER_MS && isActive) {
                        delay(100)
                        bufferedMs = player.bufferedPosition - player.currentPosition
                    }
                    
                    // Mettre en pause (garder buffer)
                    player.playWhenReady = false
                    
                    // Stocker dans le cache
                    prefetchedPlayers[channel.id] = PrefetchedStream(
                        channel = channel,
                        player = player,
                        bufferedDurationMs = bufferedMs,
                        prefetchTimeMs = System.currentTimeMillis() - startTime
                    )
                    
                    _prefetchState.value = _prefetchState.value + (channel.id to PrefetchStatus.READY)
                    
                    Timber.d("✅ Pré-chargement terminé: ${channel.name} (${bufferedMs}ms buffer en ${System.currentTimeMillis() - startTime}ms)")
                }
            } catch (e: TimeoutCancellationException) {
                Timber.w("⏱️ Timeout pré-chargement: ${channel.name}")
                _prefetchState.value = _prefetchState.value + (channel.id to PrefetchStatus.TIMEOUT)
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur pré-chargement: ${channel.name}")
                _prefetchState.value = _prefetchState.value + (channel.id to PrefetchStatus.ERROR)
            }
        }
    }

    /**
     * Crée un player ExoPlayer silencieux pour pré-chargement
     */
    private fun createSilentPlayer(url: String): ExoPlayer {
        // Configurer LoadControl pour buffering agressif
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500,  // minBufferMs
                5000,  // maxBufferMs
                1000,  // bufferForPlaybackMs
                2000   // bufferForPlaybackAfterRebufferMs
            )
            .build()
        
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setLoadControl(loadControl)
            .build()
            .apply {
                // Volume à 0 (silencieux)
                volume = 0f
                
                // Charger le média
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    /**
     * Récupère un player pré-chargé (pour zapping instantané)
     * Retourne null si non pré-chargé
     */
    fun getPrefetchedPlayer(channelId: String): ExoPlayer? {
        val prefetched = prefetchedPlayers[channelId]
        return prefetched?.player?.apply {
            // Activer le son et la lecture
            volume = 1f
            playWhenReady = true
        }
    }

    /**
     * Vérifie si un canal est prêt (pré-chargé)
     */
    fun isChannelReady(channelId: String): Boolean {
        return prefetchedPlayers[channelId]?.let {
            it.player.playbackState == Player.STATE_READY
        } ?: false
    }

    /**
     * Libère un player spécifique (après utilisation)
     */
    fun releasePlayer(channelId: String) {
        prefetchedPlayers.remove(channelId)?.player?.release()
        _prefetchState.value = _prefetchState.value - channelId
    }

    /**
     * Libère tous les ressources
     */
    fun releaseAll() {
        prefetchedPlayers.values.forEach { it.player.release() }
        prefetchedPlayers.clear()
        _prefetchState.value = emptyMap()
        coroutineScope.cancel()
        Timber.i("🧹 Tous les players pré-chargés libérés")
    }

    /**
     * Nettoie les streams hors scope (pas voisins du courant)
     */
    private fun cleanupOutOfScopeStreams(currentId: String, neighborIds: List<String>) {
        val toKeep = neighborIds + currentId
        val toRemove = prefetchedPlayers.keys.filter { it !in toKeep }
        
        toRemove.forEach { id ->
            releasePlayer(id)
            Timber.d("🗑️ Nettoyage stream hors scope: $id")
        }
    }

    /**
     * Supprime le plus ancien stream pré-chargé
     */
    private fun cleanupOldestStream() {
        prefetchedPlayers.entries
            .minByOrNull { it.value.prefetchTimeMs }
            ?.let { (id, _) ->
                releasePlayer(id)
                Timber.d("🗑️ Nettoyage stream ancien: $id")
            }
    }

    /**
     * Statistiques de pré-chargement pour debug
     */
    fun getStats(): PrefetchStats {
        val ready = prefetchedPlayers.count { it.value.player.playbackState == Player.STATE_READY }
        val buffering = prefetchedPlayers.count { it.value.player.playbackState == Player.STATE_BUFFERING }
        
        return PrefetchStats(
            totalPrefetched = prefetchedPlayers.size,
            readyCount = ready,
            bufferingCount = buffering,
            averageBufferMs = prefetchedPlayers.values.map { it.bufferedDurationMs }.average().toLong()
        )
    }
}

/**
 * Données d'un stream pré-chargé
 */
data class PrefetchedStream(
    val channel: Channel,
    val player: ExoPlayer,
    val bufferedDurationMs: Long,
    val prefetchTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Statut du pré-chargement
 */
enum class PrefetchStatus {
    LOADING,
    READY,
    TIMEOUT,
    ERROR
}

/**
 * Statistiques du prefetcher
 */
data class PrefetchStats(
    val totalPrefetched: Int,
    val readyCount: Int,
    val bufferingCount: Int,
    val averageBufferMs: Long
)
