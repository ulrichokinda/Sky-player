package com.skyplayer.pro.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skyplayer.pro.data.model.RemoteConfig
import com.skyplayer.pro.data.model.RemoteConfigState
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
 * Écoute pending_configs/{deviceId} avec reconnexion automatique
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val context: Context
) {
    private val _configState = MutableStateFlow<RemoteConfigState>(RemoteConfigState.Idle)
    val configState: StateFlow<RemoteConfigState> = _configState.asStateFlow()

    // Flow pour événements ponctuels (Toasts/Snackbars gérés par l'UI)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()
    
    private var currentListener: ValueEventListener? = null
    private var currentDeviceId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Gestion reconnexion auto
    private var retryCount = 0
    private val maxRetries = 10
    private val retryDelays = listOf(1000L, 2000L, 5000L, 10000L, 30000L) // Exponential backoff
    private var isListening = false
    
    /**
     * Démarre l'écoute professionnelle de pending_configs
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
        
        val safeDeviceId = deviceId.replace(":", "-")
        val configRef = firebaseDatabase.getReference("pending_configs").child(safeDeviceId)
        
        currentListener = createSecureListener(deviceId, configRef)
        configRef.addValueEventListener(currentListener!!)
        
        Timber.i("🎧 Écoute démarrée: pending_configs/$safeDeviceId")
    }
    
    /**
     * Crée un listener sécurisé avec gestion des 2 formats
     */
    private fun createSecureListener(deviceId: String, configRef: com.google.firebase.database.DatabaseReference): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Timber.d("ℹ️ Aucune config en attente")
                    return
                }
                
                try {
                    // Lecture sécurisée des données
                    @Suppress("UNCHECKED_CAST")
                    val data = snapshot.value as? Map<String, Any>
                    val config = RemoteConfig.fromMap(data)
                    
                    if (config != null) {
                        when {
                            config is RemoteConfig.XtreamConfig && config.isValid() -> {
                                Timber.i("✅ Config Xtream reçue: ${config.host}")
                                handleValidConfig(deviceId, config, configRef)
                            }
                            config is RemoteConfig.M3uConfig && config.isValid() -> {
                                Timber.i("✅ Config M3U reçue: ${config.url.take(50)}...")
                                handleValidConfig(deviceId, config, configRef)
                            }
                            else -> {
                                Timber.w("⚠️ Config invalide reçue: $data")
                            }
                        }
                    } else {
                        Timber.w("⚠️ Format non reconnu: $data")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Erreur parsing config")
                    _events.tryEmit("Erreur format configuration")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Timber.e("❌ Firebase onCancelled: ${error.message} (code: ${error.code})")
                
                when (error.code) {
                    DatabaseError.PERMISSION_DENIED -> {
                        _configState.value = RemoteConfigState.Error("Accès refusé")
                    }
                    DatabaseError.NETWORK_ERROR -> {
                        _configState.value = RemoteConfigState.Offline
                        scheduleReconnect(deviceId)
                    }
                    else -> {
                        _configState.value = RemoteConfigState.Error("Erreur: ${error.message}")
                        scheduleReconnect(deviceId)
                    }
                }
            }
        }
    }
    
    /**
     * Traite une config valide : notifie, supprime, continue d'écouter
     */
    private fun handleValidConfig(
        deviceId: String, 
        config: RemoteConfig,
        configRef: com.google.firebase.database.DatabaseReference
    ) {
        // 1. Notifier l'UI
        _configState.value = RemoteConfigState.Received(config)
        
        // 2. Notifier événement succès
        _events.tryEmit("Configuration reçue avec succès")
        
        // 3. Supprimer de Firebase APRÈS confirmation lecture (sécurité)
        configRef.removeValue()
            .addOnSuccessListener {
                Timber.i("🗑️ Config supprimée de Firebase (sécurité)")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "⚠️ Échec suppression config")
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
        currentListener?.let { listener ->
            currentDeviceId?.let { deviceId ->
                val safeDeviceId = deviceId.replace(":", "-")
                firebaseDatabase.getReference("pending_configs").child(safeDeviceId)
                    .removeEventListener(listener)
            }
        }
        
        // Annuler les reconnections en attente
        mainHandler.removeCallbacksAndMessages(null)
        
        currentListener = null
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
