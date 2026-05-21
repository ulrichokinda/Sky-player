package com.skyplayer.pro.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service pour communiquer avec le backend skyplayerapp.xyz
 * 
 * Endpoints :
 * - GET /api/license/{deviceId} → Vérifier statut licence
 * - POST /api/license/{deviceId}/activate → Activer (côté admin)
 * - POST /api/license/{deviceId}/deactivate → Désactiver (côté admin)
 * - GET /api/health → Vérifier santé backend
 */
interface LicenseApiService {
    
    companion object {
        // URL de votre backend déployé
        const val BASE_URL = "https://skyplayerapp.xyz/"
        
        // Clé API pour authentification (doit matcher côté backend)
        const val API_KEY = "AIzaSyCx3NV7zoZpazgMTABspe6fnteuYrJskp8"// ← MODIFIEZ CECI
    }
    
    /**
     * Vérifier le statut d'une licence
     */
    @GET("api/license/{deviceId}")
    suspend fun checkLicense(
        @Path("deviceId") deviceId: String,
        @Query("apiKey") apiKey: String = API_KEY
    ): Response<LicenseStatusResponse>
    
    /**
     * Vérifier la santé du backend
     */
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthCheckResponse>
}

/**
 * Réponse du backend pour le statut licence
 */
data class LicenseStatusResponse(
    val deviceId: String,
    val exists: Boolean,
    val isActive: Boolean,
    val isTrialExpired: Boolean,
    val trialDaysRemaining: Int,
    val installDate: String?,
    val activatedBy: String?,
    val activationDate: String?,
    val deviceInfo: DeviceInfoResponse?
)

/**
 * Info appareil depuis backend
 */
data class DeviceInfoResponse(
    val brand: String,
    val model: String,
    val androidVersion: String,
    val lastSeen: Long
)

/**
 * Réponse health check
 */
data class HealthCheckResponse(
    val status: String,
    val timestamp: String,
    val service: String
)

/**
 * Requête d'activation (pour admin)
 */
data class ActivateRequest(
    val activatedBy: String
)
