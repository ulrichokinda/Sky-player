package com.skyplayer.pro.data.security

import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager pour le contrôle parental et le verrouillage de catégories
 */
@Singleton
class ParentalControlManager @Inject constructor(
    private val encryptedPrefs: EncryptedPrefs
) {
    companion object {
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_LOCKED_CATEGORIES = "locked_categories"
        private val SENSITIVE_KEYWORDS = listOf("ADULT", "XXX", "ADULTE", "X-", "+18", "PORN", "EROTIC")
    }

    /**
     * Vérifie si un code PIN est configuré
     */
    fun isPinSet(): Boolean {
        return !encryptedPrefs.getString(KEY_PARENTAL_PIN).isNullOrBlank()
    }

    /**
     * Définit le code PIN
     */
    fun setPin(pin: String) {
        encryptedPrefs.saveString(KEY_PARENTAL_PIN, pin)
    }

    /**
     * Vérifie si le PIN saisi est correct
     */
    fun checkPin(pin: String): Boolean {
        val savedPin = encryptedPrefs.getString(KEY_PARENTAL_PIN)
        return savedPin == pin
    }

    /**
     * Détermine si une catégorie est sensible par défaut
     */
    fun isSensitiveCategory(categoryName: String): Boolean {
        val upperName = categoryName.uppercase()
        return SENSITIVE_KEYWORDS.any { upperName.contains(it) } || isManuallyLocked(categoryName)
    }

    /**
     * Vérifie si une catégorie a été verrouillée manuellement
     */
    fun isManuallyLocked(categoryName: String): Boolean {
        val locked = encryptedPrefs.getString(KEY_LOCKED_CATEGORIES, "") ?: ""
        return locked.split(",").contains(categoryName)
    }

    /**
     * Verrouille ou déverrouille une catégorie
     */
    fun toggleCategoryLock(categoryName: String) {
        val locked = encryptedPrefs.getString(KEY_LOCKED_CATEGORIES, "")?.split(",")?.toMutableList() ?: mutableListOf()
        if (locked.contains(categoryName)) {
            locked.remove(categoryName)
        } else {
            locked.add(categoryName)
        }
        encryptedPrefs.saveString(KEY_LOCKED_CATEGORIES, locked.joinToString(","))
    }
}
