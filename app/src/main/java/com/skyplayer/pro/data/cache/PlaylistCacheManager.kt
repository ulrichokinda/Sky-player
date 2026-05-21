package com.skyplayer.pro.data.cache

import android.content.Context
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * Gestionnaire de cache pour les playlists M3U
 * Optimisé pour réduire les requêtes réseau et économiser la data
 */
class PlaylistCacheManager(
    private val context: Context,
    private val m3uParser: M3UParser
) {
    companion object {
        private const val CACHE_DIR_NAME = "playlist_cache"
        private const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 heures
        private const val METADATA_FILE = "cache_metadata.json"
    }
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Récupère les chaînes depuis le cache ou depuis le réseau
     * Stratégie: Cache-First avec revalidation périodique
     */
    suspend fun getChannels(
        playlistId: String,
        url: String,
        forceRefresh: Boolean = false
    ): List<Channel> {
        return withContext(Dispatchers.IO) {
            val cacheFile = getCacheFile(url)
            val metadata = getCacheMetadata(playlistId)
            
            // Vérifier si le cache est valide
            val isCacheValid = !forceRefresh && 
                              cacheFile.exists() && 
                              metadata != null &&
                              metadata.timestamp > System.currentTimeMillis() - CACHE_MAX_AGE_MS &&
                              metadata.url == url
            
            if (isCacheValid) {
                Timber.d("Utilisation du cache pour $playlistId")
                // Parser depuis le cache
                val cachedChannels = m3uParser.parseFromFile(cacheFile, playlistId)
                if (cachedChannels.isNotEmpty()) {
                    return@withContext cachedChannels
                }
            }
            
            // Récupérer depuis le réseau
            Timber.d("Récupération depuis le réseau pour $playlistId")
            val channels = m3uParser.parseFromUrl(url, playlistId)
            
            // Sauvegarder dans le cache
            if (channels.isNotEmpty()) {
                saveToCache(url, channels, playlistId)
            }
            
            channels
        }
    }
    
    /**
     * Sauvegarde les chaînes dans le cache
     */
    private suspend fun saveToCache(url: String, channels: List<Channel>, playlistId: String) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(url)
                
                // Générer le contenu M3U pour le cache
                val m3uContent = generateM3UContent(channels)
                cacheFile.writeText(m3uContent, Charsets.UTF_8)
                
                // Mettre à jour les métadonnées
                val metadata = CacheMetadata(
                    playlistId = playlistId,
                    url = url,
                    timestamp = System.currentTimeMillis(),
                    channelCount = channels.size
                )
                saveCacheMetadata(playlistId, metadata)
                
                Timber.d("Cache sauvegardé pour $playlistId: ${channels.size} chaînes")
                
                // Nettoyer les vieux caches si nécessaire
                cleanupOldCaches()
                
            } catch (e: Exception) {
                Timber.e(e, "Erreur sauvegarde cache pour $playlistId")
            }
        }
    }
    
    /**
     * Génère le contenu M3U à partir des chaînes
     */
    private fun generateM3UContent(channels: List<Channel>): String {
        val builder = StringBuilder()
        builder.appendLine("#EXTM3U")
        
        channels.forEach { channel ->
            builder.append("#EXTINF:-1 ")
            
            // Attributs
            val attrs = mutableListOf<String>()
            channel.epgId?.let { attrs.add("tvg-id=\"$it\"") }
            channel.logoUrl?.let { attrs.add("tvg-logo=\"$it\"") }
            attrs.add("group-title=\"${channel.category}\"")
            attrs.add("tvg-name=\"${channel.name}\"")
            
            builder.append(attrs.joinToString(" "))
            builder.appendLine(",${channel.name}")
            builder.appendLine(channel.url)
        }
        
        return builder.toString()
    }
    
    /**
     * Invalide le cache pour une playlist spécifique
     */
    suspend fun invalidateCache(playlistId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Trouver et supprimer le fichier cache
                val metadata = getCacheMetadata(playlistId)
                metadata?.let {
                    val cacheFile = getCacheFile(it.url)
                    if (cacheFile.exists()) {
                        cacheFile.delete()
                        Timber.d("Cache invalidé pour $playlistId")
                    }
                }
                
                // Supprimer les métadonnées
                deleteCacheMetadata(playlistId)
                
            } catch (e: Exception) {
                Timber.e(e, "Erreur invalidation cache pour $playlistId")
            }
        }
    }
    
    /**
     * Invalide tout le cache
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
                Timber.d("Tout le cache a été effacé")
            } catch (e: Exception) {
                Timber.e(e, "Erreur suppression totale du cache")
            }
        }
    }
    
    /**
     * Récupère la taille totale du cache en bytes
     */
    suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }
    
    /**
     * Nettoie les caches trop vieux (> 7 jours)
     */
    private suspend fun cleanupOldCaches() {
        withContext(Dispatchers.IO) {
            val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 jours
            val cutoffTime = System.currentTimeMillis() - maxAge
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime && file.name != METADATA_FILE) {
                    file.delete()
                    Timber.d("Ancien cache supprimé: ${file.name}")
                }
            }
        }
    }
    
    /**
     * Génère le nom de fichier cache à partir de l'URL
     */
    private fun getCacheFile(url: String): File {
        val hash = url.toMD5()
        return File(cacheDir, "playlist_$hash.m3u")
    }
    
    /**
     * Récupère les métadonnées du cache
     */
    private fun getCacheMetadata(playlistId: String): CacheMetadata? {
        val metadataFile = File(cacheDir, "${playlistId}_$METADATA_FILE")
        return try {
            if (metadataFile.exists()) {
                val json = metadataFile.readText()
                parseMetadataJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Sauvegarde les métadonnées du cache
     */
    private fun saveCacheMetadata(playlistId: String, metadata: CacheMetadata) {
        val metadataFile = File(cacheDir, "${playlistId}_$METADATA_FILE")
        try {
            val json = generateMetadataJson(metadata)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            Timber.e(e, "Erreur sauvegarde métadonnées")
        }
    }
    
    /**
     * Supprime les métadonnées
     */
    private fun deleteCacheMetadata(playlistId: String) {
        File(cacheDir, "${playlistId}_$METADATA_FILE").delete()
    }
    
    /**
     * Parse les métadonnées JSON
     */
    private fun parseMetadataJson(json: String): CacheMetadata? {
        return try {
            val lines = json.lines()
            CacheMetadata(
                playlistId = lines.find { it.startsWith("playlistId:") }?.substringAfter(":")?.trim() ?: "",
                url = lines.find { it.startsWith("url:") }?.substringAfter(":")?.trim() ?: "",
                timestamp = lines.find { it.startsWith("timestamp:") }?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L,
                channelCount = lines.find { it.startsWith("channelCount:") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Génère les métadonnées en JSON simple
     */
    private fun generateMetadataJson(metadata: CacheMetadata): String {
        return """
            playlistId: ${metadata.playlistId}
            url: ${metadata.url}
            timestamp: ${metadata.timestamp}
            channelCount: ${metadata.channelCount}
        """.trimIndent()
    }
    
    /**
     * Extension pour calculer MD5
     */
    private fun String.toMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    data class CacheMetadata(
        val playlistId: String,
        val url: String,
        val timestamp: Long,
        val channelCount: Int
    )
}
