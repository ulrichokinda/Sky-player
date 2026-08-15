package com.skyplayer.pro.data.encrypted

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stockage sécurisé chiffré pour credentials et configurations sensibles
 * Utilise EncryptedSharedPreferences avec AES256
 */
@Singleton
class EncryptedPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "skyplayer_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Sauvegarde l'état du mode Turbo
     */
    fun saveDataSaverEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("data_saver_enabled", enabled).apply()
    }

    /**
     * Récupère l'état du mode Turbo
     */
    fun isDataSaverEnabled(): Boolean {
        return prefs.getBoolean("data_saver_enabled", false)
    }

    /**
     * Sauvegarde Xtream credentials de façon sécurisée
     */
    fun saveXtreamCredentials(host: String, username: String, password: String) {
        prefs.edit().apply {
            putString("xtream_host", host)
            putString("xtream_username", username)
            putString("xtream_password", password)
            putLong("xtream_saved_at", System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Récupère les credentials Xtream
     */
    fun getXtreamCredentials(): Triple<String, String, String>? {
        val host = prefs.getString("xtream_host", null) ?: return null
        val username = prefs.getString("xtream_username", null) ?: return null
        val password = prefs.getString("xtream_password", null) ?: return null
        return Triple(host, username, password)
    }
    
    /**
     * Sauvegarde string chiffrée générique
     */
    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    /**
     * Récupère string chiffrée
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }
    
    /**
     * Vérifie si des credentials existent
     */
    fun hasXtreamCredentials(): Boolean {
        return prefs.contains("xtream_host") && 
               prefs.contains("xtream_username") &&
               prefs.contains("xtream_password")
    }
    
    /**
     * Supprime les credentials (logout)
     */
    fun clearXtreamCredentials() {
        prefs.edit().apply {
            remove("xtream_host")
            remove("xtream_username")
            remove("xtream_password")
            remove("xtream_saved_at")
            apply()
        }
    }
    
    /**
     * Efface toutes les données chiffrées
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Sauvegarde Boolean
     */
    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    /**
     * Récupère Boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    /**
     * Sauvegarde Int
     */
    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
    
    /**
     * Récupère Int
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun isOnboardingCompleted(): Boolean {
        return getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        saveBoolean("onboarding_completed", completed)
    }
}
