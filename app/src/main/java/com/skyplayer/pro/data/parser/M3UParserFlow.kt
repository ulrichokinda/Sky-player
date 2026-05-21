package com.skyplayer.pro.data.parser

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Parser M3U asynchrone haute performance avec Flow
 * 
 * Capacités :
 * - Traite 50,000+ entrées sans bloquer l'UI
 * - Renvoie les chaînes par paquets de 100 via Flow
 * - Affichage des premiers résultats instantané (~100ms)
 * - Parsing incrémental avec backpressure handling
 * - Supporte les formats M3U/M3U8 avec attributs étendus
 */
class M3UParserFlow {

    companion object {
        private const val TAG = "M3UParserFlow"
        private const val EXT_M3U = "#EXTM3U"
        private const val EXT_INF = "#EXTINF"
        private const val BATCH_SIZE = 100 // Paquets de 100 chaînes
        
        // Regex optimisés pour performance
        private val DURATION_REGEX = "#EXTINF:(-?\\d+)".toRegex()
        private val ATTR_QUOTED_REGEX = "([\\w-]+)=\"([^\"]*)\"".toRegex()
        private val ATTR_UNQUOTED_REGEX = "([\\w-]+)=([^\\s,]+)".toRegex()
    }

    /**
     * Parse une playlist M3U et émet les chaînes par paquets via Flow
     * 
     * @param content Contenu M3U complet
     * @param playlistId ID de la playlist pour les IDs des chaînes
     * @param sourceUrl URL source (pour le logging)
     * @return Flow<List<Channel>> Émet des paquets de 100 chaînes
     */
    fun parseAsFlow(content: String, playlistId: String, sourceUrl: String = ""): Flow<List<Channel>> = flow {
        val batch = ArrayList<Channel>(BATCH_SIZE)
        var currentLine: String? = null
        var lineNumber = 0
        var extInfLine: String? = null
        var parsedCount = 0
        
        try {
            // Lecture ligne par ligne (streaming) pour mémoire constante
            val lines = content.lineSequence().iterator()
            
            // Vérifier l'en-tête M3U
            if (lines.hasNext()) {
                val firstLine = lines.next()
                lineNumber++
                if (!firstLine.trim().startsWith(EXT_M3U)) {
                    Timber.w("⚠️ Format M3U non standard détecté: $sourceUrl")
                }
            }
            
            // Parsing incrémental
            while (lines.hasNext() && currentCoroutineContext().isActive) {
                currentLine = lines.next()
                lineNumber++
                
                when {
                    // Ligne #EXTINF - métadonnées
                    currentLine.startsWith(EXT_INF) -> {
                        extInfLine = currentLine
                    }
                    
                    // Ligne URL - créer la chaîne
                    currentLine.isNotBlank() && !currentLine.startsWith("#") -> {
                        extInfLine?.let { extLine ->
                            val channel = parseChannel(extLine, currentLine, playlistId, lineNumber)
                            if (channel != null) {
                                batch.add(channel)
                                parsedCount++
                                
                                // Émettre le paquet quand on atteint BATCH_SIZE
                                if (batch.size >= BATCH_SIZE) {
                                    emit(batch.toList())
                                    batch.clear()
                                }
                            }
                        }
                        extInfLine = null
                    }
                }
            }
            
            // Émettre le dernier paquet (reste)
            if (batch.isNotEmpty()) {
                emit(batch.toList())
            }
            
            Timber.i("✅ Parsing terminé: $parsedCount chaînes trouvées dans $sourceUrl")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur parsing M3U ligne $lineNumber: $currentLine")
            // Émettre ce qu'on a déjà parsé
            if (batch.isNotEmpty()) {
                emit(batch.toList())
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse depuis une URL avec streaming de résultats
     * Télécharge et parse simultanément pour affichage rapide
     */
    fun parseFromUrlAsFlow(
        url: String, 
        playlistId: String,
        okHttpClient: okhttp3.OkHttpClient
    ): Flow<List<Channel>> = flow {
        Timber.d("🌐 Début téléchargement: $url")
        
        try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "SkyPlayerPro/1.0")
                .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            
            // Streaming du body avec buffer
            val body = response.body
            if (body != null) {
                body.byteStream().use { inputStream ->
                    val batch = ArrayList<Channel>(BATCH_SIZE)
                    var extInfLine: String? = null
                    var lineNumber = 0
                    var parsedCount = 0
                    var lastEmitTime = System.currentTimeMillis()
                    
                    BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8"))).use { reader ->
                        var line: String? = reader.readLine()
                        
                        // Vérifier en-tête
                        if (line != null && !line.trim().startsWith(EXT_M3U)) {
                            Timber.w("⚠️ Format M3U non standard: $url")
                        }
                        
                        while (line != null && currentCoroutineContext().isActive) {
                            lineNumber++
                            
                            val currentLine = line
                            when {
                                currentLine.startsWith(EXT_INF) -> extInfLine = currentLine
                                
                                currentLine.isNotBlank() && !currentLine.startsWith("#") -> {
                                    extInfLine?.let { extLine ->
                                        val channel = parseChannel(extLine, currentLine, playlistId, lineNumber)
                                        if (channel != null) {
                                            batch.add(channel)
                                            parsedCount++
                                            
                                            // Émettre par batch OU si > 200ms depuis dernier emit
                                            val now = System.currentTimeMillis()
                                            if (batch.size >= BATCH_SIZE || (now - lastEmitTime > 200 && batch.isNotEmpty())) {
                                                emit(batch.toList())
                                                batch.clear()
                                                lastEmitTime = now
                                                
                                                // Petit délai pour laisser l'UI respirer
                                                if (parsedCount % 1000 == 0) {
                                                    delay(1)
                                                }
                                            }
                                        }
                                    }
                                    extInfLine = null
                                }
                            }
                            
                            val nextLine = reader.readLine()
                            line = nextLine
                        }
                    }
                    
                    // Dernier paquet
                    if (batch.isNotEmpty()) {
                        emit(batch.toList())
                    }
                    
                    Timber.i("✅ Stream parsing terminé: $parsedCount chaînes")
                }
            } else {
                throw Exception("Body vide")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur téléchargement: $url")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse une ligne #EXTINF + URL en objet Channel
     * Optimisé pour performance avec regex compilés
     */
    private fun parseChannel(
        extInfLine: String, 
        urlLine: String, 
        playlistId: String,
        lineNumber: Int
    ): Channel? {
        return try {
            // Extraire la durée
            val duration = DURATION_REGEX.find(extInfLine)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            
            // Extraire tous les attributs
            val attrs = mutableMapOf<String, String>()
            
            // Attributs entre guillemets
            ATTR_QUOTED_REGEX.findAll(extInfLine).forEach { match ->
                attrs[match.groupValues[1]] = match.groupValues[2]
            }
            
            // Attributs sans guillemets
            ATTR_UNQUOTED_REGEX.findAll(extInfLine).forEach { match ->
                attrs[match.groupValues[1]] = match.groupValues[2]
            }
            
            // Extraire le nom (après la dernière virgule)
            val commaIndex = extInfLine.lastIndexOf(",")
            val name = if (commaIndex > 0) {
                extInfLine.substring(commaIndex + 1).trim()
            } else {
                attrs["tvg-name"] ?: "Chaîne $lineNumber"
            }
            
            // Déterminer le type de contenu
            val contentType = when {
                urlLine.contains("/movie/") || attrs["type"] == "movie" -> ContentType.VOD_MOVIE
                urlLine.contains("/series/") || attrs["type"] == "series" -> ContentType.VOD_SERIES
                duration > 0 || attrs["type"] == "vod" -> ContentType.VOD_MOVIE
                attrs["radio"] == "true" -> ContentType.RADIO
                else -> ContentType.LIVE_TV
            }
            
            Channel(
                id = "${playlistId}_${attrs["tvg-id"] ?: System.nanoTime()}_${lineNumber}",
                name = name,
                url = urlLine.trim(),
                logoUrl = attrs["tvg-logo"],
                category = attrs["group-title"] ?: "Général",
                type = contentType,
                epgId = attrs["tvg-id"],
                groupTitle = attrs["group-title"]
            )
        } catch (e: Exception) {
            Timber.w("⚠️ Ligne $lineNumber ignorée: ${e.message}")
            null
        }
    }

    /**
     * Version avec debounce pour limiter les mises à jour UI
     * Émet au maximum toutes les 100ms
     */
    fun parseAsFlowWithDebounce(
        content: String, 
        playlistId: String, 
        sourceUrl: String = "",
        debounceMs: Long = 100L
    ): Flow<List<Channel>> {
        return parseAsFlow(content, playlistId, sourceUrl)
            .debounce(debounceMs)
    }
}
