package com.skyplayer.pro.data.remote

import androidx.annotation.Keep
import com.skyplayer.pro.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Client unique du backend Sky-player — Firebase Functions via Firebase Hosting rewrites.
 *
 * Les endpoints sont servis par Firebase Hosting (skyplayerapp.xyz) et redirigés
 * vers les Cloud Functions correspondantes. La clé API est validée côté serveur
 * via le secret ACTIVATION_API_KEY.
 *
 * Endpoints :
 *  - POST /api/devices/check      → statut appareil (trial / premium / expiré) + playlist liée
 *  - GET  /api/v1/playlist/{mac}  → playlist assignée à une adresse MAC
 *  - GET  /api/mac/check/{mac}    → statut d'activation d'une MAC
 */
@Keep
interface SkyPlayerBackendApi {

    /**
     * Statut complet d'un appareil en un seul appel.
     */
    @POST("api/devices/check")
    suspend fun checkDevice(
        @Body body: DeviceCheckRequest,
        @Header("X-App-Key") appKey: String = DEVICE_APP_ID,
        @Header("X-Activation-API-Key") activationKey: String = BuildConfig.LICENSE_API_KEY
    ): Response<DeviceCheckResponse>

    /**
     * Vérifie si une playlist est assignée à cette adresse MAC.
     */
    @GET("api/v1/playlist/{mac}")
    suspend fun getMacPlaylist(
        @Path("mac") mac: String,
        @Header("X-App-Key") appKey: String = PLAYLIST_APP_KEY,
        @Header("X-Activation-API-Key") activationKey: String = BuildConfig.LICENSE_API_KEY
    ): Response<MacPlaylistResponse>

    /**
     * Vérifie le statut d'activation d'une MAC (source de vérité serveur).
     */
    @GET("api/mac/check/{mac}")
    suspend fun checkMac(
        @Path("mac") mac: String,
        @Header("X-Activation-API-Key") activationKey: String = BuildConfig.LICENSE_API_KEY
    ): Response<MacCheckResponse>

    companion object {
        /** URL du backend — Firebase Hosting redirige vers les Cloud Functions */
        const val BASE_URL = BuildConfig.BACKEND_BASE_URL

        /**
         * Clés X-App-Key par endpoint (conservées pour compatibilité historique).
         * Le backend Firebase les utilise pour identifier la génération d'essai.
         */
        const val DEVICE_APP_ID = "skyplayer_pro_v2"
        const val PLAYLIST_APP_KEY = "skyplayer_pro"
    }
}

/**
 * Corps de POST /api/devices/check — identification matérielle + logicielle.
 */
data class DeviceCheckRequest(
    @SerializedName("mac_address") val macAddress: String,
    @SerializedName("android_id") val androidId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("app_id") val appId: String,
    val brand: String,
    val model: String,
    @SerializedName("android_version") val androidVersion: String
)

/**
 * Réponse de POST /api/devices/check.
 */
@androidx.annotation.Keep
data class DeviceCheckResponse(
    val status: String? = null,
    @SerializedName("days_remaining") val daysRemaining: Int? = null,
    @SerializedName("playlist_url") val playlistUrl: String? = null,
    @SerializedName("playlist_name") val playlistName: String? = null,
    val type: String? = "m3u",
    @SerializedName("xtream_username") val xtreamUsername: String? = null,
    @SerializedName("xtream_password") val xtreamPassword: String? = null,
    @SerializedName("xtream_server_url", alternate = ["xtream_host", "xtreamServer"])
    val xtreamServerUrl: String? = null
)

/**
 * Réponse de GET /api/v1/playlist/{mac} — compat snake_case et camelCase.
 */
@androidx.annotation.Keep
data class MacPlaylistResponse(
    val active: Boolean? = null,
    val message: String? = null,
    val name: String? = null,
    @SerializedName("playlist_url", alternate = ["playlistUrl"])
    val playlistUrl: String? = null,
    @SerializedName("xtream_host", alternate = ["xtreamServer"])
    val xtreamHost: String? = null,
    @SerializedName("xtream_username", alternate = ["xtreamUser"])
    val xtreamUsername: String? = null,
    @SerializedName("xtream_password", alternate = ["xtreamPassword"])
    val xtreamPassword: String? = null,
    @SerializedName("expire") val expire: String? = null
)

/**
 * Réponse de GET /api/mac/check/{mac} :
 * `{ active: true, activation: {…} }` ou `{ active: false, error: "…" }`
 */
@androidx.annotation.Keep
data class MacCheckResponse(
    val active: Boolean,
    val activation: Map<String, Any>? = null,
    val error: String? = null
)
