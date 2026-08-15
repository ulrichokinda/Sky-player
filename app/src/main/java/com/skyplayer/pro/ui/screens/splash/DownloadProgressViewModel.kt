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
import kotlin.math.roundToInt

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

    // Variables for speed and ETA
    private var startTime: Long = System.currentTimeMillis()
    private var lastBytes: Long = 0L
    private var lastTime: Long = System.currentTimeMillis()

    // Infos playlist courante (injectées depuis SplashViewModel via startDownload)
    private var currentPlaylistInfo: MacPlaylistInfo? = null

    /**
     * Lance le téléchargement pour la playlist détectée par check_mac.
     * Appelée depuis SplashScreen dès que la navigation vers cet écran est déclenchée.
     */
    fun startDownload(info: MacPlaylistInfo) {
        currentPlaylistInfo = info
        startTime = System.currentTimeMillis()
        lastBytes = 0L
        lastTime = System.currentTimeMillis()
        _uiState.update { 
            it.copy(
                playlistName = info.name, 
                error = null, 
                isComplete = false,
                speedKbps = 0f,
                etaSeconds = 0
            ) 
        }
        doDownload(info)
    }

    fun retry() {
        currentPlaylistInfo?.let { doDownload(it) }
    }

    private fun doDownload(info: MacPlaylistInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            startTime = System.currentTimeMillis()
            lastBytes = 0L
            lastTime = System.currentTimeMillis()

            macPlaylistService.downloadPlaylistWithProgress(info.url)
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Downloading -> {
                            // Calculate current speed in kbps
                            val currentTime = System.currentTimeMillis()
                            val timeDiff = (currentTime - lastTime) / 1000f // seconds
                            val byteDiff = progress.readBytes - lastBytes
                            val speedKbps = if (timeDiff > 0 && byteDiff > 0) {
                                (byteDiff * 8) / (timeDiff * 1000) // bits per second / 1000 = kbps
                            } else {
                                _uiState.value.speedKbps
                            }
                            
                            // Estimate ETA
                            val etaSeconds = if (progress.totalBytes > 0 && speedKbps > 0) {
                                val remainingBytes = progress.totalBytes - progress.readBytes
                                val remainingBits = remainingBytes * 8
                                (remainingBits / (speedKbps * 1000)).roundToInt()
                            } else {
                                0
                            }

                            lastBytes = progress.readBytes
                            lastTime = currentTime
                            
                            _uiState.update { 
                                it.copy(
                                    progress = progress,
                                    speedKbps = speedKbps,
                                    etaSeconds = etaSeconds
                                ) 
                            }
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
                            _uiState.update { it.copy(error = getFriendlyError(progress.error)) }
                            Timber.e("❌ Téléchargement échoué: ${progress.error}")
                        }
                    }
                }
        }
    }

    private fun getFriendlyError(originalError: String?): String {
        return when {
            originalError == null -> "Un problème est survenu lors du chargement."
            originalError.contains("HTTP") || originalError.contains("Erreur réseau") -> "Connexion réseau instable. Vérifiez votre accès internet."
            originalError.contains("Corps de réponse vide") || originalError.contains("JSON") -> "Playlist temporairement indisponible. Réessayez dans quelques minutes."
            else -> "Erreur inattendue. Veuillez réessayer."
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
    val error: String? = null,
    val speedKbps: Float = 0f,
    val etaSeconds: Int = 0
)
