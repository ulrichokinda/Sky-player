package com.skyplayer.pro.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skyplayer.pro.data.license.LicenseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository pour la gestion des licences via Firebase
 * Permet la vérification à distance de l'activation et la gestion par revendeurs
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseManager: LicenseManager,
    private val firebaseDatabase: FirebaseDatabase
) {
    companion object {
        private const val LICENSES_NODE = "licenses"
        private const val HEALTH_CHECK_NODE = "health_check"
    }
    
    /**
     * Structure des données de licence dans Firebase
     */
    data class LicenseData(
        val deviceId: String = "",
        val installDate: Long = 0,
        val isActive: Boolean = false,
        val activatedBy: String = "", // Email ou ID du revendeur
        val activationDate: Long = 0,
        val deviceInfo: DeviceInfo = DeviceInfo()
    )
    
    data class DeviceInfo(
        val brand: String = android.os.Build.BRAND,
        val model: String = android.os.Build.MODEL,
        val androidVersion: String = android.os.Build.VERSION.RELEASE,
        val lastSeen: Long = System.currentTimeMillis()
    )
    
    /**
     * Enregistre l'appareil dans Firebase à la première installation
     * Cette fonction est appelée lors du premier lancement
     */
    suspend fun registerDevice(): Result<Unit> = try {
        val deviceId = licenseManager.getDeviceId()
        val installDate = licenseManager.getInstallDate()
        
        val licenseData = LicenseData(
            deviceId = deviceId,
            installDate = installDate,
            isActive = false, // Par défaut, en attente d'activation
            deviceInfo = DeviceInfo()
        )
        
        val ref = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId")
        ref.setValue(licenseData).await()
        
        Timber.i("📱 Appareil enregistré dans Firebase : $deviceId")
        Result.success(Unit)
    } catch (e : Exception) {
        Timber.e(e, "❌ Erreur lors de l'enregistrement de l'appareil")
        Result.failure(e)
    }
    
    /**
     * Vérifie si l'appareil est activé dans Firebase
     * Cette fonction est appelée à chaque lancement pour synchroniser le statut
     */
    suspend fun checkActivationStatus() : Result<Boolean> = try {
        val deviceId = licenseManager.getDeviceId()
        val ref = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId/isActive")
        
        val snapshot = ref.get().await()
        val isActive = snapshot.getValue(Boolean::class.java) ?: false
        
        // Mettre à jour le cache local
        licenseManager.setActivatedLocally(isActive)
        
        // Mettre à jour le lastSeen
        updateLastSeen(deviceId)
        
        Timber.i("🔍 Statut d'activation vérifié : $isActive")
        Result.success(isActive)
    } catch (e : Exception) {
        Timber.e(e, "❌ Erreur lors de la vérification du statut")
        Result.failure(e)
    }
    
    /**
     * Écoute en temps réel le changement de statut d'activation
     * Permet l'activation instantanée par le revendeur
     */
    fun observeActivationStatus(): Flow<Boolean> = callbackFlow {
        val deviceId = licenseManager.getDeviceId()
        val ref = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId/isActive")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot : DataSnapshot) {
                val isActive = snapshot.getValue(Boolean::class.java) ?: false
                
                // Mettre à jour le cache local
                licenseManager.setActivatedLocally(isActive)
                
                Timber.d("📡 Changement de statut d'activation : $isActive")
                trySend(isActive)
            }
            
            override fun onCancelled(error : DatabaseError) {
                Timber.e("❌ Erreur listener Firebase : ${error.message}")
            }
        }
        
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
            Timber.d("👋 Listener activation fermé")
        }
    }
    
    /**
     * Met à jour la date de dernière connexion
     */
    private suspend fun updateLastSeen(deviceId: String) {
        try {
            val ref = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId/deviceInfo/lastSeen")
            ref.setValue(System.currentTimeMillis()).await()
        } catch (e: Exception) {
            Timber.w("⚠️ Impossible de mettre à jour lastSeen: ${e.message}")
        }
    }
    
    /**
     * Vérifie la connexion avec Firebase (Health Check)
     * Écrit et lit une valeur de test pour confirmer le fonctionnement
     */
    suspend fun performHealthCheck(): Result<HealthCheckResult> = try {
        val testId = "health_${System.currentTimeMillis()}"
        val testData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "deviceId" to licenseManager.getDeviceId(),
            "status" to "test"
        )
        
        // Écriture
        val writeRef = firebaseDatabase.getReference("$HEALTH_CHECK_NODE/$testId")
        writeRef.setValue(testData).await()
        
        // Lecture
        val readSnapshot = writeRef.get().await()
        val readSuccess = readSnapshot.exists() && readSnapshot.child("status").getValue(String::class.java) == "test"
        
        // Suppression du test
        writeRef.removeValue().await()
        
        val result = HealthCheckResult(
            success = readSuccess,
            timestamp = System.currentTimeMillis(),
            message = if (readSuccess) "Connexion Firebase OK" else "Erreur de lecture/écriture"
        )
        
        Timber.i("🏥 Health Check: ${result.message}")
        Result.success(result)
    } catch (e: Exception) {
        Timber.e(e, "❌ Health Check échoué")
        Result.failure(e)
    }
    
    /**
     * Récupère les données complètes de licence depuis Firebase
     */
    suspend fun getLicenseDataFromFirebase(): Result<LicenseData> = try {
        val deviceId = licenseManager.getDeviceId()
        val ref = firebaseDatabase.getReference("$LICENSES_NODE/$deviceId")
        
        val snapshot = ref.get().await()
        val licenseData = snapshot.getValue(LicenseData::class.java)
            ?: LicenseData(deviceId = deviceId)
        
        Result.success(licenseData)
    } catch (e: Exception) {
        Timber.e(e, "❌ Erreur récupération données licence")
        Result.failure(e)
    }
    
    /**
     * Vérifie si l'appareil a un accès valide (essai ou activé)
     * Combine les données locales et Firebase
     */
    suspend fun validateAccess(): AccessValidationResult {
        val isTrialValid = licenseManager.isTrialValid()
        
        // Vérifier l'activation distante
        val activationResult = checkActivationStatus()
        val isActivated = activationResult.getOrDefault(false)
        
        return AccessValidationResult(
            hasAccess = isTrialValid || isActivated,
            isTrialActive = isTrialValid,
            isActivated = isActivated,
            trialDaysRemaining = licenseManager.getTrialDaysRemaining(),
            deviceId = licenseManager.getDeviceId()
        )
    }
}

/**
 * Résultat du health check
 */
data class HealthCheckResult(
    val success: Boolean,
    val timestamp: Long,
    val message: String
)

/**
 * Résultat de validation d'accès
 */
data class AccessValidationResult(
    val hasAccess: Boolean,
    val isTrialActive: Boolean,
    val isActivated: Boolean,
    val trialDaysRemaining: Int,
    val deviceId: String
) {
    /**
     * Message à afficher à l'utilisateur selon le statut
     */
    fun getStatusMessage(): String {
        return when {
            isActivated -> "✅ Application activée"
            isTrialActive -> "🎁 Essai gratuit - $trialDaysRemaining jours restants"
            else -> "⏳ Période d'essai terminée"
        }
    }
    
    /**
     * Message pour l'écran de blocage
     */
    fun getBlockedMessage(): String {
        return if (!hasAccess) {
            "Période d'essai terminée. Pour continuer à utiliser SkyPlayer, veuillez activer votre application sur skyplayerapp.xyz"
        } else {
            ""
        }
    }
}
