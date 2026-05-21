package com.skyplayer.pro.ui.screens.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.remote.DeviceCheckService
import com.skyplayer.pro.data.remote.DownloadProgress
import com.skyplayer.pro.data.remote.MacPlaylistInfo
import com.skyplayer.pro.data.remote.MacPlaylistService
import com.skyplayer.pro.data.repository.PlaylistRepository
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel du Dashboard intelligent - VERSION UNIFIÉE
 * Un seul appel API retourne : statut trial + playlist associée
 * Endpoint: /api/devices/check
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val playlistRepository: PlaylistRepository,
    private val macPlaylistService: MacPlaylistService,
    private val deviceCheckService: DeviceCheckService
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════
    // ÉTATS UI
    // ═══════════════════════════════════════════════════════════════

    private val _activePlaylistName = MutableStateFlow("")
    val activePlaylistName: StateFlow<String> = _activePlaylistName.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _expiryLabel = MutableStateFlow("")
    val expiryLabel: StateFlow<String> = _expiryLabel.asStateFlow()

    private val _expiryColor = MutableStateFlow(ElectricSkyBlue)
    val expiryColor: StateFlow<Color> = _expiryColor.asStateFlow()

    // État de vérification initiale
    private val _isChecking = MutableStateFlow(true)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    // État du trial (bloquant si expiré)
    private val _trialStatus = MutableStateFlow<TrialStatus>(TrialStatus.Checking)
    val trialStatus: StateFlow<TrialStatus> = _trialStatus.asStateFlow()

    // État détection playlist MAC
    private val _macPlaylistStatus = MutableStateFlow<MacPlaylistStatus>(MacPlaylistStatus.Checking)
    val macPlaylistStatus: StateFlow<MacPlaylistStatus> = _macPlaylistStatus.asStateFlow()

    // Progression téléchargement (Scenario A)
    private val _downloadProgress = MutableStateFlow<DownloadProgress.Downloading>(
        DownloadProgress.Downloading(0, -1)
    )
    val downloadProgress: StateFlow<DownloadProgress.Downloading> = _downloadProgress.asStateFlow()

    private val _downloadComplete = MutableStateFlow(false)
    val downloadComplete: StateFlow<Boolean> = _downloadComplete.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _channelCount = MutableStateFlow(0)
    val channelCount: StateFlow<Int> = _channelCount.asStateFlow()

    // Playlist détectée
    private var detectedPlaylistInfo: MacPlaylistInfo? = null

    // ═══════════════════════════════════════════════════════════════
    // INITIALISATION UNIFIÉE (UN SEUL APPEL API)
    // ═══════════════════════════════════════════════════════════════

    init {
        viewModelScope.launch {
            val deviceId = licenseManager.getDeviceId()
            _deviceId.value = deviceId
            Timber.i("📱 DashboardViewModel init - Device ID: $deviceId")

            // Un seul appel retourne : trial + playlist
            checkDeviceStatus(deviceId)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VÉRIFICATION UNIFIÉE (TRIAL + PLAYLIST EN UN APPEL)
    // ═══════════════════════════════════════════════════════════════

    private suspend fun checkDeviceStatus(deviceId: String) {
        Timber.i("🔐 Vérification unifiée device/serveur...")

        val result = deviceCheckService.checkDeviceStatus(deviceId)
        Timber.i("🌐 Résultat unifié: $result")

        when (result) {
            is DeviceCheckService.DeviceStatus.TrialActive -> {
                // Trial actif avec ou sans playlist
                _trialStatus.value = TrialStatus.TrialActive(result.daysRemaining)
                _expiryLabel.value = "Essai: ${result.daysRemaining} j restants"
                _expiryColor.value = if (result.daysRemaining <= 3) WarningOrange else PremiumGold

                if (result.playlistUrl != null) {
                    // SCÉNARIO A: Playlist trouvée → téléchargement
                    val info = MacPlaylistInfo(
                        name = result.playlistName ?: "Playlist",
                        url = result.playlistUrl,
                        type = "m3u",
                        expireDate = "",
                        xtreamUsername = "",
                        xtreamPassword = "",
                        xtreamServerUrl = ""
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    startDownload(info)
                } else {
                    // SCÉNARIO B: Aucune playlist → Dashboard
                    _macPlaylistStatus.value = MacPlaylistStatus.None
                    _isChecking.value = false
                    loadPlaylistInfo()
                }
            }
            is DeviceCheckService.DeviceStatus.PremiumActive -> {
                // Premium/Activé avec ou sans playlist
                licenseManager.setActivatedLocally(true)
                _trialStatus.value = TrialStatus.Activated
                _expiryLabel.value = "✓ Abonnement actif"
                _expiryColor.value = SuccessGreen

                if (result.playlistUrl != null) {
                    // SCÉNARIO A: Playlist trouvée → téléchargement
                    val info = MacPlaylistInfo(
                        name = result.playlistName ?: "Playlist",
                        url = result.playlistUrl,
                        type = "m3u",
                        expireDate = "",
                        xtreamUsername = "",
                        xtreamPassword = "",
                        xtreamServerUrl = ""
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    startDownload(info)
                } else {
                    // SCÉNARIO B: Aucune playlist → Dashboard
                    _macPlaylistStatus.value = MacPlaylistStatus.None
                    _isChecking.value = false
                    loadPlaylistInfo()
                }
            }
            is DeviceCheckService.DeviceStatus.Expired -> {
                // ⛔ BLOQUÉ - Trial expiré
                _trialStatus.value = TrialStatus.Expired
                _expiryLabel.value = "⚠ Essai expiré — Activation requise"
                _expiryColor.value = WarningOrange
                _isChecking.value = false
                Timber.w("⛔ Trial expiré - Accès bloqué")
            }
            is DeviceCheckService.DeviceStatus.Offline -> {
                // Fallback local
                handleOfflineFallback(deviceId)
            }
        }
    }

    private suspend fun handleOfflineFallback(deviceId: String) {
        val hasLocalAccess = licenseManager.hasValidAccess()
        if (hasLocalAccess) {
            _trialStatus.value = TrialStatus.OfflineFallback
            loadExpiryInfoLocal()
            // Essayer quand même de chercher une playlist MAC
            checkMacPlaylistFallback(deviceId)
        } else {
            _trialStatus.value = TrialStatus.Expired
            _isChecking.value = false
            Timber.w("⛔ Offline + pas d'accès local - Accès bloqué")
        }
    }

    private fun loadExpiryInfoLocal() {
        val info = licenseManager.getLicenseInfo()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        when {
            info.isActivated -> {
                _expiryLabel.value = "✓ Abonnement actif (offline)"
                _expiryColor.value = SuccessGreen
            }
            info.isTrialValid -> {
                val daysLeft = info.trialDaysRemaining
                val expiryDate = Date(info.installDate + (15L * 24 * 60 * 60 * 1000))
                _expiryLabel.value = "Essai: $daysLeft j — expire le ${dateFormat.format(expiryDate)}"
                _expiryColor.value = if (daysLeft <= 3) WarningOrange else PremiumGold
            }
            else -> {
                _expiryLabel.value = "⚠ Essai expiré — Activation requise"
                _expiryColor.value = WarningOrange
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FALLBACK: Détection MAC si offline (optionnel)
    // ═══════════════════════════════════════════════════════════════

    private suspend fun checkMacPlaylistFallback(deviceId: String) {
        try {
            val result = macPlaylistService.checkMacPlaylist(deviceId)
            if (result is com.skyplayer.pro.data.remote.MacPlaylistResult.Active) {
                val info = result.info
                detectedPlaylistInfo = info
                _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                startDownload(info)
            } else {
                _macPlaylistStatus.value = MacPlaylistStatus.None
                _isChecking.value = false
                loadPlaylistInfo()
            }
        } catch (e: Exception) {
            _macPlaylistStatus.value = MacPlaylistStatus.None
            _isChecking.value = false
            loadPlaylistInfo()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TÉLÉCHARGEMENT PROGRESSIF (SCÉNARIO A)
    // ═══════════════════════════════════════════════════════════════

    private fun startDownload(info: MacPlaylistInfo) {
        viewModelScope.launch {
            Timber.i("⬇️ Démarrage téléchargement: ${info.name}")

            macPlaylistService.downloadPlaylistWithProgress(info.url)
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Downloading -> {
                            _downloadProgress.value = progress
                            Timber.d("📥 Téléchargement: ${progress.label}")
                        }
                        is DownloadProgress.Done -> {
                            // Sauvegarder la playlist
                            val count = savePlaylist(info, progress.content)
                            _channelCount.value = count
                            _downloadComplete.value = true
                            _isChecking.value = false
                            Timber.i("✅ Téléchargement terminé - $count chaînes")
                        }
                        is DownloadProgress.Failed -> {
                            _downloadError.value = progress.error
                            _isChecking.value = false
                            _macPlaylistStatus.value = MacPlaylistStatus.None // Retour au Dashboard
                            Timber.e("❌ Erreur téléchargement: ${progress.error}")
                        }
                    }
                }
        }
    }

    private suspend fun savePlaylist(info: MacPlaylistInfo, content: String): Int {
        return try {
            val result = playlistRepository.addM3UPlaylistFromContent(
                name = info.name,
                url = info.url,
                content = content
            )
            result.getOrDefault(0)
        } catch (e: Exception) {
            Timber.e(e, "Erreur sauvegarde playlist")
            0
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES PUBLIQUES
    // ═══════════════════════════════════════════════════════════════

    fun retryDownload() {
        detectedPlaylistInfo?.let { info ->
            _downloadError.value = null
            _downloadComplete.value = false
            startDownload(info)
        }
    }

    fun skipToDashboard() {
        _macPlaylistStatus.value = MacPlaylistStatus.None
        _isChecking.value = false
        viewModelScope.launch {
            loadPlaylistInfo()
        }
    }

    private suspend fun loadPlaylistInfo() {
        try {
            val playlists = playlistRepository.getAllPlaylists().first()
            _activePlaylistName.value = playlists.firstOrNull { it.isActive }?.name
                ?: playlists.firstOrNull()?.name
                ?: ""
            Timber.i("📋 Playlists locales: ${playlists.size}")
        } catch (e: Exception) {
            Timber.e(e, "Erreur chargement playlists")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÉTATS
    // ═══════════════════════════════════════════════════════════════

    sealed class TrialStatus {
        object Checking : TrialStatus()
        object Activated : TrialStatus()
        data class TrialActive(val daysRemaining: Int) : TrialStatus()
        object Expired : TrialStatus()
        object OfflineFallback : TrialStatus()
    }

    sealed class MacPlaylistStatus {
        object Checking : MacPlaylistStatus()
        data class Found(val info: MacPlaylistInfo) : MacPlaylistStatus()
        object None : MacPlaylistStatus()
    }
}
