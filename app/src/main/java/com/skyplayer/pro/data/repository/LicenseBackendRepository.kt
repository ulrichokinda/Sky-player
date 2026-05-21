package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.remote.HealthCheckResponse
import com.skyplayer.pro.data.remote.LicenseApiService
import com.skyplayer.pro.data.remote.LicenseStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour communiquer avec le backend skyplayerapp.xyz
 * Remplace/complète LicenseRepository pour utiliser votre backend
 */
@Singleton
class LicenseBackendRepository @Inject constructor(
    private val licenseManager: LicenseManager
) {
    private val apiService: LicenseApiService
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(LicenseApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(LicenseApiService::class.java)
    }
    
    /**
     * Vérifie le statut de la licence via le backend
     */
    suspend fun checkLicenseStatus(): Result<LicenseStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val deviceId = licenseManager.getDeviceId()
            val response = apiService.checkLicense(deviceId)
            
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    // Mettre à jour le cache local avec le statut distant
                    licenseManager.setActivatedLocally(data.isActive)
                    
                    Timber.i("✅ Backend: Licence vérifiée - Active: ${data.isActive}, Essai: ${data.trialDaysRemaining}j")
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
     * Vérifie si le backend est accessible
     */
    suspend fun checkBackendHealth(): Result<HealthCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    Timber.i("🏥 Backend health: ${data.status}")
                    Result.success(data)
                } else {
                    Result.failure(Exception("Réponse health vide"))
                }
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Health check exception")
            Result.failure(e)
        }
    }
    
    /**
     * Vérifie l'accès complet via le backend (source de vérité)
     * La période d'essai est gérée côté serveur pour survivre aux réinstallations
     */
    suspend fun validateAccess(): AccessValidationResult {
        val backendResult = checkLicenseStatus()

        return if (backendResult.isSuccess) {
            val data = backendResult.getOrNull()!!

            // Le backend est la source de vérité pour l'essai gratuit
            // Cela permet de survivre aux réinstallations de l'app
            AccessValidationResult(
                hasAccess = data.isActive || (!data.isTrialExpired && data.trialDaysRemaining > 0),
                isTrialActive = !data.isTrialExpired && data.trialDaysRemaining > 0,
                isActivated = data.isActive,
                trialDaysRemaining = data.trialDaysRemaining,
                deviceId = data.deviceId
            )
        } else {
            // Fallback sur les données locales si backend inaccessible (mode offline)
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
