package com.skyplayer.pro.data.repository

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.skyplayer.pro.data.license.LicenseManager
import com.skyplayer.pro.data.remote.LicenseApiService
import com.skyplayer.pro.data.remote.MacCheckResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Document d'activation Firestore (même schéma que le backend Sky-player)
 */
data class FirestoreActivation(
    val target_mac: String? = null,
    val status: String? = null,
    val xtream_host: String? = null,
    val xtream_server_url: String? = null,
    val xtream_username: String? = null,
    val xtream_user: String? = null,
    val xtream_password: String? = null,
    val playlist_url: String? = null,
    val playlist_name: String? = null,
    val type: String? = "m3u"
) {
    fun isActive(): Boolean = status?.uppercase() == "ACTIF" || status?.uppercase() == "ACTIVE"

    fun getXtreamHost(): String? = xtream_host ?: xtream_server_url
    fun getXtreamUser(): String? = xtream_username ?: xtream_user
    fun getXtreamPassword(): String? = xtream_password
    fun getPlaylistUrl(): String? = playlist_url
    fun getPlaylistName(): String? = playlist_name
    fun getPlaylistType(): String = type ?: "m3u"
}

/**
 * Repository unique de licence — source de vérité : le backend Sky-player.
 *
 * Combine :
 * - la vérification HTTP du statut MAC : GET /api/mac/check/{mac} (auth X-Activation-API-Key),
 * - l'écoute temps réel Firestore (collection `activations`, mêmes champs que le backend),
 * - la synchronisation du cache local via [LicenseManager].
 *
 * L'ancien accès Firebase Realtime Database a été supprimé : le backend business
 * (Sky-player) s'appuie sur Firestore, garder RTDB créait deux sources de vérité.
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseManager: LicenseManager,
    private val apiService: LicenseApiService,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) {
    private var currentListener: ListenerRegistration? = null

    /**
     * Vérifie le statut d'une MAC sur le backend Sky-player (source de vérité serveur).
     */
    suspend fun checkAccess(mac: String = licenseManager.getDeviceId()): Result<MacCheckResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.checkMac(mac)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        Timber.i("✅ Backend: MAC $mac active=${data.active}")
                        Result.success(data)
                    } else {
                        Result.failure(Exception("Réponse vide du backend"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                    Timber.e("❌ Backend erreur: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception backend")
                Result.failure(e)
            }
        }

    /**
     * Résultat d'une vérification serveur avec l'heure du serveur (anti-triche)
     */
    data class ServerAccessResult(
        val active: Boolean,
        val serverTime: Long
    )

    /**
     * Vérifie l'accès sur le backend et expose l'heure du serveur (en-tête HTTP `Date`).
     * Utilisé par l'anti-triche : l'horloge du serveur ne peut pas être falsifiée côté client.
     */
    suspend fun checkAccessWithServerTime(mac: String = licenseManager.getDeviceId()): Result<ServerAccessResult> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.checkMac(mac)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        val serverTime = parseHttpDate(response.headers()["Date"]) ?: System.currentTimeMillis()
                        Timber.i("✅ Backend: MAC $mac active=${data.active} (heure serveur: $serverTime)")
                        Result.success(ServerAccessResult(active = data.active, serverTime = serverTime))
                    } else {
                        Result.failure(Exception("Réponse vide du backend"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                    Timber.e("❌ Backend erreur: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception backend")
                Result.failure(e)
            }
        }

    private fun parseHttpDate(value: String?): Long? {
        if (value == null) return null
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).parse(value)?.time
        } catch (e: Exception) {
            Timber.w("⚠️ En-tête Date illisible: $value")
            null
        }
    }

    /**
     * Vérifie l'accès complet : serveur d'abord, cache local en fallback (mode offline).
     * Le statut actif est décidé côté serveur (expiryDate dans Firestore).
     */
    suspend fun validateAccess(): AccessValidationResult {
        val backendResult = checkAccess()

        return if (backendResult.isSuccess) {
            val active = backendResult.getOrNull()?.active == true
            AccessValidationResult(
                hasAccess = active,
                isTrialActive = false,
                isActivated = active,
                trialDaysRemaining = if (active) licenseManager.getTrialDaysRemaining() else 0,
                deviceId = licenseManager.getDeviceId()
            )
        } else {
            // Fallback sur les données locales si backend inaccessible
            val localTrialValid = licenseManager.isTrialValid()
            AccessValidationResult(
                hasAccess = licenseManager.hasValidAccess(),
                isTrialActive = localTrialValid,
                isActivated = licenseManager.isActivatedLocally(),
                trialDaysRemaining = licenseManager.getTrialDaysRemaining(),
                deviceId = licenseManager.getDeviceId()
            )
        }
    }

    /**
     * Récupère le document d'activation Firestore d'une MAC
     */
    suspend fun getActivation(macAddress: String): Result<FirestoreActivation?> {
        return try {
            val snapshot = firestore.collection("activations")
                .whereEqualTo("target_mac", macAddress)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents.first()
                val activation = doc.toObject(FirestoreActivation::class.java)
                Timber.i("📄 Firestore activation found for $macAddress")
                Result.success(activation)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error getting Firestore activation for $macAddress")
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel les changements d'activation Firestore
     */
    fun observeActivation(macAddress: String): Flow<FirestoreActivation?> = callbackFlow {
        Timber.i("👂 Listening for Firestore activations for $macAddress")

        currentListener = firestore.collection("activations")
            .whereEqualTo("target_mac", macAddress)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ Firestore listener error")
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val activation = doc.toObject(FirestoreActivation::class.java)
                    Timber.i("🔄 Firestore activation update: $activation")
                    trySend(activation)
                } else {
                    Timber.d("ℹ️ No Firestore activation found for $macAddress")
                    trySend(null)
                }
            }

        awaitClose {
            currentListener?.remove()
            currentListener = null
            Timber.d("🛑 Firestore listener stopped")
        }
    }

    /**
     * Arrête l'écoute Firestore en cours
     */
    fun stopListening() {
        currentListener?.remove()
        currentListener = null
    }
}

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
            "Période d'essai terminée. Pour continuer à utiliser SkyPlayer, veuillez activer votre application auprès de votre revendeur."
        } else {
            ""
        }
    }
}
