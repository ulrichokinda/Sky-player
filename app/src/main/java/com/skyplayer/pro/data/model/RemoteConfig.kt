package com.skyplayer.pro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Configuration distante reçue via QR Code / Firebase
 * Supporte 2 formats : Xtream Codes et M3U direct
 */
sealed class RemoteConfig {
    
    /**
     * Format Xtream Codes
     * {
     *   "type": "xtream",
     *   "host": "http://serveur.com:8080",
     *   "user": "username",
     *   "pass": "password"
     * }
     */
    data class XtreamConfig(
        @SerializedName("host")
        val host: String = "",
        
        @SerializedName("user")
        val user: String = "",
        
        @SerializedName("pass")
        val pass: String = "",
        
        @SerializedName("createdAt")
        val createdAt: Long = System.currentTimeMillis()
    ) : RemoteConfig() {
        
        fun isValid(): Boolean {
            return host.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
        }
        
        fun toPlaylistUrl(): String {
            val baseUrl = host.trim().trimEnd('/')
            return "$baseUrl/get.php?username=$user&password=$pass&type=m3u_plus&output=ts"
        }
        
        fun toXtreamUrl(): String {
            return host.trim()
        }
    }
    
    /**
     * Format M3U direct
     * {
     *   "type": "m3u",
     *   "url": "http://serveur.com/playlist.m3u8"
     * }
     */
    data class M3uConfig(
        @SerializedName("url")
        val url: String = "",
        
        @SerializedName("name")
        val name: String = "Playlist distante",
        
        @SerializedName("createdAt")
        val createdAt: Long = System.currentTimeMillis()
    ) : RemoteConfig() {
        
        fun isValid(): Boolean {
            return url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
        }
    }
    
    companion object {
        /**
         * Détecte le type de config depuis le JSON Firebase
         */
        fun fromMap(data: Map<String, Any>?): RemoteConfig? {
            if (data == null) return null
            
            return when (val type = data["type"] as? String) {
                "xtream" -> XtreamConfig(
                    host = data["host"] as? String ?: "",
                    user = data["user"] as? String ?: "",
                    pass = data["pass"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
                "m3u" -> M3uConfig(
                    url = data["url"] as? String ?: "",
                    name = data["name"] as? String ?: "Playlist distante",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
                else -> null
            }
        }
    }
}

/**
 * État de la réception de configuration
 */
sealed class RemoteConfigState {
    object Idle : RemoteConfigState()
    object Waiting : RemoteConfigState()
    data class Received(val config: RemoteConfig) : RemoteConfigState()
    data class Applied(val playlistName: String) : RemoteConfigState()
    data class Error(val message: String) : RemoteConfigState()
    object Offline : RemoteConfigState()
}
