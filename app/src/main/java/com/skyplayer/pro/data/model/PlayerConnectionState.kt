package com.skyplayer.pro.data.model

/**
 * Etats de connexion du player au flux streaming.
 */
sealed class PlayerConnectionState {
    object Idle : PlayerConnectionState()
    object Connecting : PlayerConnectionState()
    object Buffering : PlayerConnectionState()
    object Ready : PlayerConnectionState()
    object Reconnecting : PlayerConnectionState()
    data class Error(val exception: Throwable, val retryCount: Int = 0) : PlayerConnectionState()
}
