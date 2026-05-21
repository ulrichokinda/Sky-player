package com.skyplayer.pro.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Gestionnaire d'identifiant unique d'appareil pour le marché africain
 *
 * Objectif : Créer un UUID persistant qui survit aux désinstallations/réinstallations
 *
 * Stratégie hybride pour persistance maximale :
 * 1. EncryptedSharedPreferences (stockage sécurisé local)
 * 2. Backup sur stockage externe (si permission accordée)
 * 3. Génération déterministe basée sur hardware ID (fallback)
 *
 * Format : AFR-XXXX-XXXX (ex: AFR-8F3A-9B2C)
 *
 * Compatibilité : Android 5.0+ (API 21+)
 */
class DeviceIdentifier private constructor(private val context: Context) {

    companion object {
        private const val PREFS_FILENAME = "skyplayer_device_id_secured"
        private const val PREFS_FILENAME_BACKUP = "skyplayer_device_id_backup"
        private const val KEY_DEVICE_ID = "device_uuid"
        private const val KEY_DEVICE_ID_HASH = "device_uuid_hash"
        private const val KEY_GENERATION_DATE = "generation_date"

        // Backup directory
        private const val BACKUP_SUBDIR = ".skyplayer_backup"
        private const val BACKUP_FILENAME = "device.id"

        // Hardware-based fallback key
        private const val HARDWARE_SEED = "SKYPLAYER_AFRICA_2024"

        @Volatile
        private var instance: DeviceIdentifier? = null

        fun getInstance(context: Context): DeviceIdentifier {
            return instance ?: synchronized(this) {
                instance ?: DeviceIdentifier(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private var cachedDeviceId: String? = null

    /**
     * Récupère l'identifiant unique de l'appareil au format AFR-XXXX-XXXX
     *
     * Stratégie :
     * 1. Vérifier cache mémoire
     * 2. Vérifier EncryptedSharedPreferences
     * 3. Vérifier backup sur stockage externe
     * 4. Vérifier anciennes préférences non chiffrées (migration)
     * 5. Générer nouveau UUID avec backup
     * 6. Fallback: ID basé sur hardware (Android ID + Serial)
     */
    fun getDeviceId(): String {
        // 1. Cache mémoire
        cachedDeviceId?.let { return it }

        // 2. EncryptedSharedPreferences (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val encryptedPrefs = getEncryptedSharedPreferences()
                encryptedPrefs?.getString(KEY_DEVICE_ID, null)?.let { id ->
                    if (isValidFormat(id)) {
                        cachedDeviceId = formatDeviceId(id)
                        Timber.d("📱 Device ID from EncryptedSharedPreferences: ${cachedDeviceId!!.take(12)}...")
                        return cachedDeviceId!!
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur lecture EncryptedSharedPreferences")
            }
        }

        // 3. Backup sur stockage externe (DocumentDirectory - persiste uninstall si utilisateur conserve données)
        try {
            val backupId = readFromExternalBackup()
            if (backupId != null && isValidFormat(backupId)) {
                cachedDeviceId = formatDeviceId(backupId)
                // Restaurer dans les préférences chiffrées
                saveDeviceId(cachedDeviceId!!)
                Timber.d("📱 Device ID restored from external backup: ${cachedDeviceId!!.take(12)}...")
                return cachedDeviceId!!
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Pas de backup externe disponible: ${e.message}")
        }

        // 4. Anciennes préférences non chiffrées (migration vers chiffré)
        try {
            val legacyPrefs = context.getSharedPreferences(PREFS_FILENAME_BACKUP, Context.MODE_PRIVATE)
            legacyPrefs.getString(KEY_DEVICE_ID, null)?.let { id ->
                if (isValidFormat(id)) {
                    cachedDeviceId = formatDeviceId(id)
                    // Migrer vers nouveau format chiffré
                    saveDeviceId(cachedDeviceId!!)
                    Timber.d("📱 Device ID migré depuis anciennes préférences: ${cachedDeviceId!!.take(12)}...")
                    return cachedDeviceId!!
                }
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur lecture anciennes préférences")
        }

        // 5. Générer nouvel UUID
        val newId = generateNewDeviceId()
        cachedDeviceId = formatDeviceId(newId)
        saveDeviceId(cachedDeviceId!!)

        Timber.i("🆕 Nouveau Device ID généré: ${cachedDeviceId!!.take(12)}...")
        return cachedDeviceId!!
    }

    /**
     * Récupère l'UUID brut (sans formatage AFR)
     */
    fun getRawUUID(): String {
        return getDeviceId().removePrefix("AFR-").replace("-", "")
    }

    /**
     * Récupère l'ID court (8 premiers caractères)
     */
    fun getShortId(): String {
        return getDeviceId().removePrefix("AFR-").take(9).replace("-", "")
    }

    /**
     * Vérifie si un ID est au format valide
     */
    private fun isValidFormat(id: String): Boolean {
        return id.matches(Regex("^AFR-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$", RegexOption.IGNORE_CASE))
    }

    /**
     * Formate un UUID en format AFR-XXXX-XXXX-XXXX-XXXX
     */
    private fun formatDeviceId(uuid: String): String {
        // Nettoyer l'UUID
        val cleanUuid = uuid.replace("-", "").uppercase()

        // Si déjà au format AFR, retourner tel quel
        if (cleanUuid.startsWith("AFR")) return uuid.uppercase()

        // Si c'est un UUID standard (32 caractères hex)
        return if (cleanUuid.length >= 16) {
            "AFR-${cleanUuid.take(4)}-${cleanUuid.substring(4, 8)}-${cleanUuid.substring(8, 12)}-${cleanUuid.substring(12, 16)}"
        } else {
            // Fallback: générer à partir de l'input
            val padded = cleanUuid.padEnd(16, '0')
            "AFR-${padded.take(4)}-${padded.substring(4, 8)}-${padded.substring(8, 12)}-${padded.substring(12, 16)}"
        }
    }

    /**
     * Génère un nouvel identifiant unique
     * Combine UUID aléatoire avec hardware ID pour robustesse
     */
    private fun generateNewDeviceId(): String {
        return try {
            // Essayer de générer un UUID v4 aléatoire
            val randomUuid = UUID.randomUUID().toString().replace("-", "")

            // Mélanger avec hardware ID si disponible (pour stabilité)
            val hardwareId = getHardwareBasedId()
            val combined = xorHexStrings(randomUuid.take(16), hardwareId.take(16))

            "AFR-$combined"
        } catch (e: Exception) {
            // Fallback ultime: ID basé sur temps + hardware
            val timestamp = System.currentTimeMillis().toString(16).uppercase().takeLast(8)
            val hardware = getHardwareBasedId().take(8)
            "AFR-$timestamp-$hardware-0000"
        }
    }

    /**
     * Génère un ID basé sur les caractéristiques hardware de l'appareil
     * Utile comme fallback et pour la stabilité
     */
    private fun getHardwareBasedId(): String {
        val components = mutableListOf<String>()

        // Android ID (désinstallations peuvent le changer sur Android 8+, mais utile pour détection)
        try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                components.add(androidId.takeLast(8))
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Impossible de récupérer Android ID")
        }

        // Build info (toujours disponible, mais pas unique)
        components.add(Build.BOARD.takeLast(4))
        components.add(Build.BRAND.takeLast(4))

        // Serial (déprécié et restreint sur Android 10+, mais encore utile sur anciens)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                @Suppress("DEPRECATION")
                Build.SERIAL?.let { components.add(it.takeLast(4)) }
            } catch (e: Exception) {
                // Ignorer
            }
        }

        // Combiner et hasher
        val combined = components.joinToString("") + HARDWARE_SEED
        return hashToHex(combined).take(16)
    }

    /**
     * Sauvegarde l'ID dans tous les emplacements possibles
     */
    private fun saveDeviceId(deviceId: String) {
        // 1. EncryptedSharedPreferences (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val encryptedPrefs = getEncryptedSharedPreferences()
                encryptedPrefs?.edit()?.apply {
                    putString(KEY_DEVICE_ID, deviceId)
                    putString(KEY_DEVICE_ID_HASH, hashToHex(deviceId))
                    putLong(KEY_GENERATION_DATE, System.currentTimeMillis())
                    apply()
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur sauvegarde EncryptedSharedPreferences")
            }
        }

        // 2. Backup sur stockage externe (DocumentDirectory)
        try {
            writeToExternalBackup(deviceId)
        } catch (e: Exception) {
            Timber.w("⚠️ Impossible de créer backup externe: ${e.message}")
        }

        // 3. Préférences non chiffrées (pour compatibilité/migration)
        try {
            val legacyPrefs = context.getSharedPreferences(PREFS_FILENAME_BACKUP, Context.MODE_PRIVATE)
            legacyPrefs.edit().apply {
                putString(KEY_DEVICE_ID, deviceId)
                putLong(KEY_GENERATION_DATE, System.currentTimeMillis())
                apply()
            }
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur sauvegarde préférences legacy")
        }
    }

    /**
     * Obtient les SharedPreferences chiffrées (Android 6.0+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getEncryptedSharedPreferences(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur création EncryptedSharedPreferences, fallback sur normal")
            null
        }
    }

    /**
     * Écrit le backup sur stockage externe (DocumentDirectory)
     * Ce répertoire est généralement préservé lors de la désinstallation
     * si l'utilisateur choisit "Conserver les données de l'application"
     */
    private fun writeToExternalBackup(deviceId: String) {
        try {
            // Utiliser le répertoire Documents de l'app (persistent)
            val backupDir = File(context.getExternalFilesDir(null), BACKUP_SUBDIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, BACKUP_FILENAME)

            // Chiffrer légèrement (XOR simple) pour ne pas stocker en clair
            val encrypted = xorWithKey(deviceId, HARDWARE_SEED)

            FileOutputStream(backupFile).use { output ->
                output.write(encrypted.toByteArray(Charsets.UTF_8))
            }

            // Rendre le fichier caché
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                backupFile.setReadable(false, false)
                backupFile.setWritable(false, false)
                backupFile.setReadable(true, true)
                backupFile.setWritable(true, true)
            }

            Timber.d("💾 Backup externe créé: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Lit le backup depuis le stockage externe
     */
    private fun readFromExternalBackup(): String? {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), BACKUP_SUBDIR)
            val backupFile = File(backupDir, BACKUP_FILENAME)

            if (!backupFile.exists()) {
                return null
            }

            val encrypted = FileInputStream(backupFile).use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }

            // Déchiffrer
            xorWithKey(encrypted, HARDWARE_SEED)
        } catch (e: Exception) {
            Timber.w("⚠️ Erreur lecture backup: ${e.message}")
            null
        }
    }

    /**
     * Supprime toutes les traces de l'ID (debug/testing uniquement)
     */
    fun clearDeviceId() {
        cachedDeviceId = null

        // Supprimer EncryptedSharedPreferences
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                context.deleteSharedPreferences(PREFS_FILENAME)
            } catch (e: Exception) {
                // Ignorer
            }
        }

        // Supprimer anciennes préférences
        context.getSharedPreferences(PREFS_FILENAME_BACKUP, Context.MODE_PRIVATE).edit().clear().apply()

        // Supprimer backup externe
        try {
            val backupFile = File(context.getExternalFilesDir(null), "$BACKUP_SUBDIR/$BACKUP_FILENAME")
            if (backupFile.exists()) {
                backupFile.delete()
            }
        } catch (e: Exception) {
            // Ignorer
        }

        Timber.w("🗑️ Device ID effacé (debug)")
    }

    // ===== UTILITAIRES CRYPTO =====

    /**
     * XOR simple pour obfuscation (pas de sécurité forte, juste pour éviter stockage clair)
     */
    private fun xorWithKey(input: String, key: String): String {
        return input.mapIndexed { index, char ->
            char.code.xor(key[index % key.length].code).toChar()
        }.joinToString("")
    }

    /**
     * XOR de deux chaînes hexadécimales
     */
    private fun xorHexStrings(a: String, b: String): String {
        val result = StringBuilder()
        for (i in 0 until maxOf(a.length, b.length)) {
            val charA = if (i < a.length) a[i].digitToIntOrNull(16) ?: 0 else 0
            val charB = if (i < b.length) b[i].digitToIntOrNull(16) ?: 0 else 0
            result.append((charA.xor(charB)).toString(16).uppercase())
        }
        return result.toString()
    }

    /**
     * Hash simple en hexadécimal
     */
    private fun hashToHex(input: String): String {
        var hash = 0
        for (char in input) {
            hash = 31 * hash + char.code
        }
        return Integer.toHexString(hash).uppercase().padStart(8, '0')
    }

    /**
     * Vérifie si l'ID actuel est valide
     */
    fun isValid(): Boolean {
        return try {
            val id = getDeviceId()
            isValidFormat(id) && id.length >= 19
        } catch (e: Exception) {
            false
        }
    }
}
