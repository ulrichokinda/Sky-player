package com.skyplayer.pro.data.parser

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream
import java.io.File

/**
 * Parser M3U haute performance pour playlists IPTV
 * Supporte les formats M3U et M3U8 avec attributs étendus et compression GZIP
 */
class M3UParser(private val okHttpClient: OkHttpClient) {
    
    
    /**
     * Parse une playlist M3U depuis une URL avec timeout configurable
     * Optimisé pour réseaux lents avec retry intégré
     */
    suspend fun parseFromUrl(url: String, playlistId: String, maxRetries: Int = 3): List<Channel> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            repeat(maxRetries) { attempt ->
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "SkyPlayerPro/1.0")
                        .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain")
                        .build()
                    
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("HTTP ${response.code}")
                        }
                        
                        response.body?.let { body ->
                            return@withContext parseFromInputStream(body.byteStream(), playlistId, url)
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                    Timber.w("Tentative ${attempt + 1}/$maxRetries échouée pour $url: ${e.message}")
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(1000L * (attempt + 1)) // Backoff exponentiel
                    }
                }
            }
            
            Timber.e(lastException, "Échec parsing M3U après $maxRetries tentatives: $url")
            emptyList()
        }
    }
    
    /**
     * Parse depuis un fichier local
     */
    suspend fun parseFromFile(file: File, playlistId: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                val content = file.readText(Charsets.UTF_8)
                parseFromContent(content, playlistId, file.absolutePath)
            } catch (e: Exception) {
                Timber.e(e, "Erreur parsing fichier M3U: ${file.path}")
                emptyList()
            }
        }
    }
    
    /**
     * Parse depuis un flux d'entrée (InputStream)
     * Supporte la détection automatique de GZIP et l'extraction de l'URL EPG
     */
    suspend fun parseFromInputStream(
        inputStream: InputStream, 
        playlistId: String, 
        sourceUrl: String? = null,
        onEpgUrlFound: (String) -> Unit = {}
    ): List<Channel> {
        return withContext(Dispatchers.Default) {
            val bufferedStream = BufferedInputStream(inputStream)
            
            // Détection GZIP (Magic bytes: 0x1f 0x8b)
            val finalStream = if (isGzipped(bufferedStream)) {
                Timber.d("Compression GZIP détectée pour $sourceUrl")
                GZIPInputStream(bufferedStream)
            } else {
                bufferedStream
            }

            val reader = BufferedReader(InputStreamReader(finalStream, Charsets.UTF_8))
            val channels = mutableListOf<Channel>()
            var extInfLine: String? = null
            var lineNumber = 0
            var firstLine = true

            reader.useLines { lines ->
                for (line in lines) {
                    lineNumber++
                    val trimmedLine = line.trim()

                    if (firstLine) {
                        firstLine = false
                        if (!trimmedLine.startsWith(EXT_M3U, ignoreCase = true)) {
                            Timber.w("Contenu invalide - pas d'en-tête #EXTM3U")
                            return@useLines
                        }
                        
                        // Extraire l'URL EPG du header x-tvg-url ou url-tvg
                        extractEpgUrl(trimmedLine)?.let { epgUrl ->
                            Timber.i("📌 URL EPG trouvée dans le header M3U : $epgUrl")
                            onEpgUrlFound(epgUrl)
                        }
                        continue
                    }

                    when {
                        trimmedLine.isEmpty() -> continue
                        trimmedLine.startsWith("#EXTVLCOPT") -> continue
                        trimmedLine.startsWith("#EXTGRP") -> {
                            extInfLine = (extInfLine ?: "") + " group-title=\"${trimmedLine.substringAfter(":").trim()}\""
                        }
                        trimmedLine.startsWith("#EXTLOGO") -> {
                            extInfLine = (extInfLine ?: "") + " tvg-logo=\"${trimmedLine.substringAfter(":").trim()}\""
                        }
                        trimmedLine.startsWith("#") && !trimmedLine.startsWith(EXT_INF) -> continue
                        trimmedLine.startsWith(EXT_INF) -> {
                            extInfLine = trimmedLine
                        }
                        else -> {
                            // URL de flux
                            extInfLine?.let { extInf ->
                                try {
                                    if (isValidStreamUrl(trimmedLine)) {
                                        val channel = parseChannel(extInf, trimmedLine, playlistId, channels.size, sourceUrl)
                                        channels.add(channel)
                                    }
                                } catch (_: Exception) {
                                    // Ignorer erreur individuelle
                                }
                                extInfLine = null
                            }
                        }
                    }
                }
            }
            
            Timber.d("M3U parsé: ${channels.size} chaînes trouvées depuis ${sourceUrl ?: "stream"}")
            channels
        }
    }

    /**
     * Extrait l'URL EPG du header #EXTM3U
     */
    private fun extractEpgUrl(header: String): String? {
        val epgRegex = "(?:url-tvg|x-tvg-url)=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
        return epgRegex.find(header)?.groupValues?.get(1)
    }

    /**
     * Vérifie si le flux est compressé en GZIP sans le consommer
     */
    private fun isGzipped(inputStream: BufferedInputStream): Boolean {
        inputStream.mark(2)
        val b1 = inputStream.read()
        val b2 = inputStream.read()
        inputStream.reset()
        return b1 == 0x1f && b2 == 0x8b
    }

    /**
     * Parse depuis une chaîne de contenu (legacy / compatibilité)
     */
    suspend fun parseFromContent(content: String, playlistId: String, sourceUrl: String? = null): List<Channel> {
        return parseFromInputStream(content.byteInputStream(), playlistId, sourceUrl)
    }
    
    /**
     * Parse une playlist M3U depuis un InputStream (streaming)
     */
    suspend fun parse(inputStream: InputStream, playlistId: String, sourceUrl: String? = null): List<Channel> {
        return parseFromInputStream(inputStream, playlistId, sourceUrl)
    }
    
    /**
     * Vérifie si l'URL est un flux valide
     */
    private fun isValidStreamUrl(url: String): Boolean {
        return url.startsWith("http://") || 
               url.startsWith("https://") || 
               url.startsWith("rtmp://") ||
               url.startsWith("rtsp://") ||
               url.startsWith("udp://") ||
               url.startsWith("rtp://") ||
               url.startsWith("file://")
    }
    
    /**
     * Parse une ligne EXTINF et l'URL associée avec métadonnées enrichies
     */
    private fun parseChannel(
        extInfLine: String, 
        urlLine: String, 
        playlistId: String, 
        index: Int,
        sourceUrl: String? = null
    ): Channel {
        // Extraire la durée et le titre
        val duration = DURATION_REGEX.find(extInfLine)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        
        // Extraire les attributs
        val attributes = parseAttributes(extInfLine)
        
        // Extraire le titre (dernière partie après la dernière virgule)
        val rawTitle = extInfLine.substringAfterLast(",", "Unknown").trim()
        val cleanTitle = cleanChannelName(rawTitle)
        
        // Déterminer le type de contenu avec heuristique améliorée
        val contentType = detectContentType(extInfLine, urlLine, duration, attributes)
        
        // Construire le groupe avec fallback intelligent
        val groupTitle = buildGroupTitle(attributes, cleanTitle, contentType)
        
        // Générer ID unique stable
        val stableId = generateStableId(playlistId, urlLine, cleanTitle, index)
        
        // Nettoyer et valider l'URL du logo
        val logoUrl = attributes[ATTR_TVG_LOGO]?.let { cleanLogoUrl(it) }
        
        return Channel(
            id = stableId,
            name = attributes[ATTR_TVG_NAME]?.let { cleanChannelName(it) } ?: cleanTitle,
            url = urlLine.trim(),
            logoUrl = logoUrl,
            category = groupTitle,
            type = contentType,
            epgId = attributes[ATTR_TVG_ID],
            groupTitle = groupTitle,
            isFavorite = false,
            isLocked = false
        )
    }
    
    companion object {
        private const val EXT_M3U = "#EXTM3U"
        private const val EXT_INF = "#EXTINF"

        // Attributs standards IPTV
        private const val ATTR_TVG_ID = "tvg-id"
        private const val ATTR_TVG_NAME = "tvg-name"
        private const val ATTR_TVG_LOGO = "tvg-logo"
        private const val ATTR_TVG_GROUP = "group-title"
        private const val ATTR_RADIO = "radio"

        // Patterns regex compilés pour performance
        private val DURATION_REGEX = "#EXTINF:(-?\\d+)".toRegex()
        private val ATTR_QUOTED_REGEX = "([\\w-]+)=\"([^\"]*)\"".toRegex()
        private val ATTR_UNQUOTED_REGEX = "([\\w-]+)=([^\\s,]+)".toRegex()

        // Patterns S01E01, S1E1, 1x01, E01, EP01 — indiquent une série
        private val SERIES_EPISODE_REGEX = Regex(
            """(?i)(S\d{1,2}E\d{1,2}|\d{1,2}x\d{1,2}\b|[.\s_]E\d{1,2}[.\s_]|[.\s_]EP\d{1,3}[.\s_]|Saison\s*\d|Season\s*\d|Episode\s*\d)""",
            RegexOption.IGNORE_CASE
        )

        // Mots-clés dans group-title indiquant une série
        private val SERIES_GROUP_KEYWORDS = listOf(
            "series", "série", "serie", "épisode", "episode", "saison", "season",
            "tv show", "tvshow", "miniserie", "feuilleton", "soap"
        )

        // Mots-clés dans group-title indiquant un film
        private val MOVIE_GROUP_KEYWORDS = listOf(
            "film", "movie", "cinema", "vod", "movies", "films", "long métrage",
            "long-metrage", "feature"
        )
    }

    /**
     * Détecte le type de contenu avec heuristique avancée
     * Ordre de priorité: Radio > Série > Film > Live
     */
    private fun detectContentType(
        extInfLine: String,
        urlLine: String,
        duration: Int,
        attributes: Map<String, String>
    ): ContentType {
        val groupTitle = (attributes[ATTR_TVG_GROUP] ?: "").lowercase()
        val titlePart = extInfLine.substringAfterLast(",", "").trim()

        // 1. Radio
        if (attributes[ATTR_RADIO] == "true" ||
            urlLine.contains(".mp3", ignoreCase = true) ||
            urlLine.contains(".aac", ignoreCase = true) ||
            extInfLine.contains("radio=\"true\"")
        ) return ContentType.RADIO

        // 2. Séries — priorité sur films (un épisode peut avoir durée > 0)
        val isSeriesByUrl = urlLine.contains("/series/", ignoreCase = true) ||
            urlLine.contains("/show/", ignoreCase = true) ||
            urlLine.contains("/episode/", ignoreCase = true)
        val isSeriesByTitle = SERIES_EPISODE_REGEX.containsMatchIn(titlePart)
        val isSeriesByGroup = SERIES_GROUP_KEYWORDS.any { groupTitle.contains(it) }
        val isSeriesByExtInf = SERIES_EPISODE_REGEX.containsMatchIn(extInfLine)

        if (isSeriesByUrl || isSeriesByTitle || isSeriesByGroup || isSeriesByExtInf) {
            return ContentType.VOD_SERIES
        }

        // 3. Films
        val isMovieByUrl = urlLine.contains("/movie/", ignoreCase = true) ||
            urlLine.contains("/vod/", ignoreCase = true) ||
            urlLine.contains(".mp4", ignoreCase = true) ||
            urlLine.contains(".mkv", ignoreCase = true) ||
            urlLine.contains(".avi", ignoreCase = true)
        val isMovieByGroup = MOVIE_GROUP_KEYWORDS.any { groupTitle.contains(it) }
        val isMovieByDuration = duration > 0  // durée fixe = VOD

        if (isMovieByUrl || isMovieByGroup || isMovieByDuration) {
            return ContentType.VOD_MOVIE
        }

        // 4. Live par défaut
        return ContentType.LIVE_TV
    }
    
    /**
     * Construit le titre du groupe avec fallback
     */
    private fun buildGroupTitle(
        attributes: Map<String, String>, 
        title: String,
        contentType: ContentType
    ): String {
        // Priorité: attribut group-title > détection par mot-clé dans titre > défaut par type
        return attributes[ATTR_TVG_GROUP] ?: detectGroupFromTitle(title) ?: when (contentType) {
            ContentType.LIVE_TV -> "Chaînes TV"
            ContentType.LIVE_SPORTS -> "Sports"
            ContentType.LIVE_NEWS -> "Actualités"
            ContentType.RADIO -> "Radio"
            ContentType.VOD_MOVIE -> "Films"
            ContentType.VOD_SERIES -> "Séries"
            ContentType.SERIES_EPISODE -> "Épisodes"
        }
    }
    
    /**
     * Détecte le groupe à partir de mots-clés dans le titre
     */
    private fun detectGroupFromTitle(title: String): String? {
        val lowerTitle = title.lowercase()
        val groupMappings = mapOf(
            "france" to "France",
            "french" to "France",
            "fr " to "France",
            "belgique" to "Belgique",
            "suisse" to "Suisse",
            "canada" to "Canada",
            "sport" to "Sports",
            "football" to "Sports",
            "cinema" to "Cinéma",
            "movie" to "Cinéma",
            "film" to "Cinéma",
            "news" to "Actualités",
            "actualite" to "Actualités",
            "documentary" to "Documentaires",
            "kids" to "Enfants",
            "enfant" to "Enfants",
            "music" to "Musique",
            "musique" to "Musique",
            "religious" to "Religion",
            "religion" to "Religion"
        )
        
        return groupMappings.entries.firstOrNull { (keyword, _) ->
            lowerTitle.contains(keyword)
        }?.value
    }
    
    /**
     * Nettoie le nom de la chaîne (supprime les qualités, résolutions, etc.)
     */
    private fun cleanChannelName(name: String): String {
        return name
            .replace(Regex("\\s*(FHD|HD|SD|UHD|4K|8K|H265|HEVC|AVC)\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\d+p\\s*$"), "")
            .replace(Regex("\\s*\\[.*?\\]"), "")
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .trim()
    }
    
    /**
     * Nettoie l'URL du logo (supporte les URLs relatives)
     */
    private fun cleanLogoUrl(url: String): String {
        return when {
            url.startsWith("http://", ignoreCase = true) || 
            url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("//") -> "https:$url"
            else -> url
        }
    }
    
    /**
     * Génère un ID stable et unique pour la chaîne
     */
    private fun generateStableId(playlistId: String, url: String, name: String, index: Int): String {
        // Hash combiné pour stabilité entre les mises à jour
        val hashInput = "${playlistId}_${name}_${url}"
        val hash = hashInput.hashCode().toString().replace("-", "n")
        return "${playlistId}_${hash}_${index}"
    }
    
    /**
     * Parse les attributs de la ligne EXTINF avec patterns compilés
     */
    private fun parseAttributes(line: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        
        // Attributs avec guillemets
        ATTR_QUOTED_REGEX.findAll(line).forEach { matchResult ->
            val (key, value) = matchResult.destructured
            attributes[key] = value
        }
        
        // Attributs sans guillemets (fallback)
        ATTR_UNQUOTED_REGEX.findAll(line).forEach { matchResult ->
            val (key, value) = matchResult.destructured
            if (!attributes.containsKey(key)) {
                attributes[key] = value
            }
        }
        
        return attributes
    }
    
    /**
     * Détecte si le contenu est un fichier M3U valide
     */
    fun isValidM3U(content: String): Boolean {
        return content.trim().startsWith(EXT_M3U, ignoreCase = true)
    }
}
