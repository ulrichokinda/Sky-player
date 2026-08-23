package com.skyplayer.pro.data.remote

import android.content.Context
import com.skyplayer.pro.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service unifié pour vérifier le statut complet d'un appareil via Retrofit.
 * Un seul appel API retourne : statut trial + playlist associée.
 * Endpoint: POST /api/devices/check
 */
@Singleton
class DeviceCheckService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendApi: SkyPlayerBackendApi
) {
    /**
     * Résultat unifié de la vérification appareil
     */
    sealed class DeviceStatus {
        data class TrialActive(
            val daysRemaining: Int,
            val playlistUrl: String?,
            val playlistName: String?,
            val playlistType: String? = "m3u",
            val xtreamUsername: String? = null,
            val xtreamPassword: String? = null,
            val xtreamServerUrl: String? = null
        ) : DeviceStatus()

        data class PremiumActive(
            val playlistUrl: String?,
            val playlistName: String?,
            val playlistType: String? = "m3u",
            val xtreamUsername: String? = null,
            val xtreamPassword: String? = null,
            val xtreamServerUrl: String? = null
        ) : DeviceStatus()

        data class Expired(val macAddress: String) : DeviceStatus()

        data object Offline : DeviceStatus()
    }

    /**
     * Vérifie le statut complet de l'appareil via Retrofit.
     */
    suspend fun checkDeviceStatus(macAddress: String): DeviceStatus = withContext(Dispatchers.IO) {
        return@withContext try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            // Hash du device_id pour ne pas exposer la concaténation brute
            val deviceIdHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest("$macAddress|$androidId".toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(16)

            val body = DeviceCheckRequest(
                macAddress = macAddress,
                androidId = androidId,
                deviceId = deviceIdHash,
                appId = "skyplayer_pro_v2",
                brand = android.os.Build.BRAND,
                model = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE
            )

            Timber.i("🔐 DeviceCheck - MAC: ${macAddress.take(8)}...")

            val response = backendApi.checkDevice(body)

            if (!response.isSuccessful) {
                Timber.w("⚠️ DeviceCheck: HTTP ${response.code()}")
                return@withContext DeviceStatus.Offline
            }

            val data = response.body() ?: return@withContext DeviceStatus.Offline
            parseDeviceResponse(data, macAddress)
        } catch (e: Exception) {
            Timber.w("⚠️ DeviceCheck erreur: ${e.message}")
            DeviceStatus.Offline
        }
    }

    private fun parseDeviceResponse(data: DeviceCheckResponse, macAddress: String): DeviceStatus {
        val status = data.status ?: return DeviceStatus.Offline
        val playlistUrl = data.playlistUrl?.takeIf { it.isNotBlank() }
        val playlistName = data.playlistName?.takeIf { it.isNotBlank() }
        val xtreamUser = data.xtreamUsername?.takeIf { it.isNotBlank() }
        val xtreamPass = data.xtreamPassword?.takeIf { it.isNotBlank() }
        val xtreamHost = data.xtreamServerUrl?.takeIf { it.isNotBlank() }

        Timber.i("✅ DeviceCheck status: $status, playlist: ${playlistName ?: "none"}")

        return when (status) {
            "trial_active" -> DeviceStatus.TrialActive(
                daysRemaining = data.daysRemaining ?: 14,
                playlistUrl = playlistUrl,
                playlistName = playlistName,
                xtreamUsername = xtreamUser,
                xtreamPassword = xtreamPass,
                xtreamServerUrl = xtreamHost
            )
            "premium_active", "activated" -> DeviceStatus.PremiumActive(
                playlistUrl = playlistUrl,
                playlistName = playlistName,
                xtreamUsername = xtreamUser,
                xtreamPassword = xtreamPass,
                xtreamServerUrl = xtreamHost
            )
            "expired", "trial_expired" -> DeviceStatus.Expired(macAddress = macAddress)
            else -> {
                Timber.w("⚠️ Statut inconnu: $status")
                DeviceStatus.Offline
            }
        }
    }
}
