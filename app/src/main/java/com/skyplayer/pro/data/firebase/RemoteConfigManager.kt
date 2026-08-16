package com.skyplayer.pro.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.skyplayer.pro.data.model.RemoteConfig
import com.skyplayer.pro.data.model.RemoteConfigState
import com.skyplayer.pro.data.repository.FirestoreActivation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager expert pour configuration à distance via QR Code
 *
 * Écoute Firestore — la même base que celle écrite par le backend Sky-player
 * (`POST /api/activations/link`, webhook JOBOOST, dashboard) :
 *  - collection `activations` où `target_mac == deviceId` (source de vérité),
 *  - document `devices/{mac}` (appareils en essai, sans doc d'activation).
 *
 * L'ancienne écoute RTDB `pending_configs` a été supprimée : c'était le dernier
 * maillon de l'ancienne base Firebase, plus personne n'y écrivait.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val _configState = MutableStateFlow<RemoteConfigState>(RemoteConfigState.Idle)
    val configState: StateFlow<RemoteConfigState> = _configState.asStateFlow()

    // Flow pour événements ponctuels (Toasts/Snackbars gérés par l'UI)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var activationsListener: ListenerRegistration? = null
    private var deviceListener: ListenerRegistration? = null
    private var currentDeviceId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Gestion reconnexion auto
    private var retryCount = 0
    private val maxRetries = 10
    private val retryDelays = listOf(1000L, 2000L, 5000L, 10000L, 30000L) // Exponential backoff
    private var isListening = false

    /**
     * Démarre l'écoute Firestore des activations pour un appareil
     */
    fun startListening(deviceId: String) {
        if (isListening && currentDeviceId == deviceId) {
            Timber.d("🔊 Écoute déjà active pour $deviceId")
            return
        }

        stopListening()

        currentDeviceId = deviceId
        isListening = true
        retryCount = 0

        if (!isNetworkAvailable()) {
            _configState.value = RemoteConfigState.Offline
            scheduleReconnect(deviceId)
            return
        }

        _configState.value = RemoteConfigState.Waiting

        val safeMac = deviceId.replace(":", "").lowercase()

        // 1. Collection `activations` — source de vérité du backend Sky-player
        activationsListener = firestore.collection("activations")
            .whereEqualTo("target_mac", safeMac)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ Firestore activations listener error")
                    handleListenerError(deviceId, error.message ?: "Erreur Firestore")
                    return@addSnapshotListener
                }
                val activation = snapshot?.documents?.firstOrNull()
                    ?.toObject(FirestoreActivation::class.java)
                processActivation(activation, deviceId)
            }

        // 2. Document `devices/{mac}` — appareils en essai (sans doc activation)
        deviceListener = firestore.collection("devices").document(safeMac)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ Firestore devices listener error")
                    return@addSnapshotListener
                }
                val activation = snapshot?.toObject(FirestoreActivation::class.java)
                processActivation(activation, deviceId)
            }

        Timber.i("🎧 Écoute Firestore démarrée: activations + devices/$safeMac")
    }

    /**
     * Convertit une activation Firestore en config de lecture si une playlist est liée
     */
    private fun processActivation(activation: FirestoreActivation?, deviceId: String) {
        if (activation == null) {
            Timber.d("ℹ️ Aucune activation Firestore pour $deviceId")
            return
        }

        val config = activation.toRemoteConfig()
        when {
            config is RemoteConfig.XtreamConfig && config.isValid() -> {
                Timber.i("✅ Config Xtream reçue: ${config.host}")
                handleValidConfig(config)
            }
            config is RemoteConfig.M3uConfig && config.isValid() -> {
                Timber.i("✅ Config M3U reçue: ${config.url.take(50)}...")
                handleValidConfig(config)
            }
            else -> {
                Timber.d("ℹ️ Activation sans playlist configurée")
            }
        }
    }

    /**
     * Traite une config valide : notifie l'UI et émet l'événement succès.
     * Le document Firestore n'est PAS supprimé : c'est la source de vérité,
     * il sert aussi au Dashboard et aux rechargements futurs.
     */
    private fun handleValidConfig(config: RemoteConfig) {
        _configState.value = RemoteConfigState.Received(config)
        _events.tryEmit("Configuration reçue avec succès")
    }

    /**
     * Gère les erreurs du listener Firestore (permissions, réseau…)
     */
    private fun handleListenerError(deviceId: String, message: String) {
        when {
            message.contains("denied", ignoreCase = true) ||
                message.contains("permission", ignoreCase = true) -> {
                _configState.value = RemoteConfigState.Error("Accès refusé")
            }
            else -> {
                _configState.value = RemoteConfigState.Offline
                scheduleReconnect(deviceId)
            }
        }
    }

    /**
     * Reconnexion automatique avec exponential backoff
     */
    private fun scheduleReconnect(deviceId: String) {
        if (retryCount >= maxRetries) {
            Timber.w("⛔ Max retries atteint ($maxRetries)")
            _configState.value = RemoteConfigState.Error("Connexion impossible")
            return
        }

        val delay = retryDelays.getOrElse(retryCount) { 30000L }
        retryCount++

        Timber.i("🔄 Reconnexion dans ${delay}ms (tentative $retryCount/$maxRetries)")

        mainHandler.postDelayed({
            if (isNetworkAvailable()) {
                startListening(deviceId)
            } else {
                scheduleReconnect(deviceId)
            }
        }, delay)
    }

    /**
     * Arrête proprement l'écoute
     */
    fun stopListening() {
        activationsListener?.remove()
        activationsListener = null
        deviceListener?.remove()
        deviceListener = null

        // Annuler les reconnections en attente
        mainHandler.removeCallbacksAndMessages(null)

        isListening = false
        retryCount = 0

        Timber.i("🛑 Écoute arrêtée proprement")
    }

    /**
     * Confirme l'application de la config
     */
    fun confirmApplied(playlistName: String) {
        _configState.value = RemoteConfigState.Applied(playlistName)
        _events.tryEmit("Playlist '$playlistName' chargée")
    }

    /**
     * Réinitialise l'état
     */
    fun reset() {
        _configState.value = RemoteConfigState.Idle
        retryCount = 0
    }

    /**
     * Vérifie la connectivité réseau
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Génère l'URL du QR Code
     */
    fun generateQrUrl(deviceId: String): String {
        val safeId = deviceId.replace(":", "-")
        return "https://skyplayerapp.xyz/connect?mac=$safeId"
    }
}

/**
 * Convertit une activation Firestore (écrite par le backend Sky-player)
 * en config de lecture (Xtream ou M3U) pour l'application.
 */
private fun FirestoreActivation.toRemoteConfig(): RemoteConfig? {
    val xtreamHost = getXtreamHost()
    val xtreamUser = getXtreamUser()
    val xtreamPass = getXtreamPassword()
    return when {
        !xtreamHost.isNullOrBlank() && !xtreamUser.isNullOrBlank() && !xtreamPass.isNullOrBlank() ->
            RemoteConfig.XtreamConfig(host = xtreamHost, user = xtreamUser, pass = xtreamPass)
        !getPlaylistUrl().isNullOrBlank() ->
            RemoteConfig.M3uConfig(
                url = getPlaylistUrl()!!,
                name = getPlaylistName() ?: "Playlist distante"
            )
        else -> null
    }
}
