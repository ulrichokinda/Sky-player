package com.skyplayer.pro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modèle représentant une configuration en attente pour Android TV/Box
 * Stocké temporairement dans Firebase pending_configs/{macId}
 */
data class PendingConfig(
    @SerializedName("host")
    val host: String = "",
    
    @SerializedName("username")
    val username: String = "",
    
    @SerializedName("password")
    val password: String = "",
    
    @SerializedName("playlistName")
    val playlistName: String = "Ma Playlist",
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @SerializedName("configured")
    val configured: Boolean = false
) {
    /**
     * Vérifie si la configuration est valide (tous les champs requis présents)
     */
    fun isValid(): Boolean {
        return host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }
    
    /**
     * Génère l'URL de connexion au serveur
     */
    fun getServerUrl(): String {
        return host.trim()
    }
    
    /**
     * Génère l'URL de la playlist au format standard
     */
    fun getPlaylistUrl(): String {
        val baseUrl = host.trim().trimEnd('/')
        return "$baseUrl/get.php?username=$username&password=$password&type=m3u_plus&output=ts"
    }
}

/**
 * État de la configuration TV
 */
sealed class TvConfigState {
    object Idle : TvConfigState()
    object Loading : TvConfigState()
    data class WaitingForScan(val qrCodeUrl: String, val macId: String) : TvConfigState()
    data class ConfigReceived(val config: PendingConfig) : TvConfigState()
    data class Success(val playlistName: String) : TvConfigState()
    data class Error(val message: String) : TvConfigState()
}
