package com.skyplayer.pro.data.remote

import com.skyplayer.pro.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * API Retrofit pour le backend Sky-player.
 *
 * Endpoints consommés :
 *  - GET /api/mac/check/{mac}  → vérifie si la MAC est active
 *  - GET /api/health           → santé du serveur
 *
 * L'authentification se fait via le header X-Activation-API-Key
 * dont la valeur vient de BuildConfig.LICENSE_API_KEY.
 */
interface LicenseApiService {

    companion object {
        /** URL de base du backend — injectée via BuildConfig */
        val BASE_URL: String
            get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/') + "/"
    }

    /**
     * Vérifie le statut d'une MAC sur le backend.
     * Le header `Date` de la réponse est utilisé comme heure serveur (anti-triche).
     */
    @GET("api/mac/check/{mac}")
    suspend fun checkMac(
        @Path("mac") macAddress: String,
        @Header("X-Activation-API-Key") apiKey: String = BuildConfig.LICENSE_API_KEY
    ): Response<MacCheckResponse>

    /**
     * Vérifie la santé du backend.
     */
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthCheckResponse>
}

/**
 * Réponse de GET /api/mac/check/{mac}
 */
data class MacCheckResponse(
    val active: Boolean,
    val error: String? = null,
    val activation: ActivationData? = null
)

data class ActivationData(
    val id: String? = null,
    val target_mac: String? = null,
    val status: String? = null,
    val expiryDate: String? = null,
    val playlist_url: String? = null,
    val xtream_host: String? = null,
    val xtream_username: String? = null,
    val xtream_password: String? = null
)

data class HealthCheckResponse(
    val status: String,
    val version: String,
    val uptime: Long,
    val firestore: String,
    val timestamp: String
)
