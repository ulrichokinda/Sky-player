package com.skyplayer.pro.data.license

import com.skyplayer.pro.data.repository.LicenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de sécurité des licences (backend Sky-player)
 * - Vérifie le statut serveur via GET /api/mac/check/{mac} ; l'heure du serveur
 *   provient de l'en-tête HTTP `Date` (non falsifiable côté client, anti-triche)
 * - Bloque la lecture si la licence est révoquée ou expirée
 * - Sonde périodiquement le serveur pendant la lecture
 *
 * L'ancien accès Firebase Realtime Database (serverTime/licenses) a été retiré :
 * la source de vérité est désormais le backend Sky-player.
 */
@Singleton
class LicenseSecurityManager @Inject constructor(
    private val licenseManager: LicenseManager,
    private val licenseRepository: LicenseRepository
) {
    companion object {
        private const val TRIAL_DAYS = LicenseManager.TRIAL_DAYS.toLong()
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val MONITOR_INTERVAL_MS = 60_000L
    }

    /**
     * Callback appelé lorsque la licence devient invalide pendant la lecture
     */
    interface LicenseInvalidCallback {
        fun onLicenseInvalid(reason: InvalidReason)
    }

    enum class InvalidReason {
        DEACTIVATED,           // isActive passé à false côté serveur
        TRIAL_EXPIRED,         // Essai de 14 jours terminé
        SERVER_VALIDATION_FAILED  // Erreur validation serveur
    }

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var invalidCallback: LicenseInvalidCallback? = null
    private var isMonitoring = false

    /**
     * Définit le callback pour la révocation de licence
     */
    fun setLicenseInvalidCallback(callback: LicenseInvalidCallback) {
        this.invalidCallback = callback
    }

    /**
     * Démarre la surveillance temps réel de la licence pendant la lecture.
     * Sonde le backend toutes les 60 s et bloque si la licence est révoquée ou expirée.
     */
    fun startLicenseMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Timber.i("🔒 Surveillance licence démarrée pour: ${licenseManager.getDeviceId()}")

        monitorJob = monitorScope.launch {
            while (isMonitoring) {
                checkServerStatus()
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    /**
     * Arrête la surveillance de la licence
     */
    fun stopLicenseMonitoring() {
        isMonitoring = false
        monitorJob?.cancel()
        monitorJob = null
        Timber.i("🔓 Surveillance licence arrêtée")
    }

    /**
     * Interroge le backend et réagit si la licence est devenue invalide
     */
    private suspend fun checkServerStatus() {
        val result = licenseRepository.checkAccess()

        if (result.isSuccess) {
            val active = result.getOrNull()?.active == true
            if (active) {
                licenseManager.setActivatedLocally(true)
            }

            if (!active && isMonitoring) {
                // Pas de blocage si l'essai local est encore valide (période de grâce)
                if (!licenseManager.isTrialValid()) {
                    val reason = if (licenseManager.isActivatedLocally()) {
                        InvalidReason.DEACTIVATED
                    } else {
                        InvalidReason.TRIAL_EXPIRED
                    }
                    Timber.w("🚨 Licence révoquée/expirée côté serveur! ($reason)")
                    invalidCallback?.onLicenseInvalid(reason)
                    stopLicenseMonitoring()
                }
            }
        } else {
            Timber.w("⚠️ Sonde serveur en échec (mode offline): ${result.exceptionOrNull()?.message}")
            // Fallback sur les données locales (moins sécurisé)
            if (!licenseManager.isTrialValid() && !licenseManager.isActivatedLocally()) {
                invalidCallback?.onLicenseInvalid(InvalidReason.SERVER_VALIDATION_FAILED)
                stopLicenseMonitoring()
            }
        }
    }

    /**
     * Vérifie si l'accès est valide en utilisant l'heure du serveur (anti-triche).
     * Appelée avant de démarrer la lecture.
     */
    suspend fun validateAccessWithServerTime(): ServerValidationResult {
        return try {
            val deviceId = licenseManager.getDeviceId()

            // Statut + heure serveur (en-tête HTTP Date, non falsifiable côté client)
            val result = licenseRepository.checkAccessWithServerTime(deviceId)
            if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Backend inaccessible")

            val check = result.getOrNull()!!
            val isActive = check.active
            val serverTime = check.serverTime
            val installDate = licenseManager.getInstallDate()

            // Calculer l'expiration côté serveur
            val trialEndDate = installDate + (TRIAL_DAYS * DAY_IN_MILLIS)
            val isTrialValid = serverTime < trialEndDate

            // Mettre à jour le cache local
            licenseManager.setActivatedLocally(isActive)

            val hasAccess = isActive || isTrialValid

            Timber.i("🔐 Validation serveur - Accès: $hasAccess, Activé: $isActive, Essai valide: $isTrialValid")

            ServerValidationResult(
                isValid = hasAccess,
                isActivated = isActive,
                isTrialValid = isTrialValid,
                trialDaysRemaining = ((trialEndDate - serverTime) / DAY_IN_MILLIS).coerceAtLeast(0).toInt(),
                serverTime = serverTime
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur validation serveur")
            // Fallback sur les données locales
            ServerValidationResult(
                isValid = licenseManager.hasValidAccess(),
                isActivated = licenseManager.isActivatedLocally(),
                isTrialValid = licenseManager.isTrialValid(),
                trialDaysRemaining = licenseManager.getTrialDaysRemaining(),
                serverTime = System.currentTimeMillis(),
                error = e.message
            )
        }
    }
}

/**
 * Résultat de validation serveur
 */
data class ServerValidationResult(
    val isValid: Boolean,
    val isActivated: Boolean,
    val isTrialValid: Boolean,
    val trialDaysRemaining: Int,
    val serverTime: Long,
    val error: String? = null
) {
    /**
     * Formate la date serveur
     */
    fun getServerTimeFormatted(): String {
        val date = java.util.Date(serverTime)
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.FRANCE)
        return formatter.format(date)
    }
}
