package com.skyplayer.pro.data.remote

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.XtreamAuthResponse
import com.skyplayer.pro.data.model.XtreamCategory
import com.skyplayer.pro.data.model.XtreamEpgResponse
import com.skyplayer.pro.data.model.XtreamSeries
import com.skyplayer.pro.data.model.XtreamStream
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * API Xtream Codes pour les serveurs IPTV
 * Supporte l'authentification et les endpoints standards avec URLs dynamiques
 */
interface XtreamCodesApi {

    /**
     * Authentification et récupération des infos utilisateur
     * URL complète: baseUrl/player_api.php?username=$user&password=$pass
     */
    @GET
    suspend fun authenticate(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponse

    /**
     * Récupère les catégories de Live TV
     */
    @GET
    suspend fun getLiveCategories(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    /**
     * Récupère les chaînes Live TV
     * URL complète: baseUrl/player_api.php?username=$user&password=$pass&action=get_live_streams
     */
    @GET
    suspend fun getLiveStreams(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): List<XtreamStream>

    /**
     * Récupère les catégories VOD
     */
    @GET
    suspend fun getVodCategories(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    /**
     * Récupère la liste des VOD (films)
     */
    @GET
    suspend fun getVodStreams(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<XtreamStream>

    /**
     * Récupère les catégories de séries
     */
    @GET
    suspend fun getSeriesCategories(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>

    /**
     * Récupère l'EPG pour un stream spécifique
     */
    @GET
    suspend fun getShortEpg(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: Int,
        @Query("action") action: String = "get_short_epg"
    ): XtreamEpgResponse

    /**
     * Récupère les séries
     */
    @GET
    suspend fun getSeries(
        @Url fullUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<XtreamSeries>
    
    companion object {
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

/**
 * Extension pour convertir les streams Xtream en modèles Channel
 */
fun XtreamStream.toChannel(
    baseUrl: String,
    username: String,
    password: String,
    playlistId: String,
    isVod: Boolean = false
): Channel {
    val contentType = when {
        isVod || streamType == "movie" -> ContentType.VOD_MOVIE
        streamType == "live" -> ContentType.LIVE_TV
        else -> ContentType.LIVE_TV
    }

    val streamUrl = when (contentType) {
        ContentType.LIVE_TV -> XtreamCodesApi.buildLiveUrl(baseUrl, username, password, streamId.toString())
        ContentType.VOD_MOVIE -> XtreamCodesApi.buildVodUrl(baseUrl, username, password, streamId.toString())
        else -> ""
    }

    return Channel(
        id = "${playlistId}_${streamId}",
        name = name ?: "Chaîne sans nom",
        url = streamUrl,
        logoUrl = streamIcon,
        category = categoryId?.ifEmpty { "Général" } ?: "Général",
        type = contentType,
        epgId = epgChannelId,
        groupTitle = categoryId ?: ""
    )
}
