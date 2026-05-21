package com.skyplayer.pro.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service unifié pour vérifier le statut complet d'un appareil.
 * Un seul appel API retourne : statut trial + playlist associée.
 * Endpoint: /api/devices/check
 */
@Singleton
class DeviceCheckService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE_URL = "https://skyplayerapp.xyz"
        private const val CHECK_URL = "$BASE_URL/api/devices/check"
        private const val APP_ID = "skyplayer_pro_v1"
        private const val TRIAL_DAYS = 15
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Résultat unifié de la vérification appareil
     */
    sealed class DeviceStatus {
        /**
         * Trial actif - afficher jours restants + charger playlist si présente
         */
        data class TrialActive(
            val daysRemaining: Int,
            val playlistUrl: String?,
            val playlistName: String?
        ) : DeviceStatus()

        /**
         * Premium/Activé actif - charger directement
         */
        data class PremiumActive(
            val playlistUrl: String?,
            val playlistName: String?
        ) : DeviceStatus()

        /**
         * Expiré - bloquer l'app, afficher écran paiement
         */
        data class Expired(
            val macAddress: String
        ) : DeviceStatus()

        /**
         * Erreur réseau - mode offline, vérification locale
         */
        data object Offline : DeviceStatus()
    }

    /**
     * Vérifie le statut complet de l'appareil en un seul appel.
     * Envoie MAC + ANDROID_ID pour identification unique.
     */
    suspend fun checkDeviceStatus(macAddress: String): DeviceStatus = withContext(Dispatchers.IO) {
        return@withContext try {
            // Récupérer ANDROID_ID pour anti-fraude
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            val jsonBody = JSONObject().apply {
                put("mac_address", macAddress)
                put("android_id", androidId)
                put("device_id", "$macAddress|$androidId")
                put("app_id", APP_ID)
                put("brand", android.os.Build.BRAND)
                put("model", android.os.Build.MODEL)
                put("android_version", android.os.Build.VERSION.RELEASE)
            }.toString()

            Timber.i("🔐 DeviceCheck - MAC: ${macAddress.take(8)}... AndroidID: ${androidId.take(8)}...")

            val request = Request.Builder()
                .url(CHECK_URL)
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-App-Key", APP_ID)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.w("⚠️ DeviceCheck: HTTP ${response.code}")
                return@withContext DeviceStatus.Offline
            }

            val responseBody = response.body?.string()
                ?: return@withContext DeviceStatus.Offline

            parseDeviceResponse(responseBody, macAddress)

        } catch (e: Exception) {
            Timber.w("⚠️ DeviceCheck erreur: ${e.message}")
            DeviceStatus.Offline
        }
    }

    /**
     * Parse la réponse JSON du serveur
     */
    private fun parseDeviceResponse(responseBody: String, macAddress: String): DeviceStatus {
        return try {
            val json = JSONObject(responseBody)
            val status = json.getString("status")
            val playlistUrl = json.optString("playlist_url", "").takeIf { it.isNotBlank() }
            val playlistName = json.optString("playlist_name", "").takeIf { it.isNotBlank() }

            Timber.i("✅ DeviceCheck status: $status, playlist: ${playlistName ?: "none"}")

            when (status) {
                "trial_active" -> {
                    val daysRemaining = json.optInt("days_remaining", TRIAL_DAYS)
                    DeviceStatus.TrialActive(
                        daysRemaining = daysRemaining,
                        playlistUrl = playlistUrl,
                        playlistName = playlistName
                    )
                }
                "premium_active", "activated" -> {
                    DeviceStatus.PremiumActive(
                        playlistUrl = playlistUrl,
                        playlistName = playlistName
                    )
                }
                "expired", "trial_expired" -> {
                    DeviceStatus.Expired(macAddress = macAddress)
                }
                else -> {
                    Timber.w("⚠️ Statut inconnu: $status")
                    DeviceStatus.Offline
                }
            }
        } catch (e: Exception) {
            Timber.e("❌ Erreur parsing réponse: ${e.message}")
            DeviceStatus.Offline
        }
    }
}
