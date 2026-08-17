package com.skyplayer.pro.data.parser

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.organizer.ContentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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
     * Parse une playlist M3U depuis un String et émet les chaînes par paquets via Flow
     * Optimisé pour utiliser une séquence de lignes sans créer de copies inutiles
     */
    fun parseAsFlow(content: String, playlistId: String, sourceUrl: String = ""): Flow<List<Channel>> = flow {
        val batch = ArrayList<Channel>(BATCH_SIZE)
        var currentLine: String? = null
        var lineNumber = 0
        var extInfLine: String? = null
        var parsedCount = 0

        try {
            // Utilisation d'un BufferedReader sur le String pour économiser la mémoire sur les gros fichiers
            content.reader().buffered().useLines { lines ->
                val iterator = lines.iterator()

                // Vérifier l'en-tête M3U
                if (iterator.hasNext()) {
                    val firstLine = iterator.next()
                    lineNumber++
                    if (!firstLine.trim().startsWith(EXT_M3U)) {
                        Timber.w("⚠️ Format M3U non standard détecté: $sourceUrl")
                    }
                }

                // Parsing incrémental
                while (iterator.hasNext() && currentCoroutineContext().isActive) {
                    currentLine = iterator.next()
                    lineNumber++

                    when {
                        currentLine!!.startsWith(EXT_INF) -> extInfLine = currentLine
                        currentLine!!.isNotBlank() && !currentLine!!.startsWith("#") -> {
                            extInfLine?.let { extLine ->
                                val channel = parseChannel(extLine, currentLine!!, playlistId, lineNumber)
                                if (channel != null) {
                                    batch.add(channel)
                                    parsedCount++

                                    if (batch.size >= BATCH_SIZE) {
                                        emit(batch.toList())
                                        batch.clear()
                                        // Laisser l'UI souffler un peu sur les gros fichiers
                                        if (parsedCount % 1000 == 0) delay(1)
                                    }
                                }
                            }
                            extInfLine = null
                        }
                    }
                }
            }

            if (batch.isNotEmpty()) emit(batch.toList())
            Timber.i("✅ Parsing terminé: $parsedCount chaînes")
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur parsing M3U ligne $lineNumber")
            if (batch.isNotEmpty()) emit(batch.toList())
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
                // Même User-Agent navigateur que partout ailleurs : certains serveurs
                // IPTV bloquent les User-Agents inconnus du type "SkyPlayerPro/1.0".
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain, */*")
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

                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
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

            val contentType = ContentClassifier.inferContentType(
                name = name,
                groupTitle = attrs["group-title"],
                url = urlLine,
                duration = duration,
                explicitType = attrs["type"]
            )
            val inferredCategory = ContentClassifier.inferCategory(name, attrs["group-title"], contentType)

            // ID déterministe : tvg-id si présent, sinon hash de l'URL. Stable entre
            // deux parses → favoris/historique préservés + REPLACE dédoublonne.
            Channel(
                id = M3UChannelId.forChannel(playlistId, attrs["tvg-id"], urlLine),
                name = name,
                url = urlLine.trim(),
                logoUrl = attrs["tvg-logo"],
                category = inferredCategory,
                type = contentType,
                epgId = attrs["tvg-id"],
                groupTitle = inferredCategory
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
    @OptIn(FlowPreview::class)
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
