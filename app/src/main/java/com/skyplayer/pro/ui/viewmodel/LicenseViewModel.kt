package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseInfo
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.license.LicenseSecurityManager
import com.skyplayer.pro.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour gérer la licence, l'activation et le statut de l'appareil
 */
@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val licenseRepository: LicenseRepository,
    val licenseSecurityManager: LicenseSecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        refreshLicenseStatus()
    }

    /**
     * Rafraîchit les informations de licence depuis le local et le serveur
     */
    fun refreshLicenseStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val deviceId = licenseManager.getDeviceId()
            val localInfo = licenseManager.getLicenseInfo()
            
            // Tenter de rafraîchir depuis le serveur (Firebase)
            try {
                licenseRepository.checkActivationStatus()
            } catch (e: Exception) {
                Timber.w("Impossible de rafraîchir la licence depuis le serveur: ${e.message}")
            }

            val updatedInfo = licenseManager.getLicenseInfo()
            
            _uiState.update {
                it.copy(
                    deviceId = deviceId,
                    licenseInfo = updatedInfo,
                    isActivated = updatedInfo.isActivated,
                    hasValidAccess = updatedInfo.hasAccess(),
                    showTrialExpired = !updatedInfo.isTrialValid && !updatedInfo.isActivated,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Effectue un check de santé des services
     */
    fun performHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulation ou appel réel au repository pour check health
            refreshLicenseStatus()
        }
    }

    /**
     * État de l'UI pour l'écran de licence
     */
    data class LicenseUiState(
        val deviceId: String = "",
        val licenseInfo: LicenseInfo? = null,
        val isActivated: Boolean = false,
        val isFirebaseConnected: Boolean = true,
        val hasValidAccess: Boolean = false,
        val showTrialExpired: Boolean = false,
        val isLoading: Boolean = false,
        val healthCheckResult: HealthCheckResult? = null
    )

    data class HealthCheckResult(
        val success: Boolean,
        val message: String
    )
}
