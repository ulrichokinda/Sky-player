package com.skyplayer.pro.ui.screens.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.remote.DeviceCheckService
import com.skyplayer.pro.data.remote.DownloadProgress
import com.skyplayer.pro.data.remote.MacPlaylistInfo
import com.skyplayer.pro.data.remote.MacPlaylistService
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.LicenseRepository
import com.skyplayer.pro.data.repository.PlaylistRepository
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    private val channelRepository: ChannelRepository,
    private val macPlaylistService: MacPlaylistService,
    private val deviceCheckService: DeviceCheckService,
    private val licenseRepository: LicenseRepository
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

    private val _expiryColor = MutableStateFlow(PremiumEmerald)
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

    // Sync status
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _expiryDateFormatted = MutableStateFlow("")
    val expiryDateFormatted: StateFlow<String> = _expiryDateFormatted.asStateFlow()

    /** Derniers contenus regardés (Live, VOD, Séries) */
    val recentlyWatched: StateFlow<List<Channel>> = channelRepository.getAllRecentlyWatched(limit = 8)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Playlist détectée
    private var detectedPlaylistInfo: MacPlaylistInfo? = null

    // ═══════════════════════════════════════════════════════════════
    // INITIALISATION UNIFIÉE (UN SEUL APPEL API + RÉFRESH PÉRIODIQUE)
    // ═══════════════════════════════════════════════════════════════

    init {
        // getDeviceId() fait de l'I/O (EncryptedSharedPreferences + fichiers système) :
        // on le sort du thread principal pour éviter tout jank/ANR au premier lancement.
        viewModelScope.launch {
            val deviceId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                licenseManager.getDeviceId()
            }
            _deviceId.value = deviceId
            Timber.i("DashboardViewModel init")

            // 1. Vérification initiale du statut
            checkDeviceStatus(deviceId)

            // 2. Vérification périodique du statut
            launch {
                while (true) {
                    kotlinx.coroutines.delay(60_000)
                    Timber.i("🔄 Actualisation périodique du statut...")
                    if (!_isChecking.value && !_isSyncing.value && _macPlaylistStatus.value is MacPlaylistStatus.None) {
                        checkDeviceStatus(deviceId)
                    }
                }
            }

            // 3. Écoute Firestore en temps réel (source de vérité : backend Sky-player)
            licenseRepository.observeActivation(deviceId).collect { activation ->
                handleFirestoreActivation(activation)
            }
        }
    }

    private fun handleFirestoreActivation(activation: com.skyplayer.pro.data.repository.FirestoreActivation?) {
        if (activation == null) {
            Timber.d("ℹ️ No Firestore activation")
            return
        }

        Timber.i("📄 Firestore activation received: ${activation.status}")

        if (activation.isActive()) {
            val xtreamHost = activation.getXtreamHost()
            val xtreamUser = activation.getXtreamUser()
            val xtreamPassword = activation.getXtreamPassword()
            val playlistUrl = activation.getPlaylistUrl()
            val playlistName = activation.getPlaylistName()
            val playlistType = activation.getPlaylistType()

            if (!_isSyncing.value && _macPlaylistStatus.value is MacPlaylistStatus.None) {
                if (!xtreamHost.isNullOrBlank() && !xtreamUser.isNullOrBlank() && !xtreamPassword.isNullOrBlank()) {
                    // Charger playlist Xtream depuis Firestore
                    val info = MacPlaylistInfo(
                        name = playlistName ?: "Playlist Firestore",
                        url = "",
                        type = "xtream",
                        expireDate = "",
                        xtreamUsername = xtreamUser,
                        xtreamPassword = xtreamPassword,
                        xtreamServerUrl = xtreamHost
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    startXtreamLoad(info)
                } else if (!playlistUrl.isNullOrBlank()) {
                    // Charger playlist M3U depuis Firestore
                    val info = MacPlaylistInfo(
                        name = playlistName ?: "Playlist Firestore",
                        url = playlistUrl,
                        type = playlistType,
                        expireDate = "",
                        xtreamUsername = "",
                        xtreamPassword = "",
                        xtreamServerUrl = ""
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    startDownload(info)
                }
            }
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
                _expiryDateFormatted.value = ""
                _expiryColor.value = if (result.daysRemaining <= 3) WarningOrange else PremiumGold

                if (result.playlistUrl != null || result.xtreamServerUrl != null) {
                    // SCÉNARIO A: Playlist trouvée → téléchargement ou ajout Xtream
                    val info = MacPlaylistInfo(
                        name = result.playlistName ?: "Playlist",
                        url = result.playlistUrl ?: "",
                        type = result.playlistType ?: "m3u",
                        expireDate = "",
                        xtreamUsername = result.xtreamUsername ?: "",
                        xtreamPassword = result.xtreamPassword ?: "",
                        xtreamServerUrl = result.xtreamServerUrl ?: ""
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    
                    if (info.type == "xtream" && info.xtreamServerUrl.isNotBlank() && info.xtreamUsername.isNotBlank()) {
                        startXtreamLoad(info)
                    } else {
                        startDownload(info)
                    }
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
                _expiryDateFormatted.value = ""
                _expiryColor.value = SuccessGreen

                if (result.playlistUrl != null || result.xtreamServerUrl != null) {
                    // SCÉNARIO A: Playlist trouvée → téléchargement ou ajout Xtream
                    val info = MacPlaylistInfo(
                        name = result.playlistName ?: "Playlist",
                        url = result.playlistUrl ?: "",
                        type = result.playlistType ?: "m3u",
                        expireDate = "",
                        xtreamUsername = result.xtreamUsername ?: "",
                        xtreamPassword = result.xtreamPassword ?: "",
                        xtreamServerUrl = result.xtreamServerUrl ?: ""
                    )
                    detectedPlaylistInfo = info
                    _macPlaylistStatus.value = MacPlaylistStatus.Found(info)
                    
                    if (info.type == "xtream" && info.xtreamServerUrl.isNotBlank() && info.xtreamUsername.isNotBlank()) {
                        startXtreamLoad(info)
                    } else {
                        startDownload(info)
                    }
                } else {
                    // SCÉNARIO B: Aucune playlist → Dashboard
                    _macPlaylistStatus.value = MacPlaylistStatus.None
                    _isChecking.value = false
                    loadPlaylistInfo()
                }
            }
            is DeviceCheckService.DeviceStatus.Expired -> {
                // ⛔ BLOQUÉ - Non activé
                _trialStatus.value = TrialStatus.Expired
                _expiryLabel.value = "⚠ Application non activée"
                _expiryColor.value = WarningOrange
                _isChecking.value = false
                Timber.w("⛔ Non activé - Accès bloqué")
            }
            is DeviceCheckService.DeviceStatus.Offline -> {
                // Fallback local
                handleOfflineFallback(deviceId)
            }
        }
    }

    private suspend fun handleOfflineFallback(deviceId: String) {
        // Complément : seconde source serveur via GET /api/mac/check/{mac}
        // Si le POST /api/devices/check échoue mais que la MAC est active,
        // on accorde l'accès au lieu de retomber immédiatement en local.
        val backendResult = licenseRepository.checkAccess(deviceId)
        if (backendResult.isSuccess && backendResult.getOrNull()?.active == true) {
            Timber.i("✅ Accès confirmé via /api/mac/check - MAC active")
            licenseManager.setActivatedLocally(true)
            _trialStatus.value = TrialStatus.Activated
            _expiryLabel.value = "✓ Abonnement actif"
            _expiryDateFormatted.value = ""
            _expiryColor.value = SuccessGreen
            // Récupérer la playlist via GET /api/v1/playlist/{mac}
            checkMacPlaylistFallback(deviceId)
            return
        }

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
                val expiryDate = Date(info.installDate + (LicenseManager.TRIAL_DAYS.toLong() * 24 * 60 * 60 * 1000))
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
                
                if (info.type == "xtream" && info.xtreamServerUrl.isNotBlank() && info.xtreamUsername.isNotBlank()) {
                    startXtreamLoad(info)
                } else {
                    startDownload(info)
                }
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
    // CHARGEMENT PLAYLIST (M3U ou XTREAM)
    // ═══════════════════════════════════════════════════════════════

    private fun startDownload(info: MacPlaylistInfo) {
        viewModelScope.launch {
            Timber.i("⬇️ Démarrage téléchargement M3U: ${info.name}")
            _isSyncing.value = true
            _syncProgress.value = 0f
            _downloadError.value = null

            playlistRepository.addM3UPlaylist(info.name, info.url).collect { loadProgress ->
                when (loadProgress) {
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Loading -> {
                        _downloadProgress.value = DownloadProgress.Downloading(
                            readBytes = (loadProgress.progress ?: 0f).toLong(),
                            totalBytes = if (loadProgress.progress != null) 100L else -1L
                        )
                        _syncProgress.value = loadProgress.progress ?: 0f
                        Timber.d("📥 Téléchargement: ${loadProgress.message}")
                    }
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Success -> {
                        _channelCount.value = loadProgress.channelCount
                        _downloadComplete.value = true
                        _isChecking.value = false
                        _isSyncing.value = false
                        _syncProgress.value = 1f
                        Timber.i("✅ Téléchargement terminé - ${loadProgress.channelCount} chaînes")
                    }
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Error -> {
                        _downloadError.value = loadProgress.exception.message ?: "Erreur inconnue"
                        _isChecking.value = false
                        _isSyncing.value = false
                        _macPlaylistStatus.value = MacPlaylistStatus.None // Retour au Dashboard
                        Timber.e(loadProgress.exception, "❌ Erreur téléchargement")
                    }
                }
            }
        }
    }

    private fun startXtreamLoad(info: MacPlaylistInfo) {
        viewModelScope.launch {
            Timber.i("📺 Démarrage chargement Xtream: ${info.name}")
            _isSyncing.value = true
            _syncProgress.value = 0f
            _downloadError.value = null

            playlistRepository.addXtreamPlaylist(
                name = info.name,
                username = info.xtreamUsername,
                password = info.xtreamPassword,
                serverUrl = info.xtreamServerUrl
            ).collect { loadProgress ->
                when (loadProgress) {
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Loading -> {
                        _downloadProgress.value = DownloadProgress.Downloading(
                            readBytes = (loadProgress.progress ?: 0f).toLong(),
                            totalBytes = if (loadProgress.progress != null) 100L else -1L
                        )
                        _syncProgress.value = loadProgress.progress ?: 0f
                        Timber.d("📺 Chargement Xtream: ${loadProgress.message}")
                    }
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Success -> {
                        _channelCount.value = loadProgress.channelCount
                        _downloadComplete.value = true
                        _isChecking.value = false
                        _isSyncing.value = false
                        _syncProgress.value = 1f
                        Timber.i("✅ Playlist Xtream chargée - ${loadProgress.channelCount} chaînes")
                    }
                    is com.skyplayer.pro.data.repository.PlaylistLoadProgress.Error -> {
                        _downloadError.value = loadProgress.exception.message ?: "Erreur inconnue"
                        _isChecking.value = false
                        _isSyncing.value = false
                        _macPlaylistStatus.value = MacPlaylistStatus.None
                        Timber.e(loadProgress.exception, "❌ Erreur chargement Xtream")
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
            if (info.type == "xtream" && info.xtreamServerUrl.isNotBlank() && info.xtreamUsername.isNotBlank()) {
                startXtreamLoad(info)
            } else {
                startDownload(info)
            }
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

    private fun maskDeviceId(id: String): String {
        if (id.length <= 8) return id
        return id.take(4) + "****" + id.takeLast(4)
    }
}