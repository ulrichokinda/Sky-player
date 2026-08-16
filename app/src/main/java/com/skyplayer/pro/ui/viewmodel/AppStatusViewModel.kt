package com.skyplayer.pro.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.PlaylistRepository
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class AppNetworkStatus {
    Checking, Wifi, Cellular, Connected, Offline
}

data class AppStatusUiState(
    val networkStatus: AppNetworkStatus = AppNetworkStatus.Checking,
    val licenseLabel: String = "",
    val licenseColor: Color = PremiumEmerald,
    val licenseWarning: Boolean = false,
    val channelCount: Int = 0,
    val playlistName: String = "",
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0
)

/**
 * État global de confiance affiché dans le bandeau de statut.
 * Partagé entre Dashboard et sections de contenu.
 */
@HiltViewModel
class AppStatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val licenseManager: LicenseManager,
    private val channelRepository: ChannelRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppStatusUiState())
    val uiState: StateFlow<AppStatusUiState> = _uiState.asStateFlow()

    init {
        refreshLicenseInfo()
        refreshPlaylistInfo()
        refreshChannelCount()
        startNetworkMonitoring()
    }

    fun refresh() {
        refreshLicenseInfo()
        refreshPlaylistInfo()
        refreshChannelCount()
    }

    fun setSyncing(active: Boolean, progress: Int = 0) {
        _uiState.update { it.copy(isSyncing = active, syncProgress = progress) }
    }

    private fun startNetworkMonitoring() {
        viewModelScope.launch {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

            while (isActive) {
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

                val status = when {
                    activeNetwork == null || caps == null -> AppNetworkStatus.Offline
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> AppNetworkStatus.Wifi
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> AppNetworkStatus.Cellular
                    else -> AppNetworkStatus.Connected
                }
                _uiState.update { it.copy(networkStatus = status) }
                delay(5_000)
            }
        }
    }

    private fun refreshLicenseInfo() {
        val info = licenseManager.getLicenseInfo()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val (label, color, warning) = when {
            info.isActivated -> Triple(
                "✓ Abonnement actif",
                SuccessGreen,
                false
            )
            info.isTrialValid -> {
                val days = info.trialDaysRemaining
                val expiry = Date(info.installDate + (LicenseManager.TRIAL_DAYS.toLong() * 24 * 60 * 60 * 1000))
                Triple(
                    "Essai : $days j — expire le ${dateFormat.format(expiry)}",
                    if (days <= 3) WarningOrange else PremiumGold,
                    days <= 3
                )
            }
            else -> Triple(
                "⚠ Activation requise",
                WarningOrange,
                true
            )
        }

        _uiState.update {
            it.copy(licenseLabel = label, licenseColor = color, licenseWarning = warning)
        }
    }

    private fun refreshPlaylistInfo() {
        viewModelScope.launch {
            try {
                val playlists = playlistRepository.getAllPlaylists().first()
                val active = playlists.firstOrNull { it.isActive } ?: playlists.firstOrNull()
                _uiState.update { it.copy(playlistName = active?.name ?: "") }
            } catch (_: Exception) {
                _uiState.update { it.copy(playlistName = "") }
            }
        }
    }

    private fun refreshChannelCount() {
        viewModelScope.launch {
            try {
                val count = channelRepository.getChannelCount()
                _uiState.update { it.copy(channelCount = count) }
            } catch (_: Exception) {
                _uiState.update { it.copy(channelCount = 0) }
            }
        }
    }
}
