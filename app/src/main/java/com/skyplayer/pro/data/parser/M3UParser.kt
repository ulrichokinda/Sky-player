package com.skyplayer.pro.data.parser

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.organizer.ContentClassifier
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream
import java.io.File
import java.net.URL

/**
 * Parser M3U haute performance pour playlists IPTV
 * Supporte les formats M3U et M3U8 avec attributs étendus et compression GZIP
 */
class M3UParser(private val okHttpClient: OkHttpClient) {


    /**
     * Parse une playlist M3U depuis une URL avec timeout configurable
     * Optimisé pour réseaux lents avec retry intégré et mode Streaming
     */
    suspend fun parseFromUrl(url: String, playlistId: String, maxRetries: Int = 3): List<Channel> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    Timber.i("📥 Tentative ${attempt + 1}/$maxRetries : téléchargement M3U depuis $url")
                    val request = Request.Builder()
                        .url(url)
                        // User-Agent standard pour éviter les erreurs réseau M3U
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain, */*")
                        .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        Timber.i("📥 Réponse HTTP ${response.code} pour $url")
                        if (!response.isSuccessful) {
                            throw Exception("Erreur HTTP ${response.code}")
                        }

                        val body = response.body ?: throw Exception("Corps de réponse vide")
                        val contentLength = body.contentLength()
                        Timber.i("📥 Taille du fichier M3U : ${if (contentLength > 0) "${contentLength / 1024} Ko" else "inconnue"}")
                        
                        // Utilisation de byteStream() pour ne pas charger tout en RAM
                        return@withContext parseFromInputStream(body.byteStream(), playlistId, url)
                    }
                } catch (e: Exception) {
                    lastException = e
                    Timber.e(e, "❌ Tentative ${attempt + 1}/$maxRetries échouée pour $url")
                    if (attempt < maxRetries - 1) {
                        val delayMs = 2000L * (attempt + 1)
                        Timber.i("⌛ Attente de $delayMs ms avant nouvelle tentative...")
                        kotlinx.coroutines.delay(delayMs) // Backoff exponentiel
                    }
                }
            }

            Timber.e(lastException, "❌ Échec parsing M3U après $maxRetries tentatives: $url")
            throw lastException ?: Exception("Échec inattendu")
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
                throw e
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
        return withContext(Dispatchers.IO) {
            val bufferedStream = BufferedInputStream(inputStream)

            // Détection GZIP (Magic bytes: 0x1f 0x8b)
            // Détection GZIP (Magic bytes: 0x1f 0x8b)
            val finalStream = if (isGzipped(bufferedStream)) {
                Timber.d("Compression GZIP détectée pour $sourceUrl")
                GZIPInputStream(bufferedStream)
            } else {
                bufferedStream
            }

            // Content sniffing : lire les premiers octets pour détecter du HTML/JSON/XML
            // avant de tenter le parsing M3U — évite les erreurs cryptiques
            val sniffer = BufferedInputStream(finalStream, 4096)
            val isHtmlContent = sniffHtml(sniffer)

            if (isHtmlContent) {
                // Lire un extrait pour le message d'erreur
                val preview = readPreview(sniffer)
                val hint = when {
                    preview.contains("login", ignoreCase = true) || preview.contains("connect", ignoreCase = true) ->
                        "Le serveur renvoie une page de connexion. L'URL nécessite peut-être une authentification."
                    preview.contains("captcha", ignoreCase = true) ->
                        "Le serveur affiche un captcha. Réessayez plus tard."
                    preview.contains("cloudflare", ignoreCase = true) || preview.contains("cf-", ignoreCase = true) ->
                        "Le serveur est protégé par Cloudflare. Essayez une autre URL ou contactez le fournisseur."
                    preview.contains("expired", ignoreCase = true) || preview.contains("expiré", ignoreCase = true) ->
                        "L'abonnement ou le lien semble expiré."
                    else ->
                        "Le serveur renvoie une page web (HTML) au lieu d'une playlist M3U. Vérifiez l'URL ou contactez votre fournisseur IPTV."
                }
                Timber.e("❌ Contenu HTML détecté au lieu de M3U depuis $sourceUrl")
                throw Exception(hint)
            }

            val reader = BufferedReader(InputStreamReader(sniffer, Charsets.UTF_8))
            val channels = mutableListOf<Channel>()
            var extInfLine: String? = null
            var lineNumber = 0
            var firstLine = true
            var errorsCount = 0

            reader.useLines { lines ->
                for (line in lines) {
                    lineNumber++
                    val trimmedLine = stripBom(line.trim())

                    if (lineNumber % 1000 == 0) {
                        Timber.d("📄 Parsing en cours... Ligne $lineNumber, ${channels.size} chaînes trouvées")
                        yield() // Libérer le thread UI toutes les 1000 lignes
                    }

                    if (trimmedLine.isEmpty() || (trimmedLine.startsWith("#") && !trimmedLine.startsWith("#EXT", ignoreCase = true))) {
                        continue
                    }

                    if (firstLine) {
                        if (trimmedLine.startsWith(EXT_M3U, ignoreCase = true)) {
                            firstLine = false
                            // Extraire l'URL EPG du header x-tvg-url ou url-tvg
                            extractEpgUrl(trimmedLine)?.let { epgUrl ->
                                Timber.i("📌 URL EPG trouvée dans le header M3U : $epgUrl")
                                onEpgUrlFound(epgUrl)
                            }
                            continue
                        } else {
                            Timber.w("⚠️ Contenu invalide à la ligne $lineNumber - pas d'en-tête #EXTM3U")
                            // On continue quand même au cas où le fichier est mal formé mais contient des #EXTINF
                            firstLine = false
                        }
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
                                        val resolvedUrl = resolveStreamUrl(trimmedLine, sourceUrl)
                                        val channel = parseChannel(extInf, resolvedUrl, playlistId)
                                        channels.add(channel)
                                    } else {
                                        val resolved = resolveStreamUrl(trimmedLine, sourceUrl)
                                        if (isValidStreamUrl(resolved)) {
                                            val channel = parseChannel(extInf, resolved, playlistId)
                                            channels.add(channel)
                                        } else {
                                            Timber.w("⚠️ URL de flux invalide à la ligne $lineNumber : ${trimmedLine.take(50)}...")
                                            errorsCount++
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "⚠️ Erreur parsing ligne $lineNumber")
                                    errorsCount++
                                }
                                extInfLine = null
                            }
                        }
                    }
                }
            }

            Timber.i("✅ M3U parsé: ${channels.size} chaînes trouvées depuis ${sourceUrl ?: "stream"}, $errorsCount erreurs ignorées")
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
     * Sniff les premiers octets du flux pour détecter du HTML.
     * Ne consomme pas le flux (mark/reset).
     * @return true si le contenu est du HTML
     */
    private fun sniffHtml(stream: BufferedInputStream): Boolean {
        val bufferSize = 512
        stream.mark(bufferSize)
        val bytes = ByteArray(bufferSize)
        var totalRead = 0
        while (totalRead < bufferSize) {
            val read = stream.read(bytes, totalRead, bufferSize - totalRead)
            if (read == -1) break
            totalRead += read
        }
        stream.reset()

        if (totalRead == 0) return false

        val head = String(bytes, 0, totalRead, Charsets.UTF_8).trimStart()
            .lowercase()
            .replace(Regex("\\s+"), " ")
        return head.startsWith("<!doctype") ||
               head.startsWith("<html") ||
               head.startsWith("<head") ||
               head.startsWith("<?xml") && head.contains("<html") ||
               head.contains("<html") ||
               head.startsWith("{")  // JSON (Xtream API response)
    }

    /**
     * Lit un extrait du stream pour le message d'erreur (apres sniffHtml).
     * @return les 500 premiers caracteres lisibles
     */
    private fun readPreview(stream: BufferedInputStream): String {
        val bufferSize = 1024
        stream.mark(bufferSize)
        val bytes = ByteArray(bufferSize)
        var totalRead = 0
        while (totalRead < bufferSize) {
            val read = stream.read(bytes, totalRead, bufferSize - totalRead)
            if (read == -1) break
            totalRead += read
        }
        stream.reset()

        return String(bytes, 0, totalRead, Charsets.ISO_8859_1)
            .replace(Regex("[^ -~\n\r\t]"), " ")
            .take(500)
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
     * Résout une URL relative par rapport à la playlist source.
     */
    private fun resolveStreamUrl(urlLine: String, sourceUrl: String?): String {
        val trimmed = urlLine.trim()
        if (isValidStreamUrl(trimmed)) return trimmed
        if (sourceUrl.isNullOrBlank()) return trimmed

        return runCatching {
            URL(URL(sourceUrl), trimmed).toString()
        }.getOrElse {
            runCatching {
                val base = URL(sourceUrl)
                val pathBase = base.path.substringBeforeLast('/', "")
                URL(base, "$pathBase/$trimmed").toString()
            }.getOrDefault(trimmed)
        }
    }

    private fun stripBom(value: String): String =
        if (value.startsWith("\uFEFF")) value.removePrefix("\uFEFF") else value

    /**
     * Parse une ligne EXTINF et l'URL associée avec métadonnées enrichies
     */
    private fun parseChannel(
        extInfLine: String,
        urlLine: String,
        playlistId: String
    ): Channel {
        // Extraire la durée et le titre
        val duration = DURATION_REGEX.find(extInfLine)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        // Extraire les attributs
        val attributes = parseAttributes(extInfLine)

        // Extraire le titre (dernière partie après la dernière virgule)
        val rawTitle = extInfLine.substringAfterLast(",", "Unknown").trim()
        val cleanTitle = cleanChannelName(rawTitle)

        val rawGroupTitle = attributes[ATTR_TVG_GROUP]

        // Déterminer le type de contenu avec heuristique améliorée
        val contentType = ContentClassifier.inferContentType(
            name = cleanTitle,
            groupTitle = rawGroupTitle,
            url = urlLine,
            duration = duration,
            explicitType = attributes["type"]
        )

        // Construire le groupe avec fallback intelligent
        val groupTitle = ContentClassifier.inferCategory(cleanTitle, rawGroupTitle, contentType)

        // Générer ID unique stable (tvg-id ou hash de l'URL — jamais de nanoTime/ligne)
        val stableId = M3UChannelId.forChannel(playlistId, attributes[ATTR_TVG_ID], urlLine)

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

        // Patterns regex compilés pour performance
        private val DURATION_REGEX = "#EXTINF:(-?\\d+)".toRegex()
        private val ATTR_QUOTED_REGEX = "([\\w-]+)=\"([^\"]*)\"".toRegex()
        private val ATTR_UNQUOTED_REGEX = "([\\w-]+)=([^\\s,]+)".toRegex()


    }



    /**
     * Nettoie le nom de la chaîne (supprime les qualités, résolutions, etc.)
     */
    private fun cleanChannelName(name: String): String {
        return name
            // Supprimer d'abord les tags entre crochets et parenthèses car ils peuvent contenir des suffixes de qualité
            .replace(Regex("\\s*\\[[^\\]]*]"), "")
            .replace(Regex("\\s*\\(.*?\\)"), "")
            // Ensuite supprimer les indicateurs de qualité en fin de nom
            .replace(Regex("\\s*(FHD|HD|SD|UHD|4K|8K|H265|HEVC|AVC)\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\d+p\\s*$", RegexOption.IGNORE_CASE), "")
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
}
