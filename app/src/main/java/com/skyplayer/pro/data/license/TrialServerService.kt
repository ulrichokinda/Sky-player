package com.skyplayer.pro.data.license

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service de vérification du trial via un endpoint REST distant (skyplayerapp.xyz).
 * Anti-fraude: Utilise MAC + ANDROID_ID pour identifier l'appareil de manière unique.
 *
 * Protocole:
 *  POST https://skyplayerapp.xyz/api/trial/check
 *  Body JSON: { 
 *    "mac_address": "XX:XX:...",     // Adresse MAC physique
 *    "android_id": "...",            // ANDROID_ID unique
 *    "device_id": "combined_hash",   // Identifiant combiné
 *    "app": "skyplayer_pro" 
 *  }
 *
 *  Réponse attendue:
 *  {
 *    "status": "trial_active" | "trial_expired" | "activated" | "unknown",
 *    "days_remaining": 12,          // si trial_active
 *    "expiry_timestamp": 1716000000 // timestamp Unix
 *  }
 */
@Singleton
class TrialServerService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val licenseManager: LicenseManager
) {
    companion object {
        private const val CHECK_URL = "https://skyplayerapp.xyz/api/trial/check"
        private const val REGISTER_URL = "https://skyplayerapp.xyz/api/trial/register"
        private const val APP_ID   = "skyplayer_pro"
        private const val TIMEOUT_SECONDS = 10L
    }

    // Client dédié avec timeout court pour ne pas bloquer l'UI
    private val httpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Vérifie l'état du trial/activation auprès du serveur distant.
     * Envoie MAC + ANDROID_ID pour identification unique anti-fraude.
     * Retourne [TrialCheckResult.Offline] en cas d'échec réseau (fail-open côté serveur).
     */
    suspend fun checkTrialStatus(deviceId: String): TrialCheckResult = withContext(Dispatchers.IO) {
        return@withContext try {
            // Récupérer ANDROID_ID pour anti-fraude
            val context = licenseManager.javaClass.getDeclaredField("context").apply { isAccessible = true }.get(licenseManager) as android.content.Context
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            // La MAC est déjà dans le deviceId (format généré par LicenseManager)
            val macAddress = deviceId

            val bodyJson = JSONObject().apply {
                put("mac_address", macAddress)
                put("android_id", androidId)
                put("device_id", "$macAddress|$androidId") // Identifiant unique combiné
                put("app", APP_ID)
                put("install_date", licenseManager.getInstallDate())
                put("brand", android.os.Build.BRAND)
                put("model", android.os.Build.MODEL)
                put("android_version", android.os.Build.VERSION.RELEASE)
                put("sdk_int", android.os.Build.VERSION.SDK_INT)
            }.toString()

            Timber.i("🔐 Envoi check trial - MAC: ${macAddress.take(8)}... AndroidID: ${androidId.take(8)}...")

            val request = Request.Builder()
                .url(CHECK_URL)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-App-Key", APP_ID)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.w("⚠️ Serveur trial: code HTTP ${response.code}")
                return@withContext TrialCheckResult.Offline
            }

            val rawBody = response.body?.string() ?: return@withContext TrialCheckResult.Offline
            parseServerResponse(rawBody)

        } catch (e: Exception) {
            Timber.w("⚠️ Impossible de joindre le serveur trial: ${e.message}")
            // Fail-open: si le serveur est injoignable, on se fie au contrôle local
            TrialCheckResult.Offline
        }
    }

    /**
     * Enregistre l'appareil sur le serveur (premier lancement).
     * Envoie MAC + ANDROID_ID pour enregistrement unique anti-fraude.
     * Appel feu-et-oubli — ne bloque pas le démarrage.
     */
    suspend fun registerDevice(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Récupérer ANDROID_ID pour anti-fraude
            val context = licenseManager.javaClass.getDeclaredField("context").apply { isAccessible = true }.get(licenseManager) as android.content.Context
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            val macAddress = deviceId

            val bodyJson = JSONObject().apply {
                put("mac_address", macAddress)
                put("android_id", androidId)
                put("device_id", "$macAddress|$androidId") // Identifiant unique combiné
                put("app", APP_ID)
                put("install_date", licenseManager.getInstallDate())
                put("brand", android.os.Build.BRAND)
                put("model", android.os.Build.MODEL)
                put("android_version", android.os.Build.VERSION.RELEASE)
                put("sdk_int", android.os.Build.VERSION.SDK_INT)
            }.toString()

            Timber.i("📱 Enregistrement serveur - MAC: ${macAddress.take(8)}... AndroidID: ${androidId.take(8)}...")

            val request = Request.Builder()
                .url(REGISTER_URL)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-App-Key", APP_ID)
                .build()

            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            Timber.i("📱 Enregistrement serveur: ${if (success) "OK" else "ÉCHEC code ${response.code}"}")
            success
        } catch (e: Exception) {
            Timber.w("⚠️ Enregistrement serveur impossible: ${e.message}")
            false
        }
    }

    private fun parseServerResponse(json: String): TrialCheckResult {
        return try {
            val obj = JSONObject(json)
            val status = obj.optString("status", "unknown")
            val daysRemaining = obj.optInt("days_remaining", 0)
            val expiryTimestamp = obj.optLong("expiry_timestamp", 0L)

            when (status) {
                "activated"     -> TrialCheckResult.Activated
                "trial_active"  -> TrialCheckResult.TrialActive(daysRemaining, expiryTimestamp)
                "trial_expired" -> TrialCheckResult.TrialExpired
                else            -> TrialCheckResult.Offline
            }.also { Timber.i("🌐 Réponse serveur trial: status=$status daysLeft=$daysRemaining") }
        } catch (e: Exception) {
            Timber.e(e, "❌ Parsing réponse serveur trial échoué")
            TrialCheckResult.Offline
        }
    }
}

/**
 * États possibles renvoyés par le serveur de vérification du trial.
 */
sealed class TrialCheckResult {
    /** Le serveur confirme que l'appareil a un abonnement actif. */
    object Activated : TrialCheckResult()

    /** Le serveur confirme que le trial est encore en cours. */
    data class TrialActive(val daysRemaining: Int, val expiryTimestamp: Long) : TrialCheckResult()

    /** Le serveur confirme que le trial de 15 jours est expiré. */
    object TrialExpired : TrialCheckResult()

    /** Pas de réseau ou serveur injoignable — on bascule sur la vérification locale. */
    object Offline : TrialCheckResult()
}
