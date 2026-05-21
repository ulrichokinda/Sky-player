package com.skyplayer.pro.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.remote.DownloadProgress
import com.skyplayer.pro.data.remote.MacPlaylistInfo
import com.skyplayer.pro.data.remote.MacPlaylistService
import com.skyplayer.pro.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'écran de téléchargement progressif de la playlist.
 *
 * Reçoit les infos playlist (nom + URL) depuis le SplashViewModel
 * via SavedStateHandle, télécharge le M3U par flux et met à jour l'UI.
 */
@HiltViewModel
class DownloadProgressViewModel @Inject constructor(
    private val macPlaylistService: MacPlaylistService,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadProgressUiState())
    val uiState: StateFlow<DownloadProgressUiState> = _uiState.asStateFlow()

    // Infos playlist courante (injectées depuis SplashViewModel via startDownload)
    private var currentPlaylistInfo: MacPlaylistInfo? = null

    /**
     * Lance le téléchargement pour la playlist détectée par check_mac.
     * Appelée depuis SplashScreen dès que la navigation vers cet écran est déclenchée.
     */
    fun startDownload(info: MacPlaylistInfo) {
        currentPlaylistInfo = info
        _uiState.update { it.copy(playlistName = info.name, error = null, isComplete = false) }
        doDownload(info)
    }

    fun retry() {
        currentPlaylistInfo?.let { doDownload(it) }
    }

    private fun doDownload(info: MacPlaylistInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            macPlaylistService.downloadPlaylistWithProgress(info.url)
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Downloading -> {
                            _uiState.update { it.copy(progress = progress) }
                        }

                        is DownloadProgress.Done -> {
                            // Parse et sauvegarde en base de données
                            val channelCount = savePlaylist(info, progress.content)
                            _uiState.update {
                                it.copy(
                                    progress = DownloadProgress.Downloading(
                                        readBytes  = progress.totalBytes,
                                        totalBytes = progress.totalBytes
                                    ),
                                    isComplete   = true,
                                    channelCount = channelCount
                                )
                            }
                            Timber.i("✅ Playlist '${info.name}' importée — $channelCount chaînes")
                        }

                        is DownloadProgress.Failed -> {
                            _uiState.update { it.copy(error = progress.error) }
                            Timber.e("❌ Téléchargement échoué: ${progress.error}")
                        }
                    }
                }
        }
    }

    /**
     * Sauvegarde la playlist en base de données via PlaylistRepository.
     * @return Nombre de chaînes importées
     */
    private suspend fun savePlaylist(info: MacPlaylistInfo, m3uContent: String): Int {
        return try {
            val result = playlistRepository.addM3UPlaylistFromContent(
                name    = info.name,
                url     = info.url,
                content = m3uContent
            )
            result.getOrDefault(0)
        } catch (e: Exception) {
            Timber.e(e, "Erreur sauvegarde playlist")
            0
        }
    }
}

/**
 * État UI de l'écran de téléchargement
 */
data class DownloadProgressUiState(
    val playlistName: String = "",
    val progress: DownloadProgress.Downloading = DownloadProgress.Downloading(0, -1),
    val isComplete: Boolean = false,
    val channelCount: Int = 0,
    val error: String? = null
)
