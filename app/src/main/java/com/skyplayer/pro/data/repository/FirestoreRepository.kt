package com.skyplayer.pro.data.repository

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.skyplayer.pro.data.license.LicenseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data class representing an activation document
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
 * Repository for Firestore operations (activations and playlists)
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val licenseManager: LicenseManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) {
    private var currentListener: ListenerRegistration? = null

    /**
     * Get activation document from Firestore by MAC address
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
     * Listen for realtime activation updates from Firestore
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
     * Stop listening to Firestore updates
     */
    fun stopListening() {
        currentListener?.remove()
        currentListener = null
    }
}
