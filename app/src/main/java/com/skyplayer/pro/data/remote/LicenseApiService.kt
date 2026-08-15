package com.skyplayer.pro.data.remote

import com.skyplayer.pro.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * API Service pour le backend Sky-player (https://github.com/ulrichokinda/Sky-player)
 *
 * Endpoints :
 * - GET /api/mac/check/{mac} → Vérifier le statut d'une MAC (auth `X-Activation-API-Key`)
 */
interface LicenseApiService {

    companion object {
        // URL du backend déployé (injectée via BuildConfig)
        const val BASE_URL = BuildConfig.BACKEND_BASE_URL

        // Clé d'activation (injectée via BuildConfig)
        // Doit correspondre à ACTIVATION_API_KEY du backend Sky-player
        const val API_KEY = BuildConfig.LICENSE_API_KEY
    }

    /**
     * Vérifier le statut d'une MAC sur le backend Sky-player
     */
    @GET("api/mac/check/{mac}")
    suspend fun checkMac(
        @Path("mac") mac: String,
        @Header("X-Activation-API-Key") apiKey: String = API_KEY
    ): Response<MacCheckResponse>
}

/**
 * Réponse du backend pour GET /api/mac/check/{mac} :
 * `{ active: true, activation: {…} }` ou `{ active: false, error: "…" }`
 */
@androidx.annotation.Keep
data class MacCheckResponse(
    val active: Boolean,
    // Document d'activation Firestore (schéma variable côté serveur → Map)
    val activation: Map<String, Any>? = null,
    val error: String? = null
)
