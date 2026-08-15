package com.skyplayer.pro.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.skyplayer.pro.utils.DeviceIdentifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Firebase (Obsolète - Remplacé par DeviceCheckService pour la production)
 */
@Singleton
class FirebaseConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdentifier: DeviceIdentifier,
    private val firebaseDatabase: FirebaseDatabase
) {
    companion object {
        private const val FIREBASE_USERS_NODE = "users"
        private const val FIREBASE_BACKUP_PREFS = "firebase_backup"
    }

    private val secureBackupPrefs : SharedPreferences by lazy {
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

    fun release() {
        coroutineScope.cancel()
    }
}
