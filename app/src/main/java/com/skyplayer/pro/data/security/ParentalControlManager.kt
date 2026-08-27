package com.skyplayer.pro.data.security

import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import java.security.MessageDigest
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
        private const val KEY_SECURITY_QUESTION = "parental_security_question"
        private const val KEY_SECURITY_ANSWER = "parental_security_answer"
        private const val KEY_LOCKED_CATEGORIES = "locked_categories"
        private val SENSITIVE_KEYWORDS = listOf(
            // Anglais / international
            "ADULT", "XXX", "PORN", "EROTIC", "EROTIQUE", "SEX", "SEXY", "SEXUAL",
            "EXPLICIT", "MATURE", "NUDITY", "NUDE", "NUDITE", "BABES", "STRIP",
            "PLAYBOY", "PENTHOUSE", "DORCEL", "VIVID", "BAZOOKA", "SCANDALE", "ONLYFANS",
            // Français
            "ADULTE", "SEXE", "18+", "+18", "18 ANS", "X-",
            // Espagnol / Portugais
            "ADULTOS", "ADULTO", "EROTICAS", "SEXO"
        )

        /**
         * Vérification pure (testable sans Android) : le nom de la catégorie
         * contient un mot-clé de contenu sensible.
         */
        fun isSensitiveCategoryName(categoryName: String): Boolean {
            val upperName = categoryName.uppercase()
            return SENSITIVE_KEYWORDS.any { upperName.contains(it) }
        }
    }

    /**
     * Vérifie si un code PIN est configuré
     */
    fun isPinSet(): Boolean {
        return !encryptedPrefs.getString(KEY_PARENTAL_PIN).isNullOrBlank()
    }

    /**
     * Définit le code PIN et la question de sécurité
     */
    fun setupParentalControl(pin: String, question: String, answer: String) {
        encryptedPrefs.saveString(KEY_PARENTAL_PIN, pin)
        encryptedPrefs.saveString(KEY_SECURITY_QUESTION, question)
        encryptedPrefs.saveString(KEY_SECURITY_ANSWER, answer.lowercase().trim())
    }

    /**
     * Récupère la question de sécurité configurée
     */
    fun getSecurityQuestion(): String? {
        return encryptedPrefs.getString(KEY_SECURITY_QUESTION)
    }

    /**
     * Vérifie la réponse à la question de sécurité pour réinitialiser le PIN
     */
    fun verifySecurityAnswer(answer: String): Boolean {
        val savedAnswer = encryptedPrefs.getString(KEY_SECURITY_ANSWER)
        return savedAnswer != null && savedAnswer == answer.lowercase().trim()
    }

    /**
     * Réinitialise le code PIN (après validation de la question de sécurité)
     */
    fun resetPin(newPin: String) {
        encryptedPrefs.saveString(KEY_PARENTAL_PIN, newPin)
    }

    /**
     * Définit le code PIN
     */
    fun setPin(pin: String) {
        encryptedPrefs.saveString(KEY_PARENTAL_PIN, hashPin(pin))
    }

    /**
     * Vérifie si le PIN saisi est correct
     */
    fun checkPin(pin: String): Boolean {
        val savedHash = encryptedPrefs.getString(KEY_PARENTAL_PIN) ?: return false
        return savedHash == hashPin(pin)
    }

    /**
     * Détermine si une catégorie est sensible par défaut
     */
    fun isSensitiveCategory(categoryName: String): Boolean {
        return isSensitiveCategoryName(categoryName) || isManuallyLocked(categoryName)
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

    private fun hashPin(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}