package com.skyplayer.pro.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.repository.PlaylistRepository
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
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    /**
     * Ajoute une playlist M3U
     */
    fun addM3UPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Validation URL IPTV (HTTP/HTTPS)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    _error.value = "L'URL doit commencer par http:// ou https://"
                    _isLoading.value = false
                    return@launch
                }

                val result = playlistRepository.addM3UPlaylist(name, url)

                result.onSuccess {
                    _isSuccess.value = true
                }.onFailure { e ->
                    Timber.e(e, "Erreur ajout playlist M3U")
                    _error.value = e.message ?: "Erreur lors de l'ajout de la playlist"
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception ajout playlist M3U")
                _error.value = e.message ?: "Une erreur est survenue"
            } finally {
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

            try {
                // Nettoyer l'URL
                val cleanUrl = serverUrl.trim().removeSuffix("/")

                if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    _error.value = "L'URL du serveur doit commencer par http:// ou https://"
                    _isLoading.value = false
                    return@launch
                }

                val result = playlistRepository.addXtreamPlaylist(
                    name = name,
                    username = username,
                    password = password,
                    serverUrl = cleanUrl
                )

                result.onSuccess {
                    _isSuccess.value = true
                }.onFailure { e ->
                    Timber.e(e, "Erreur ajout playlist Xtream")
                    _error.value = e.message ?: "Erreur lors de la connexion au serveur"
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception ajout playlist Xtream")
                _error.value = e.message ?: "Une erreur est survenue"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
