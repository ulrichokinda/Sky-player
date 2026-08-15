package com.skyplayer.pro.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.repository.PlaylistLoadProgress
import com.skyplayer.pro.data.repository.PlaylistRepository
import com.skyplayer.pro.util.XtreamUrlNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'ajout de playlists
 */
@HiltViewModel
class AddPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val encryptedPrefs: EncryptedPrefs
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _progressMessage = MutableStateFlow<String?>(null)
    val progressMessage: StateFlow<String?> = _progressMessage.asStateFlow()

    private val _progressPercent = MutableStateFlow<Float?>(null)
    val progressPercent: StateFlow<Float?> = _progressPercent.asStateFlow()

    /**
     * Ajoute une playlist M3U
     */
    fun addM3UPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isSuccess.value = false
            _progressMessage.value = null
            _progressPercent.value = null

            try {
                // Validation URL IPTV (HTTP/HTTPS)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    _error.value = "L'URL doit commencer par http:// ou https://"
                    _isLoading.value = false
                    return@launch
                }

                playlistRepository.addM3UPlaylist(name, url).collect { progress ->
                    when (progress) {
                        is PlaylistLoadProgress.Loading -> {
                            _progressMessage.value = progress.message
                            _progressPercent.value = progress.progress
                        }
                        is PlaylistLoadProgress.Success -> {
                            encryptedPrefs.setOnboardingCompleted()
                            _isSuccess.value = true
                            _isLoading.value = false
                        }
                        is PlaylistLoadProgress.Error -> {
                            Timber.e(progress.exception, "Erreur ajout playlist M3U")
                            _error.value = progress.exception.message ?: "Erreur lors de l'ajout de la playlist"
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception ajout playlist M3U")
                _error.value = e.message ?: "Une erreur est survenue"
                _isLoading.value = false
            }
        }
    }

    /**
     * Ajoute une playlist Xtream Codes
     */
    fun addXtreamPlaylist(name: String, username: String, password: String, serverUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isSuccess.value = false
            _progressMessage.value = null
            _progressPercent.value = null

            try {
                val normalized = XtreamUrlNormalizer.normalize(
                    rawInput = serverUrl,
                    fallbackUsername = username,
                    fallbackPassword = password
                )
                val resolvedUsername = normalized.username?.takeIf { it.isNotBlank() }
                    ?: run {
                        _error.value = "Nom d'utilisateur Xtream manquant"
                        _isLoading.value = false
                        return@launch
                    }
                val resolvedPassword = normalized.password?.takeIf { it.isNotBlank() }
                    ?: run {
                        _error.value = "Mot de passe Xtream manquant"
                        _isLoading.value = false
                        return@launch
                    }

                playlistRepository.addXtreamPlaylist(
                    name = name,
                    username = resolvedUsername,
                    password = resolvedPassword,
                    serverUrl = normalized.serverUrl
                ).collect { progress ->
                    when (progress) {
                        is PlaylistLoadProgress.Loading -> {
                            _progressMessage.value = progress.message
                            _progressPercent.value = progress.progress
                        }
                        is PlaylistLoadProgress.Success -> {
                            encryptedPrefs.setOnboardingCompleted()
                            _isSuccess.value = true
                            _isLoading.value = false
                        }
                        is PlaylistLoadProgress.Error -> {
                            Timber.e(progress.exception, "Erreur ajout playlist Xtream")
                            _error.value = progress.exception.message ?: "Erreur lors de la connexion au serveur"
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception ajout playlist Xtream")
                _error.value = e.message ?: "Une erreur est survenue"
                _isLoading.value = false
            }
        }
    }
}
