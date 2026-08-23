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
 * Client unique du backend Sky-player — dédié à la gestion des clients, des panels,
 * des achats de crédit et au provisionnement des playlists synchronisées avec l'app.
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

    /**
     * Heartbeat — informe le serveur de la chaîne en cours + timestamp.
     */
    @POST("api/activations/heartbeat")
    suspend fun sendHeartbeat(
        @Body body: HeartbeatRequest,
        @Header("X-Activation-API-Key") activationKey: String = BuildConfig.LICENSE_API_KEY
    ): Response<HeartbeatResponse>

    companion object {
        /** URL du backend déployé (injectée via BuildConfig) */
        val BASE_URL: String
            get() = BuildConfig.BACKEND_BASE_URL

        /** Clés X-App-Key par endpoint */
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
@Keep
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
/**
 * Corps de POST /api/activations/heartbeat
 */
data class HeartbeatRequest(
    val mac: String,
    val system: String = "Android ${android.os.Build.VERSION.RELEASE}",
    val version: String = "1.0.0-Pro",
    val country: String? = null,
    val channel: String? = null
)

/**
 * Réponse de POST /api/activations/heartbeat
 */
data class HeartbeatResponse(
    val success: Boolean = false
)

@Keep
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

