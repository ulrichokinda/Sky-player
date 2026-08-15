package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.firebase.RemoteConfigManager
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.model.RemoteConfig
import com.skyplayer.pro.data.model.RemoteConfigState
import com.skyplayer.pro.data.repository.PlaylistLoadProgress
import com.skyplayer.pro.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel expert pour configuration à distance
 * Gère sauvegarde sécurisée et chargement des playlists
 */
@HiltViewModel
class RemoteConfigViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val remoteConfigManager: RemoteConfigManager,
    private val playlistRepository: PlaylistRepository,
    private val encryptedPrefs: EncryptedPrefs
) : ViewModel() {
    
    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()
    
    private val _qrUrl = MutableStateFlow("")
    val qrUrl: StateFlow<String> = _qrUrl.asStateFlow()
    
    val configState: StateFlow<RemoteConfigState> = remoteConfigManager.configState
    
    // Exposer les événements pour l'UI
    val events: SharedFlow<String> = remoteConfigManager.events
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        initialize()
    }
    
    private fun initialize() {
        viewModelScope.launch {
            val macId = licenseManager.getDeviceId()
            _deviceId.value = macId
            _qrUrl.value = remoteConfigManager.generateQrUrl(macId)
            
            // Démarrer l'écoute Firebase
            remoteConfigManager.startListening(macId)
        }
    }
    
    /**
     * Traite une config reçue : sauvegarde sécurisée + chargement
     */
    fun applyRemoteConfig(config: RemoteConfig, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                when (config) {
                    is RemoteConfig.XtreamConfig -> {
                        saveXtreamConfig(config)
                        loadXtreamPlaylist(config, onComplete)
                    }
                    is RemoteConfig.M3uConfig -> {
                        saveM3uConfig(config)
                        loadM3uPlaylist(config, onComplete)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur application config")
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sauvegarde sécurisée Xtream dans EncryptedSharedPreferences
     */
    private suspend fun saveXtreamConfig(config: RemoteConfig.XtreamConfig) {
        withContext(Dispatchers.IO) {
            encryptedPrefs.saveXtreamCredentials(
                host = config.host,
                username = config.user,
                password = config.pass
            )
            Timber.i("🔒 Credentials Xtream sauvegardés (chiffrés)")
        }
    }
    
    /**
     * Sauvegarde sécurisée M3U
     */
    private suspend fun saveM3uConfig(config: RemoteConfig.M3uConfig) {
        withContext(Dispatchers.IO) {
            encryptedPrefs.saveString("m3u_url", config.url)
            encryptedPrefs.saveString("m3u_name", config.name)
            Timber.i("🔒 URL M3U sauvegardée (chiffrée)")
        }
    }
    
    /**
     * Charge la playlist Xtream et notifie le succès
     */
    private fun loadXtreamPlaylist(
        config: RemoteConfig.XtreamConfig,
        onComplete: () -> Unit
    ) {
        val playlistUrl = config.toPlaylistUrl()
        val playlistName = "Xtream ${config.user}"
        
        viewModelScope.launch {
            try {
                // Sauvegarder dans Room
                playlistRepository.addPlaylist(
                    name = playlistName,
                    url = playlistUrl,
                    type = "XTREAM",
                    username = config.user,
                    password = config.pass,
                    serverUrl = config.host
                ).collect { progress ->
                    when (progress) {
                        is PlaylistLoadProgress.Loading -> {
                            Timber.i("⏳ Chargement Xtream: ${progress.message}")
                        }
                        is PlaylistLoadProgress.Success -> {
                            // Confirmer l'application
                            remoteConfigManager.confirmApplied(playlistName)
                            Timber.i("✅ Playlist Xtream chargée: $playlistName")
                            onComplete()
                            _isLoading.value = false
                        }
                        is PlaylistLoadProgress.Error -> {
                            Timber.e(progress.exception, "❌ Erreur chargement Xtream")
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur chargement Xtream")
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Charge la playlist M3U et notifie le succès
     */
    private fun loadM3uPlaylist(
        config: RemoteConfig.M3uConfig,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                playlistRepository.addPlaylist(
                    name = config.name,
                    url = config.url,
                    type = "M3U"
                ).collect { progress ->
                    when (progress) {
                        is PlaylistLoadProgress.Loading -> {
                            Timber.i("⏳ Chargement M3U: ${progress.message}")
                        }
                        is PlaylistLoadProgress.Success -> {
                            remoteConfigManager.confirmApplied(config.name)
                            Timber.i("✅ Playlist M3U chargée: ${config.name}")
                            onComplete()
                            _isLoading.value = false
                        }
                        is PlaylistLoadProgress.Error -> {
                            Timber.e(progress.exception, "❌ Erreur chargement M3U")
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur chargement M3U")
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Régénère l'URL QR (si besoin refresh)
     */
    fun refreshQrCode() {
        _qrUrl.value = remoteConfigManager.generateQrUrl(_deviceId.value)
    }
    
    /**
     * Redémarre l'écoute après erreur
     */
    fun retryConnection() {
        remoteConfigManager.reset()
        remoteConfigManager.startListening(_deviceId.value)
    }
    
    override fun onCleared() {
        super.onCleared()
        remoteConfigManager.stopListening()
        Timber.i("🛑 RemoteConfigViewModel cleared")
    }
}
