package com.skyplayer.pro.data.remote

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class ActivationResponse(
    val status: String?,
    val active: Boolean?,

    // Format Playlist M3U
    @SerializedName("playlist_url") val playlistUrl: String?,
    @SerializedName("playlist_name") val playlistName: String?,

    // Format Xtream Codes
    @SerializedName("xtream_server_url") val xtreamHost: String?,
    @SerializedName("xtream_username") val xtreamUser: String?,
    @SerializedName("xtream_password") val xtreamPassword: String?,

    @SerializedName("type") val playlistType: String? = "m3u"
)
