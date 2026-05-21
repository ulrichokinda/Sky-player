package com.skyplayer.pro.data.license

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de sécurité des licences
 * - Vérifie l'heure serveur pour éviter la triche sur la date
 * - Bloque immédiatement la lecture si la licence est révoquée
 * - Écoute en temps réel les changements d'activation
 */
@Singleton
class LicenseSecurityManager @Inject constructor(
    private val licenseManager: LicenseManager,
    private val firebaseDatabase: FirebaseDatabase
) {
    companion object {
        private const val LICENSES_NODE = "licenses"
        private const val SERVER_TIME_NODE = "serverTime"
        private const val TRIAL_DAYS = 15L
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    }

    /**
     * Callback appelé lorsque la licence devient invalide pendant la lecture
     */
    interface LicenseInvalidCallback {
        fun onLicenseInvalid(reason: InvalidReason)
    }

    enum class InvalidReason {
        DEACTIVATED,           // isActive passé à false
        TRIAL_EXPIRED,         // Essai de 15 jours terminé
        SERVER_VALIDATION_FAILED  // Erreur validation serveur
    }

    private var invalidCallback: LicenseInvalidCallback? = null
    private var isMonitoring = false

    /**
     * Définit le callback pour la révocation de licence
     */
    fun setLicenseInvalidCallback(callback: LicenseInvalidCallback) {
        this.invalidCallback = callback
    }

    /**
     * Démarre la surveillance temps réel de la licence pendant la lecture
     * Cette fonction doit être appelée quand le lecteur démarre
     */
    fun startLicenseMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        val deviceId = licenseManager.getDeviceId()
        
        // Écouter les changements d'activation
        val activationRef = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId/isActive")
        activationRef.addValueEventListener(activationListener)
        
        // Vérifier périodiquement l'expiration de l'essai via le serveur
        checkTrialExpirationServerSide()
        
        Timber.i("🔒 Surveillance licence démarrée pour: $deviceId")
    }

    /**
     * Arrête la surveillance de la licence
     */
    fun stopLicenseMonitoring() {
        isMonitoring = false
        val deviceId = licenseManager.getDeviceId()
        val activationRef = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId/isActive")
        activationRef.removeEventListener(activationListener)
        
        Timber.i("🔓 Surveillance licence arrêtée")
    }

    /**
     * Listener pour les changements d'activation
     */
    private val activationListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val isActive = snapshot.getValue(Boolean::class.java) ?: false
            
            if (!isActive && isMonitoring) {
                // La licence a été révoquée !
                Timber.w("🚨 Licence révoquée en temps réel!")
                invalidCallback?.onLicenseInvalid(InvalidReason.DEACTIVATED)
                stopLicenseMonitoring()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Timber.e("❌ Erreur listener activation: ${error.message}")
        }
    }

    /**
     * Vérifie l'expiration de l'essai en utilisant l'heure du serveur (anti-triche)
     */
    private fun checkTrialExpirationServerSide() {
        val deviceId = licenseManager.getDeviceId()
        val installDate = licenseManager.getInstallDate()
        
        // Obtenir l'heure serveur
        firebaseDatabase.getReference(SERVER_TIME_NODE).get().addOnSuccessListener { snapshot ->
            val serverTime = snapshot.getValue(Long::class.java) ?: System.currentTimeMillis()
            
            // Calculer si l'essai est expiré côté serveur
            val trialEndDate = installDate + (TRIAL_DAYS * DAY_IN_MILLIS)
            val isExpired = serverTime > trialEndDate
            
            if (isExpired && !licenseManager.isActivatedLocally()) {
                Timber.w("🚨 Essai expiré (vérification serveur)!")
                invalidCallback?.onLicenseInvalid(InvalidReason.TRIAL_EXPIRED)
                stopLicenseMonitoring()
            }
        }.addOnFailureListener { e ->
            Timber.e(e, "❌ Impossible d'obtenir l'heure serveur")
            // Fallback sur l'heure locale (moins sécurisé)
            if (!licenseManager.isTrialValid() && !licenseManager.isActivatedLocally()) {
                invalidCallback?.onLicenseInvalid(InvalidReason.SERVER_VALIDATION_FAILED)
            }
        }
    }

    /**
     * Vérifie si l'accès est valide en utilisant l'heure serveur (anti-triche)
     * Cette fonction est appelée avant de démarrer la lecture
     */
    suspend fun validateAccessWithServerTime(): ServerValidationResult {
        return try {
            val deviceId = licenseManager.getDeviceId()
            
            // Obtenir l'heure serveur
            val serverTimeSnapshot = firebaseDatabase.getReference(SERVER_TIME_NODE).get().await()
            val serverTime = serverTimeSnapshot.getValue(Long::class.java) ?: System.currentTimeMillis()
            
            // Obtenir les données de licence
            val licenseRef = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId").get().await()
            val isActive = licenseRef.child("isActive").getValue(Boolean::class.java) ?: false
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

    /**
     * Met à jour le timestamp serveur dans Firebase (appelé par le dashboard admin)
     * L'application ne peut pas écrire ici, seul l'admin peut le faire
     */
    suspend fun updateServerTimestamp(): Result<Unit> = try {
        // Cette opération échouera si l'app n'a pas les droits admin (c'est normal)
        val ref = firebaseDatabase.getReference(SERVER_TIME_NODE)
        ref.setValue(ServerValue.TIMESTAMP).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.w("⚠️ Impossible de mettre à jour serverTime (normal si pas admin): ${e.message}")
        Result.failure(e)
    }

    /**
     * Observe le temps serveur pour vérification anti-triche
     */
    fun observeServerTime(): Flow<Long> = callbackFlow {
        val ref = firebaseDatabase.getReference(SERVER_TIME_NODE)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val time = snapshot.getValue(Long::class.java) ?: System.currentTimeMillis()
                trySend(time)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("❌ Erreur listener serverTime: ${error.message}")
            }
        }
        
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
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
