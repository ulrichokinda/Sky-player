package com.skyplayer.pro.data.epg

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A11: Cache EPG persistant.
 *
 * Le parser EPG télécharge ~50 Mo de données XML. Ce manager :
 * 1. Vérifie si un cache valide existe (< 24h)
 * 2. Si oui, lit le fichier en cache au lieu de re-télécharger
 * 3. Si non, télécharge et met en cache
 * 4. Nettoie les caches > 48h
 *
 * Le cache est stocké en fichier brut (pas Room) car les données EPG
 * sont volumineuses et ne sont pas structurées pour des queries SQL.
 */
@Singleton
class EpgCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CACHE_DIR = "epg_cache"
        private const val CACHE_FILE = "epg_programs.xml"
        private const val METADATA_FILE = "epg_meta"
        private const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L  // 24h
        private const val CACHE_CLEANUP_AGE_MS = 48 * 60 * 60 * 1000L  // 48h
        private const val MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024L  // 100 Mo max
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            METADATA_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).also { it.mkdirs() }
    }

    private val cacheFile: File by lazy {
        File(cacheDir, CACHE_FILE)
    }

    /**
     * Vérifie si le cache est encore valide (< 24h).
     */
    fun isCacheValid(): Boolean {
        if (!cacheFile.exists()) return false

        val lastModified = cacheFile.lastModified()
        val age = System.currentTimeMillis() - lastModified
        val isValid = age < CACHE_MAX_AGE_MS

        Timber.d("📦 EPG cache: exists=${cacheFile.exists()}, age=${age / 3600000}h, valid=$isValid")
        return isValid
    }

    /**
     * Lit le fichier EPG en cache.
     * @return le contenu XML ou null si pas de cache.
     */
    fun readCache(): String? {
        if (!isCacheValid()) return null

        return try {
            val content = cacheFile.readText(Charsets.UTF_8)
            Timber.i("📦 EPG cache: lu ${content.length / 1024} Ko depuis le cache")
            content
        } catch (e: Exception) {
            Timber.e(e, "❌ EPG cache: erreur de lecture")
            null
        }
    }

    /**
     * Écrit le contenu EPG dans le cache.
     */
    fun writeCache(content: String) {
        try {
            cacheFile.writeText(content, Charsets.UTF_8)
            prefs.edit().putLong("last_epg_download", System.currentTimeMillis()).apply()
            Timber.i("📦 EPG cache: écrit ${content.length / 1024} Ko")
        } catch (e: Exception) {
            Timber.e(e, "❌ EPG cache: erreur d'écriture")
        }
    }

    /**
     * Nettoie les caches trop vieux (> 48h) et vérifie la taille.
     */
    fun cleanup() {
        val now = System.currentTimeMillis()

        // Supprimer le cache si trop vieux
        if (cacheFile.exists()) {
            val age = now - cacheFile.lastModified()
            if (age > CACHE_CLEANUP_AGE_MS) {
                cacheFile.delete()
                Timber.i("🧹 EPG cache: supprimé (age > 48h)")
            }
        }

        // Vérifier la taille totale du répertoire
        val totalSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0
        if (totalSize > MAX_CACHE_SIZE_BYTES) {
            Timber.w("⚠️ EPG cache: taille $totalSize octets > ${MAX_CACHE_SIZE_BYTES / 1024 / 1024} Mo")
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * Force le refresh du cache (supprime et re-télécharge).
     */
    fun invalidateCache() {
        cacheFile.delete()
        Timber.i("🔄 EPG cache: invalidé")
    }

    /**
     * Retourne les stats du cache pour l'UI.
     */
    fun getCacheInfo(): EpgCacheInfo {
        val exists = cacheFile.exists()
        val size = if (exists) cacheFile.length() else 0L
        val lastModified = if (exists) cacheFile.lastModified() else 0L
        val ageHours = if (lastModified > 0) (System.currentTimeMillis() - lastModified) / 3600000 else 0L
        val valid = isCacheValid()

        return EpgCacheInfo(
            exists = exists,
            sizeKb = size / 1024,
            ageHours = ageHours,
            isValid = valid,
            lastDownload = prefs.getLong("last_epg_download", 0L)
        )
    }
}

data class EpgCacheInfo(
    val exists: Boolean,
    val sizeKb: Long,
    val ageHours: Long,
    val isValid: Boolean,
    val lastDownload: Long
)
