package com.skyplayer.pro.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service unifié pour vérifier le statut complet d'un appareil.
 * Un seul appel API retourne : statut trial + playlist associée.
 * Endpoint: POST /api/devices/check (via le client backend unique).
 */
@Singleton
class DeviceCheckService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendApi: SkyPlayerBackendApi,
    private val resilience: BackendResilience
) {
    companion object {
        private const val TRIAL_DAYS = 14
        private const val ENDPOINT = "devices/check"
    }

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
            val playlistName: String?,
            val playlistType: String? = "m3u",
            val xtreamUsername: String? = null,
            val xtreamPassword: String? = null,
            val xtreamServerUrl: String? = null
        ) : DeviceStatus()

        /**
         * Premium/Activé actif - charger directement
         */
        data class PremiumActive(
            val playlistUrl: String?,
            val playlistName: String?,
            val playlistType: String? = "m3u",
            val xtreamUsername: String? = null,
            val xtreamPassword: String? = null,
            val xtreamServerUrl: String? = null
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

            val hardwareFingerprint = getHardwareFingerprint()

            val request = DeviceCheckRequest(
                macAddress = macAddress,
                androidId = androidId,
                deviceId = "$macAddress|$androidId",
                appId = SkyPlayerBackendApi.DEVICE_APP_ID,
                hardwareFingerprint = hardwareFingerprint,
                brand = android.os.Build.BRAND,
                model = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE
            )

            Timber.i("🔐 DeviceCheck - MAC: ${macAddress.take(8)}... AndroidID: ${androidId.take(8)}...")

            val response = resilience.execute(ENDPOINT) {
                backendApi.checkDevice(request)
            }

            if (!response.isSuccessful) {
                Timber.w("⚠️ DeviceCheck: HTTP ${response.code()}")
                return@withContext DeviceStatus.Offline
            }

            val body = response.body()
                ?: return@withContext DeviceStatus.Offline

            parseDeviceResponse(body, macAddress)

        } catch (e: CircuitOpenException) {
            Timber.w("🚫 DeviceCheck: serveur down — ${e.retryAfterSeconds}s avant retry")
            DeviceStatus.Offline
        } catch (e: Exception) {
            Timber.w("⚠️ DeviceCheck erreur: ${e.message}")
            DeviceStatus.Offline
        }
    }

    /**
     * Interprète la réponse du serveur
     */
    private fun parseDeviceResponse(json: DeviceCheckResponse, macAddress: String): DeviceStatus {
        return try {
            val status = json.status
            val playlistUrl = json.playlistUrl?.takeIf { it.isNotBlank() }
            val playlistName = json.playlistName?.takeIf { it.isNotBlank() }
            val playlistType = json.type ?: "m3u"
            val xtreamUsername = json.xtreamUsername?.takeIf { it.isNotBlank() }
            val xtreamPassword = json.xtreamPassword?.takeIf { it.isNotBlank() }
            val xtreamServerUrl = json.xtreamServerUrl?.takeIf { it.isNotBlank() }

            Timber.i("✅ DeviceCheck status: $status, playlist: ${playlistName ?: "none"}, type: $playlistType")

            when (status) {
                "trial_active" -> DeviceStatus.TrialActive(
                    daysRemaining = json.daysRemaining ?: TRIAL_DAYS,
                    playlistUrl = playlistUrl,
                    playlistName = playlistName,
                    playlistType = playlistType,
                    xtreamUsername = xtreamUsername,
                    xtreamPassword = xtreamPassword,
                    xtreamServerUrl = xtreamServerUrl
                )
                "premium_active", "activated" -> DeviceStatus.PremiumActive(
                    playlistUrl = playlistUrl,
                    playlistName = playlistName,
                    playlistType = playlistType,
                    xtreamUsername = xtreamUsername,
                    xtreamPassword = xtreamPassword,
                    xtreamServerUrl = xtreamServerUrl
                )
                "expired", "trial_expired" -> DeviceStatus.Expired(macAddress = macAddress)
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

    /**
     * Empreinte hardware déterministe — SHA-256 des champs Build.
     * Utilisée côté serveur pour détecter les reinstalls (même hardware, nouvel ANDROID_ID).
     */
    private fun getHardwareFingerprint(): String {
        val raw = buildString {
            append(android.os.Build.BOARD)
            append("|")
            append(android.os.Build.BRAND)
            append("|")
            append(android.os.Build.DEVICE)
            append("|")
            append(android.os.Build.HARDWARE)
            append("|")
            append(android.os.Build.MANUFACTURER)
            append("|")
            append(android.os.Build.PRODUCT)
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return hash.take(16).joinToString("") { "%02x".format(it) }
    }
}
