package com.skyplayer.pro.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.skyplayer.pro.data.local.PlaylistDao
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.utils.DeviceIdentifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Firebase pour configuration utilisateur et gestion des abonnements
 *
 * Structure Firebase Realtime Database:
 * ```
 * /users
 *   /{device_id_AFR_XXXX}
 *     - playlistUrl: String
 *     - subscription:
 *         - active: Boolean
 *         - plan: String (basic/premium/vip)
 *         - expiresAt: Long (timestamp)
 *         - startedAt: Long
 *     - alerts:
 *         - message: String
 *         - type: String (info/warning/critical)
 *         - shownAt: Long
 *     - settings:
 *         - preferredQuality: String
 *         - autoPlay: Boolean
 *         - parentalLock: Boolean
 *     - lastUpdated: Long
 * ```
 *
 * Fonctionnalités:
 * - Récupération config utilisateur via DeviceIdentifier
 * - Vérification abonnement avant lecture
 * - Mode hors-ligne avec fallback local
 * - Sync automatique des changements
 */
@Singleton
class FirebaseConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val deviceIdentifier: DeviceIdentifier
) {
    companion object {
        private const val FIREBASE_USERS_NODE = "users"
        private const val FIREBASE_SUBSCRIPTION_NODE = "subscription"
        private const val FIREBASE_ALERTS_NODE = "alerts"
        private const val FIREBACKUP_PLAYLIST_NODE = "backup"
        
        // Timeout Firebase
        private const val FIREBASE_TIMEOUT_MS = 10000L
        private const val OFFLINE_GRACE_PERIOD_MS = 24 * 60 * 60 * 1000 // 24h
        private const val FIREBASE_BACKUP_PREFS = "firebase_backup"
    }

    private val database: FirebaseDatabase = Firebase.database
    private val usersRef: DatabaseReference = database.getReference(FIREBASE_USERS_NODE)
    private val secureBackupPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FIREBASE_BACKUP_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // État de connexion Firebase
    private val _connectionState = MutableStateFlow<FirebaseConnectionState>(FirebaseConnectionState.Unknown)
    val connectionState: StateFlow<FirebaseConnectionState> = _connectionState.asStateFlow()

    // Configuration utilisateur cache
    private val _userConfig = MutableStateFlow<UserConfig?>(null)
    val userConfig: StateFlow<UserConfig?> = _userConfig.asStateFlow()

    // État de l'abonnement
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Unknown)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    // Messages d'alerte
    private val _alertMessage = MutableStateFlow<AlertMessage?>(null)
    val alertMessage: StateFlow<AlertMessage?> = _alertMessage.asStateFlow()

    init {
        // Observer état de connexion Firebase
        setupConnectionObserver()
    }

    /**
     * Récupère l'ID unique de l'appareil
     */
    private fun getDeviceId(): String {
        return deviceIdentifier.getDeviceId()
            .replace(".", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("#", "_")
            .replace("/", "_")
    }

    /**
     * Récupère la configuration complète de l'utilisateur depuis Firebase
     * avec fallback hors-ligne
     */
    suspend fun fetchUserConfig(): UserConfigResult {
        val deviceId = getDeviceId()
        Timber.i("🔍 Récupération config pour device: ${deviceId.take(15)}...")

        return try {
            // Essayer Firebase avec timeout
            val config = withTimeoutOrNull(FIREBASE_TIMEOUT_MS) {
                val snapshot = usersRef.child(deviceId).get().await()
                snapshot.getValue<UserConfigData>()
            }

            if (config != null) {
                // Succès - mettre en cache et sauvegarder backup local
                val userConfig = config.toUserConfig(deviceId)
                _userConfig.value = userConfig
                saveOfflineBackup(userConfig)
                _connectionState.value = FirebaseConnectionState.Online

                Timber.i("✅ Config Firebase récupérée - Subscription: ${config.subscription?.active}")
                UserConfigResult.Success(userConfig)
            } else {
                // Pas de config sur Firebase - essayer backup local
                Timber.w("⚠️ Pas de config Firebase, tentative fallback local...")
                val offlineBackup = getOfflineBackup()
                if (offlineBackup != null) {
                    UserConfigResult.Offline(offlineBackup)
                } else {
                    UserConfigResult.NoConfig
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur connexion Firebase: ${e.message}")
            _connectionState.value = FirebaseConnectionState.Offline

            // Fallback hors-ligne
            val offlineConfig = getOfflineBackup()
            if (offlineConfig != null) {
                Timber.i("📴 Mode hors-ligne - utilisant backup local")
                UserConfigResult.Offline(offlineConfig)
            } else {
                UserConfigResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * Vérifie si l'abonnement est actif avant de lancer le lecteur
     *
     * @return SubscriptionCheckResult avec statut et message
     */
    suspend fun checkSubscription(): SubscriptionCheckResult {
        val deviceId = getDeviceId()

        return try {
            val snapshot = withTimeoutOrNull(FIREBASE_TIMEOUT_MS) {
                usersRef.child(deviceId)
                    .child(FIREBASE_SUBSCRIPTION_NODE)
                    .get()
                    .await()
            }

            val subscription = snapshot?.getValue<SubscriptionData>()

            if (subscription == null) {
                _subscriptionStatus.value = SubscriptionStatus.NoSubscription
                return SubscriptionCheckResult.NoActiveSubscription("Aucun abonnement trouvé")
            }

            val now = System.currentTimeMillis()
            val isActive = subscription.active == true &&
                    (subscription.expiresAt == null || subscription.expiresAt > now)

            _subscriptionStatus.value = if (isActive) {
                SubscriptionStatus.Active(subscription.plan ?: "basic")
            } else {
                SubscriptionStatus.Expired
            }

            when {
                !isActive -> SubscriptionCheckResult.Expired(
                    expiresAt = subscription.expiresAt,
                    message = "Votre abonnement a expiré"
                )
                subscription.expiresAt != null && (subscription.expiresAt - now) < 7 * 24 * 60 * 60 * 1000 -> {
                    // Expire dans moins de 7 jours
                    SubscriptionCheckResult.ActiveButExpiringSoon(
                        expiresAt = subscription.expiresAt,
                        daysRemaining = ((subscription.expiresAt - now) / (24 * 60 * 60 * 1000)).toInt()
                    )
                }
                else -> SubscriptionCheckResult.Active(plan = subscription.plan ?: "basic")
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur vérification abonnement")

            // Mode hors-ligne - vérifier grace period
            val offlineConfig = getOfflineBackup()
            if (offlineConfig?.lastSyncTimestamp != null) {
                val offlineDuration = System.currentTimeMillis() - offlineConfig.lastSyncTimestamp

                if (offlineDuration < OFFLINE_GRACE_PERIOD_MS) {
                    // Grace period active - permettre accès temporairement
                    _subscriptionStatus.value = SubscriptionStatus.OfflineGracePeriod
                    SubscriptionCheckResult.OfflineGracePeriod(
                        hoursRemaining = ((OFFLINE_GRACE_PERIOD_MS - offlineDuration) / (60 * 60 * 1000)).toInt()
                    )
                } else {
                    SubscriptionCheckResult.OfflineExpired
                }
            } else {
                SubscriptionCheckResult.Error("Connexion requise pour vérifier l'abonnement")
            }
        }
    }

    /**
     * Récupère la playlist M3U de l'utilisateur
     */
    suspend fun getPlaylistUrl(): String? {
        // Essayer d'abord Firebase
        val deviceId = getDeviceId()

        return try {
            val snapshot = withTimeoutOrNull(FIREBASE_TIMEOUT_MS) {
                usersRef.child(deviceId).child("playlistUrl").get().await()
            }

            val url = snapshot?.getValue<String>()

            if (!url.isNullOrBlank()) {
                // Sauvegarder en local pour hors-ligne
                savePlaylistUrlOffline(url)
                url
            } else {
                // Fallback local
                getOfflinePlaylistUrl()
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur récupération URL, fallback local: ${e.message}")
            getOfflinePlaylistUrl()
        }
    }

    /**
     * Récupère les messages d'alerte administrateur
     */
    suspend fun fetchAlertMessage(): AlertMessage? {
        val deviceId = getDeviceId()

        return try {
            val snapshot = withTimeoutOrNull(FIREBASE_TIMEOUT_MS) {
                usersRef.child(deviceId).child(FIREBASE_ALERTS_NODE).get().await()
            }

            val alert = snapshot?.getValue<AlertData>()
            val alertMessage = alert?.toAlertMessage()
            _alertMessage.value = alertMessage
            alertMessage
        } catch (e: Exception) {
            Timber.w("⚠️ Impossible de récupérer les alertes: ${e.message}")
            null
        }
    }

    /**
     * Écoute les changements de configuration en temps réel
     */
    fun observeUserConfig(): Flow<UserConfig?> = callbackFlow<UserConfig?> {
        val deviceId = getDeviceId()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val config = snapshot.getValue<UserConfigData>()
                val userConfig = config?.toUserConfig(deviceId)
                _userConfig.value = userConfig
                channel.trySend(userConfig)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("❌ Erreur listener Firebase: ${error.message}")
                channel.close(error.toException())
            }
        }

        val ref = usersRef.child(deviceId)
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    /**
     * Met à jour la configuration utilisateur
     */
    suspend fun updateUserConfig(update: UserConfigUpdate): Boolean {
        val deviceId = getDeviceId()

        return try {
            val updates = mutableMapOf<String, Any?>()
            updates["deviceId"] = deviceId
            update.playlistUrl?.let { updates["playlistUrl"] = it }
            update.settings?.let { updates["settings"] = it }
            updates["lastUpdated"] = ServerValue.TIMESTAMP

            usersRef.child(deviceId).updateChildren(updates).await()
            
            // Mettre à jour le cache local
            val current = _userConfig.value
            if (current != null) {
                val updated = current.copy(
                    playlistUrl = update.playlistUrl ?: current.playlistUrl,
                    settings = update.settings ?: current.settings
                )
                _userConfig.value = updated
                saveOfflineBackup(updated)
            }

            Timber.i("✅ Config mise à jour sur Firebase")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur mise à jour config")
            false
        }
    }

    /**
     * Sauvegarde la configuration pour usage hors-ligne
     */
    private suspend fun saveOfflineBackup(config: UserConfig) {
        try {
            secureBackupPrefs.edit().apply {
                putString("playlist_url", config.playlistUrl)
                putString("device_id", config.deviceId)
                putBoolean("subscription_active", config.subscription?.active ?: false)
                putString("subscription_plan", config.subscription?.plan)
                putLong("subscription_expires", config.subscription?.expiresAt ?: 0)
                putLong("last_sync", System.currentTimeMillis())
                apply()
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur sauvegarde backup: ${e.message}")
        }
    }

    /**
     * Récupère le backup hors-ligne
     */
    private fun getOfflineBackup(): UserConfig? {
        return try {
            val lastSync = secureBackupPrefs.getLong("last_sync", 0)
            
            if (lastSync == 0L) return null

            UserConfig(
                deviceId = secureBackupPrefs.getString("device_id", "") ?: "",
                playlistUrl = secureBackupPrefs.getString("playlist_url", null),
                subscription = SubscriptionInfo(
                    active = secureBackupPrefs.getBoolean("subscription_active", false),
                    plan = secureBackupPrefs.getString("subscription_plan", "basic") ?: "basic",
                    expiresAt = secureBackupPrefs.getLong("subscription_expires", 0).takeIf { it > 0 },
                    startedAt = null
                ),
                settings = UserSettings(),
                alerts = null,
                lastSyncTimestamp = lastSync
            )
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur lecture backup: ${e.message}")
            null
        }
    }

    private fun savePlaylistUrlOffline(url: String) {
        secureBackupPrefs.edit()
            .putString("playlist_url", url)
            .apply()
    }

    private fun getOfflinePlaylistUrl(): String? {
        return secureBackupPrefs
            .getString("playlist_url", null)
    }

    private fun setupConnectionObserver() {
        try {
            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue<Boolean>() ?: false
                    _connectionState.value = if (connected) {
                        FirebaseConnectionState.Online
                    } else {
                        FirebaseConnectionState.Offline
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _connectionState.value = FirebaseConnectionState.Offline
                }
            })
        } catch (e: Exception) {
            Timber.w("⚠️ Impossible d'observer connexion Firebase: ${e.message}")
            _connectionState.value = FirebaseConnectionState.Unknown
        }
    }

    fun release() {
        coroutineScope.cancel()
    }
}

// ===== DATA CLASSES =====

data class UserConfig(
    val deviceId: String,
    val playlistUrl: String?,
    val subscription: SubscriptionInfo?,
    val settings: UserSettings?,
    val alerts: AlertMessage?,
    val lastSyncTimestamp: Long? = null
)

data class SubscriptionInfo(
    val active: Boolean,
    val plan: String,
    val expiresAt: Long?,
    val startedAt: Long?
)

data class UserSettings(
    val preferredQuality: String = "auto",
    val autoPlay: Boolean = true,
    val parentalLock: Boolean = false
)

data class AlertMessage(
    val message: String,
    val type: AlertType,
    val shownAt: Long?
)

enum class AlertType { INFO, WARNING, CRITICAL }

enum class FirebaseConnectionState { Online, Offline, Unknown }

sealed class SubscriptionStatus {
    object Unknown : SubscriptionStatus()
    data class Active(val plan: String) : SubscriptionStatus()
    object Expired : SubscriptionStatus()
    object NoSubscription : SubscriptionStatus()
    object OfflineGracePeriod : SubscriptionStatus()
}

sealed class UserConfigResult {
    data class Success(val config: UserConfig) : UserConfigResult()
    data class Offline(val config: UserConfig) : UserConfigResult()
    object NoConfig : UserConfigResult()
    data class Error(val message: String) : UserConfigResult()
}

sealed class SubscriptionCheckResult {
    data class Active(val plan: String) : SubscriptionCheckResult()
    data class ActiveButExpiringSoon(val expiresAt: Long?, val daysRemaining: Int) : SubscriptionCheckResult()
    data class Expired(val expiresAt: Long?, val message: String) : SubscriptionCheckResult()
    data class NoActiveSubscription(val message: String) : SubscriptionCheckResult()
    data class OfflineGracePeriod(val hoursRemaining: Int) : SubscriptionCheckResult()
    object OfflineExpired : SubscriptionCheckResult()
    data class Error(val message: String) : SubscriptionCheckResult()
}

data class UserConfigUpdate(
    val playlistUrl: String? = null,
    val settings: UserSettings? = null
)

// ===== FIREBASE DATA CLASSES =====

private data class UserConfigData(
    val playlistUrl: String? = null,
    val subscription: SubscriptionData? = null,
    val alerts: AlertData? = null,
    val settings: UserSettingsData? = null,
    val lastUpdated: Long? = null
) {
    fun toUserConfig(deviceId: String) = UserConfig(
        deviceId = deviceId,
        playlistUrl = playlistUrl,
        subscription = subscription?.toSubscriptionInfo(),
        settings = settings?.toUserSettings() ?: UserSettings(),
        alerts = alerts?.toAlertMessage(),
        lastSyncTimestamp = lastUpdated
    )
}

private data class SubscriptionData(
    val active: Boolean? = false,
    val plan: String? = "basic",
    val expiresAt: Long? = null,
    val startedAt: Long? = null
) {
    fun toSubscriptionInfo() = SubscriptionInfo(
        active = active ?: false,
        plan = plan ?: "basic",
        expiresAt = expiresAt,
        startedAt = startedAt
    )
}

private data class AlertData(
    val message: String? = null,
    val type: String? = "info",
    val shownAt: Long? = null
) {
    fun toAlertMessage() = AlertMessage(
        message = message ?: "",
        type = when (type) {
            "warning" -> AlertType.WARNING
            "critical" -> AlertType.CRITICAL
            else -> AlertType.INFO
        },
        shownAt = shownAt
    )
}

private data class UserSettingsData(
    val preferredQuality: String? = "auto",
    val autoPlay: Boolean? = true,
    val parentalLock: Boolean? = false
) {
    fun toUserSettings() = UserSettings(
        preferredQuality = preferredQuality ?: "auto",
        autoPlay = autoPlay ?: true,
        parentalLock = parentalLock ?: false
    )
}
