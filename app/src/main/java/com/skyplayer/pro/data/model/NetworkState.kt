package com.skyplayer.pro.data.model

/**
 * État du réseau pour monitoring de la connexion
 */
sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
    object Lost : NetworkState()
    data class WeakSignal(val strength: Int) : NetworkState() // 0-100
}

/**
 * Résultat de l'opération avec gestion d'état
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * États de connexion du lecteur
 */
sealed class PlayerConnectionState {
    object Idle : PlayerConnectionState()
    object Connecting : PlayerConnectionState()
    object Buffering : PlayerConnectionState()
    object Ready : PlayerConnectionState()
    data class Error(val error: Throwable, val retryCount: Int = 0) : PlayerConnectionState()
    object Reconnecting : PlayerConnectionState()
}
