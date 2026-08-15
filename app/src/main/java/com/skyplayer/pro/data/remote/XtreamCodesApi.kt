package com.skyplayer.pro.data.remote

import androidx.annotation.Keep
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.XtreamAuthResponse
import com.skyplayer.pro.data.model.XtreamCategory
import com.skyplayer.pro.data.model.XtreamEpgResponse
import com.skyplayer.pro.data.model.XtreamSeries
import com.skyplayer.pro.data.model.XtreamStream
import com.skyplayer.pro.data.model.XtreamVodDetails
import com.skyplayer.pro.data.model.XtreamEpisode
import com.skyplayer.pro.data.model.XtreamSeason
import com.skyplayer.pro.data.model.XtreamSeriesDetails
import com.skyplayer.pro.data.organizer.ContentClassifier
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import timber.log.Timber
import java.io.InputStreamReader

/**
 * Interface pour l'API Xtream Codes
 * Compatible avec la plupart des panels IPTV (Xtream UI, ZapX, etc.)
 */
@Keep
interface XtreamCodesApi {

    /**
     * Authentification et informations générales du serveur
     */
    @GET
    suspend fun authenticate(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<XtreamAuthResponse>

    /**
     * Récupérer les catégories (Live, VOD ou Séries)
     */
    @GET
    suspend fun getCategories(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String // "get_live_categories", "get_vod_categories", "get_series_categories"
    ): Response<List<XtreamCategory>>



    /**
     * Récupérer les flux d'une catégorie spécifique
     */
    @Streaming
    @GET
    suspend fun getStreams(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // "get_live_streams", "get_vod_streams"
        @Query("category_id") categoryId: String? = null
    ): Response<ResponseBody>

    /**
     * Récupérer les séries
     */
    @Streaming
    @GET
    suspend fun getSeriesList(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): Response<ResponseBody>



    /**
     * Récupérer les détails d'un film ou d'une série
     */
    @GET
    suspend fun getVodDetails(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // "get_vod_info" ou "get_series_info"
        @Query("vod_id") vodId: Int? = null,
        @Query("series_id") seriesId: Int? = null
    ): Response<XtreamVodDetails>

    /**
     * Récupérer les détails d'une série avec ses saisons et épisodes
     */
    @Streaming
    @GET
    suspend fun getSeriesDetails(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Int
    ): Response<ResponseBody>

    /**
     * Récupérer l'EPG pour un stream spécifique
     */
    @GET
    suspend fun getShortEpg(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: Int
    ): Response<XtreamEpgResponse>

    companion object {
        private val gson = Gson()

        /**
         * Parse un flux JSON de streams Xtream de manière optimisée (Streaming)
         */
        suspend fun parseStreamsStream(responseBody: ResponseBody): List<XtreamStream> = withContext(Dispatchers.IO) {
            val streams = mutableListOf<XtreamStream>()
            val reader = JsonReader(InputStreamReader(responseBody.byteStream(), "UTF-8"))
            try {
                when (reader.peek()) {
                    com.google.gson.stream.JsonToken.BEGIN_ARRAY -> {
                        reader.beginArray()
                        var count = 0
                        while (reader.hasNext()) {
                            try {
                                val stream: XtreamStream = gson.fromJson(reader, XtreamStream::class.java)
                                if (stream.streamId > 0 || stream.name.isNotBlank()) {
                                    streams.add(stream)
                                    count++
                                    if (count % 500 == 0) yield()
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Entrée stream Xtream ignorée (mal formée)")
                            }
                        }
                        reader.endArray()
                    }
                    com.google.gson.stream.JsonToken.BEGIN_OBJECT -> {
                        Timber.w("Réponse Xtream streams = objet JSON (erreur serveur probable)")
                    }
                    else -> Timber.w("Réponse Xtream streams inattendue: ${reader.peek()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Erreur parsing flux Xtream streams")
            } finally {
                reader.close()
            }
            streams
        }

        /**
         * Parse un flux JSON de séries Xtream de manière optimisée (Streaming)
         */
        suspend fun parseSeriesStream(responseBody: ResponseBody): List<XtreamSeries> = withContext(Dispatchers.IO) {
            val seriesList = mutableListOf<XtreamSeries>()
            val reader = JsonReader(InputStreamReader(responseBody.byteStream(), "UTF-8"))
            try {
                reader.beginArray()
                var count = 0
                while (reader.hasNext()) {
                    val series: XtreamSeries = gson.fromJson(reader, XtreamSeries::class.java)
                    seriesList.add(series)
                    count++
                    if (count % 500 == 0) yield()
                }
                reader.endArray()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                reader.close()
            }
            seriesList
        }

        /**
         * Parse les détails d'une série en streaming
         */
        suspend fun parseSeriesDetailsStream(responseBody: ResponseBody): XtreamSeriesDetails? = withContext(Dispatchers.IO) {
            try {
                return@withContext gson.fromJson<XtreamSeriesDetails>(
                    InputStreamReader(responseBody.byteStream(), "UTF-8"),
                    XtreamSeriesDetails::class.java
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Construit l'URL de streaming pour Live TV
         */
        fun buildLiveUrl(baseUrl: String, username: String, password: String, streamId: String, extension: String = "ts"): String {
            return "$baseUrl/live/$username/$password/$streamId.$extension"
        }

        /**
         * Construit l'URL de streaming pour VOD
         */
        fun buildVodUrl(baseUrl: String, username: String, password: String, streamId: String, extension: String = "mp4"): String {
            return "$baseUrl/movie/$username/$password/$streamId.$extension"
        }

        /**
         * Construit l'URL de streaming pour Séries
         */
        fun buildSeriesUrl(baseUrl: String, username: String, password: String, streamId: String, extension: String = "mp4"): String {
            return "$baseUrl/series/$username/$password/$streamId.$extension"
        }
    }
}

// ── Extension functions (hors interface Retrofit pour éviter "HTTP method annotation is required") ──

suspend fun XtreamCodesApi.getLiveCategories(fullUrl: String, username: String, password: String): List<XtreamCategory> =
    getCategories(fullUrl, username, password, "get_live_categories").body() ?: emptyList()

suspend fun XtreamCodesApi.getVodCategories(fullUrl: String, username: String, password: String): List<XtreamCategory> =
    getCategories(fullUrl, username, password, "get_vod_categories").body() ?: emptyList()

suspend fun XtreamCodesApi.getSeriesCategories(fullUrl: String, username: String, password: String): List<XtreamCategory> =
    getCategories(fullUrl, username, password, "get_series_categories").body() ?: emptyList()

suspend fun XtreamCodesApi.getLiveStreams(fullUrl: String, username: String, password: String): List<XtreamStream> {
    val response = getStreams(fullUrl, username, password, "get_live_streams")
    if (!response.isSuccessful) {
        throw Exception("Erreur serveur Xtream (${response.code()}) lors de get_live_streams")
    }
    return response.body()?.let { XtreamCodesApi.parseStreamsStream(it) } ?: emptyList()
}

suspend fun XtreamCodesApi.getVodStreams(fullUrl: String, username: String, password: String): List<XtreamStream> {
    val response = getStreams(fullUrl, username, password, "get_vod_streams")
    if (!response.isSuccessful) {
        throw Exception("Erreur serveur Xtream (${response.code()}) lors de get_vod_streams")
    }
    return response.body()?.let { XtreamCodesApi.parseStreamsStream(it) } ?: emptyList()
}

suspend fun XtreamCodesApi.getSeries(fullUrl: String, username: String, password: String): List<XtreamSeries> {
    val response = getSeriesList(fullUrl, username, password, "get_series")
    return response.body()?.let { XtreamCodesApi.parseSeriesStream(it) } ?: emptyList()
}

/**
 * Extension pour convertir les streams Xtream en modèles Channel
 */
fun XtreamStream.toChannel(
    baseUrl: String,
    username: String,
    password: String,
    playlistId: String,
    categoryName: String? = null,
    forcedType: ContentType? = null
): Channel {
    // 1. DÉTERMINATION DU TYPE (Basée uniquement sur l'endpoint/explicitType pour Xtream)
    val resolvedType = forcedType ?: ContentClassifier.inferContentType(
        name = name,
        groupTitle = categoryName ?: categoryId,
        url = directSource ?: streamType,
        duration = if (streamType == "movie") 1 else -1,
        explicitType = streamType,
        isXtream = true
    )

    val normalizedBase = baseUrl.trim().trimEnd('/')
    val extension = containerExtension?.trim()?.trimStart('.')?.takeIf { it.isNotBlank() }
        ?: when (resolvedType) {
            ContentType.VOD_MOVIE -> "mp4"
            ContentType.VOD_SERIES, ContentType.SERIES_EPISODE -> "mp4"
            else -> if (streamType.equals("live", ignoreCase = true)) "ts" else "m3u8"
        }

    // 2. CONSTRUCTION DE L'URL
    val streamUrl = when (resolvedType) {
        ContentType.VOD_MOVIE -> directSource?.takeIf { it.isNotBlank() }
            ?: XtreamCodesApi.buildVodUrl(normalizedBase, username, password, streamId.toString(), extension)
        ContentType.VOD_SERIES, ContentType.SERIES_EPISODE -> directSource?.takeIf { it.isNotBlank() }
            ?: XtreamCodesApi.buildSeriesUrl(normalizedBase, username, password, streamId.toString(), extension)
        else -> directSource?.takeIf { it.isNotBlank() }
            ?: XtreamCodesApi.buildLiveUrl(normalizedBase, username, password, streamId.toString(), extension)
    }

    val resolvedCategory = ContentClassifier.inferCategory(
        name = name,
        groupTitle = categoryName ?: categoryId,
        contentType = resolvedType
    )

    return Channel(
        id = "${playlistId}_${streamId}",
        name = name.ifBlank { "Contenu sans nom" },
        url = streamUrl,
        logoUrl = streamIcon,
        category = resolvedCategory,
        type = resolvedType,
        epgId = epgChannelId,
        groupTitle = resolvedCategory
    )
}

/**
 * Extension pour convertir les séries Xtream en modèles Channel
 */
fun XtreamSeries.toChannel(
    baseUrl: String,
    username: String,
    password: String,
    playlistId: String,
    categoryName: String? = null
): Channel {
    val resolvedType = ContentType.VOD_SERIES
    val streamUrl = XtreamCodesApi.buildSeriesUrl(baseUrl, username, password, seriesId.toString())
    
    val resolvedCategory = ContentClassifier.inferCategory(
        name = name,
        groupTitle = categoryName ?: categoryId,
        contentType = resolvedType
    )

    return Channel(
        id = "${playlistId}_${seriesId}",
        name = name.ifBlank { "Série sans nom" },
        url = streamUrl,
        logoUrl = cover,
        category = resolvedCategory,
        type = resolvedType,
        groupTitle = resolvedCategory
    )
}
