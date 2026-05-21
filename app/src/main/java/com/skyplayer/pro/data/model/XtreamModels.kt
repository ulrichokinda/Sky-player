package com.skyplayer.pro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modèles de données pour l'API Xtream Codes
 */

data class XtreamCategory(
    @SerializedName("category_id")
    val id: String,
    @SerializedName("category_name")
    val name: String,
    @SerializedName("parent_id")
    val parentId: Int = 0
)

data class XtreamStream(
    @SerializedName("num")
    val num: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("stream_type")
    val streamType: String = "live",
    @SerializedName("stream_id")
    val streamId: Int = 0,
    @SerializedName("stream_icon")
    val streamIcon: String? = null,
    @SerializedName("epg_channel_id")
    val epgChannelId: String? = null,
    @SerializedName("added")
    val added: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("custom_sid")
    val customSid: String? = null,
    @SerializedName("tv_archive")
    val tvArchive: Int = 0,
    @SerializedName("direct_source")
    val directSource: String? = null,
    @SerializedName("tv_archive_duration")
    val tvArchiveDuration: Int = 0
)

data class XtreamSeries(
    @SerializedName("num")
    val num: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("series_id")
    val seriesId: Int = 0,
    @SerializedName("cover")
    val cover: String? = null,
    @SerializedName("plot")
    val plot: String? = null,
    @SerializedName("cast")
    val cast: String? = null,
    @SerializedName("director")
    val director: String? = null,
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("releaseDate")
    val releaseDate: String? = null,
    @SerializedName("last_modified")
    val lastModified: String? = null,
    @SerializedName("rating")
    val rating: String? = null,
    @SerializedName("rating_5based")
    val rating5Based: Double? = null,
    @SerializedName("backdrop_path")
    val backdropPath: List<String>? = null,
    @SerializedName("youtube_trailer")
    val youtubeTrailer: String? = null,
    @SerializedName("episode_run_time")
    val episodeRunTime: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null
)

data class XtreamEpgResponse(
    @SerializedName("epg_listings")
    val epgList: List<XtreamEpgListing>? = null
)

data class XtreamEpgListing(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("epg_id")
    val epgId: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("start")
    val start: String? = null,
    @SerializedName("end")
    val end: String? = null,
    @SerializedName("start_timestamp")
    val startTimestamp: Long? = null,
    @SerializedName("stop_timestamp")
    val stopTimestamp: Long? = null
)

data class XtreamAuthResponse(
    @SerializedName("user_info")
    val userInfo: XtreamUserInfo? = null,
    @SerializedName("server_info")
    val serverInfo: XtreamServerInfo? = null
)

data class XtreamUserInfo(
    @SerializedName("username")
    val username: String = "",
    @SerializedName("password")
    val password: String = "",
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("auth")
    private val _auth: Any? = 0,  // Peut être Int (1) ou String ("1")
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("exp_date")
    val expDate: String? = null,
    @SerializedName("is_trial")
    val isTrial: String? = null,
    @SerializedName("active_cons")
    val activeCons: Int = 0,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("max_connections")
    val maxConnections: Int = 0,
    @SerializedName("allowed_output_formats")
    val allowedOutputFormats: List<String>? = null
) {
    // Propriété calculée pour gérer auth comme Int ou String
    val auth: Int
        get() = when (_auth) {
            is Int -> _auth
            is String -> _auth.toIntOrNull() ?: 0
            else -> 0
        }
    
    // Vérifie si l'authentification est réussie
    val isAuthenticated: Boolean
        get() = auth == 1 || status?.equals("Active", ignoreCase = true) == true
}

data class XtreamServerInfo(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("port")
    val port: String = "",
    @SerializedName("https_port")
    val httpsPort: String = "",
    @SerializedName("server_protocol")
    val serverProtocol: String = "",
    @SerializedName("rtmp_port")
    val rtmpPort: String = "",
    @SerializedName("timezone")
    val timezone: String = "",
    @SerializedName("timestamp_now")
    val timestampNow: Long = 0,
    @SerializedName("time_now")
    val timeNow: String? = null
)
