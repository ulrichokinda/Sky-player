package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.remote.LicenseApiService
import com.skyplayer.pro.data.remote.MacCheckResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository backend Sky-player : vérifie le statut d'une MAC via GET /api/mac/check/{mac}
 */
@Singleton
class LicenseBackendRepository @Inject constructor(
    private val licenseManager: LicenseManager,
    private val apiService: LicenseApiService
) {
    /**
     * Vérifie le statut d'une MAC sur le backend Sky-player
     */
    suspend fun checkMacStatus(mac: String = licenseManager.getDeviceId()): Result<MacCheckResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.checkMac(mac)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        Timber.i("✅ Backend: MAC $mac active=${data.active}")
                        Result.success(data)
                    } else {
                        Result.failure(Exception("Réponse vide du backend"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                    Timber.e("❌ Backend erreur: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception backend")
                Result.failure(e)
            }
        }

    /**
     * Vérifie l'accès complet via le backend (source de vérité).
     * Le statut actif est décidé côté serveur (expiryDate dans Firestore).
     * En cas d'échec réseau, on retombe sur la vérification locale (mode offline).
     */
    suspend fun validateAccess(): AccessValidationResult {
        val backendResult = checkMacStatus()

        return if (backendResult.isSuccess) {
            val active = backendResult.getOrNull()?.active == true
            AccessValidationResult(
                hasAccess = active,
                isTrialActive = false,
                isActivated = active,
                trialDaysRemaining = if (active) licenseManager.getTrialDaysRemaining() else 0,
                deviceId = licenseManager.getDeviceId()
            )
        } else {
            // Fallback sur les données locales si backend inaccessible
            val localTrialValid = licenseManager.isTrialValid()
            AccessValidationResult(
                hasAccess = licenseManager.hasValidAccess(),
                isTrialActive = localTrialValid,
                isActivated = licenseManager.isActivatedLocally(),
                trialDaysRemaining = licenseManager.getTrialDaysRemaining(),
                deviceId = licenseManager.getDeviceId()
            )
        }
    }
}
