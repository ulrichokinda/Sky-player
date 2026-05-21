package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseInfo
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.license.LicenseSecurityManager
import com.skyplayer.pro.data.license.ServerValidationResult
import com.skyplayer.pro.data.repository.AccessValidationResult
import com.skyplayer.pro.data.repository.HealthCheckResult
import com.skyplayer.pro.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour la gestion de la licence
 * Gère l'état de l'UI et les interactions avec le système de licence
 */
@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val licenseRepository: LicenseRepository,
    val licenseSecurityManager: LicenseSecurityManager
) : ViewModel() {

    /**
     * État de l'écran de licence
     */
    data class LicenseUiState(
        val isLoading: Boolean = true,
        val licenseInfo: LicenseInfo? = null,
        val isActivated: Boolean = false,
        val hasValidAccess: Boolean = false,
        val showTrialExpired: Boolean = false,
        val healthCheckResult: HealthCheckResult? = null,
        val errorMessage: String? = null,
        val deviceId: String = "",
        val isFirebaseConnected: Boolean = false
    ) {
        /**
         * Détermine quel écran afficher
         */
        fun shouldShowLicenseScreen(): Boolean {
            return !hasValidAccess || !isActivated && licenseInfo?.isTrialValid == false
        }
    }

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        initializeLicense()
    }

    /**
     * Initialise la licence au démarrage
     * - Récupère les infos locales
     * - Enregistre l'appareil dans Firebase
     * - Vérifie le statut d'activation
     */
    private fun initializeLicense() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. Récupérer les infos locales
                val licenseInfo = licenseManager.getLicenseInfo()
                val deviceId = licenseInfo.deviceId
                
                Timber.i("🚀 Initialisation licence - Device ID: $deviceId")
                
                _uiState.update {
                    it.copy(
                        licenseInfo = licenseInfo,
                        deviceId = deviceId,
                        isLoading = false
                    )
                }
                
                // 2. Enregistrer dans Firebase (silencieux)
                licenseRepository.registerDevice()
                
                // 3. Vérifier le statut d'activation distant
                syncActivationStatus()
                
                // 4. Démarrer l'observation en temps réel
                observeActivationChanges()
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur initialisation licence")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur lors de l'initialisation: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Synchronise le statut d'activation avec Firebase
     */
    fun syncActivationStatus() {
        viewModelScope.launch {
            try {
                val result = licenseRepository.checkActivationStatus()
                val isActivated = result.getOrDefault(false)
                
                val validation = licenseRepository.validateAccess()
                
                _uiState.update {
                    it.copy(
                        isActivated = isActivated,
                        hasValidAccess = validation.hasAccess,
                        showTrialExpired = !validation.hasAccess,
                        isFirebaseConnected = result.isSuccess
                    )
                }
                
                Timber.i("🔄 Sync activation - Activé: $isActivated, Accès: ${validation.hasAccess}")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur sync activation")
                // En cas d'erreur Firebase, on garde les données locales
                val localAccess = licenseManager.hasValidAccess()
                _uiState.update {
                    it.copy(
                        hasValidAccess = localAccess,
                        isFirebaseConnected = false
                    )
                }
            }
        }
    }

    /**
     * Observe les changements d'activation en temps réel
     */
    private fun observeActivationChanges() {
        viewModelScope.launch {
            licenseRepository.observeActivationStatus().collect { isActivated ->
                Timber.i("📡 Statut d'activation mis à jour: $isActivated")
                
                _uiState.update { state ->
                    state.copy(
                        isActivated = isActivated,
                        hasValidAccess = state.licenseInfo?.isTrialValid == true || isActivated,
                        showTrialExpired = !state.licenseInfo?.isTrialValid!! && !isActivated
                    )
                }
            }
        }
    }

    /**
     * Effectue un health check de la connexion Firebase
     */
    fun performHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = licenseRepository.performHealthCheck()
            
            result.fold(
                onSuccess = { healthResult ->
                    _uiState.update {
                        it.copy(
                            healthCheckResult = healthResult,
                            isFirebaseConnected = healthResult.success,
                            isLoading = false
                        )
                    }
                    Timber.i("🏥 Health Check réussi: ${healthResult.message}")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            healthCheckResult = HealthCheckResult(
                                success = false,
                                timestamp = System.currentTimeMillis(),
                                message = "Erreur: ${error.message}"
                            ),
                            isFirebaseConnected = false,
                            isLoading = false
                        )
                    }
                    Timber.e(error, "❌ Health Check échoué")
                }
            )
        }
    }

    /**
     * Vérifie si l'utilisateur peut accéder au lecteur (avec validation serveur)
     * @return true si accès autorisé (essai valide ou activé)
     */
    fun canAccessPlayer(): Boolean {
        return _uiState.value.hasValidAccess
    }
    
    /**
     * Vérifie l'accès avec validation serveur (anti-triche)
     * Cette fonction est appelée avant de démarrer la lecture
     */
    suspend fun validateAccessServerSide(): ServerValidationResult {
        return licenseSecurityManager.validateAccessWithServerTime()
    }
    
    /**
     * Démarre la surveillance temps réel de la licence pendant la lecture
     */
    fun startLicenseMonitoring() {
        licenseSecurityManager.startLicenseMonitoring()
    }
    
    /**
     * Arrête la surveillance de la licence
     */
    fun stopLicenseMonitoring() {
        licenseSecurityManager.stopLicenseMonitoring()
    }

    /**
     * Force une réactualisation des données de licence
     */
    fun refreshLicenseStatus() {
        syncActivationStatus()
    }

    /**
     * Réinitialise la licence (pour tests uniquement)
     */
    fun resetLicenseForTesting() {
        viewModelScope.launch {
            licenseManager.resetLicense()
            initializeLicense()
            Timber.w("⚠️ Licence réinitialisée pour test")
        }
    }

    /**
     * Simule une date d'installation passée pour tester le blocage
     * À utiliser uniquement en DEBUG
     */
    fun simulateExpiredTrial() {
        // Cette méthode est intentionnellement vide - le test se fait via
        // une modification manuelle des SharedPreferences ou via un flag de debug
        Timber.w("🧪 Pour simuler un essai expiré, modifiez la date dans les préférences chiffrées")
    }

    /**
     * Nettoie les ressources
     */
    override fun onCleared() {
        super.onCleared()
        Timber.d("👋 LicenseViewModel cleared")
    }
}
