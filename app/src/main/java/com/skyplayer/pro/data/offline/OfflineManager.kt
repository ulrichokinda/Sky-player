package com.skyplayer.pro.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A2: Mode offline intelligent.
 *
 * - Surveille l'état du réseau en temps réel
 * - Expose un StateFlow<Boolean> pour que l'UI affiche un badge "Hors-ligne"
 * - Permet de charger les chaînes depuis le cache Room même sans réseau
 * - Distingue "pas de réseau" de "réseau lent" (pour le retry automatique)
 */
@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class NetworkState {
        CONNECTED,       // Réseau OK
        SLOW,            // Réseau lent (WiFi faible, 2G)
        DISCONNECTED,    // Pas de réseau
        METERED          // Réseau à données limitées (roaming)
    }

    private val _networkState = MutableStateFlow(NetworkState.CONNECTED)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _networkState.value = NetworkState.CONNECTED
            Timber.i("🌐 Network: CONNECTED")
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _networkState.value = NetworkState.DISCONNECTED
            Timber.w("🔴 Network: DISCONNECTED")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val isLowBandwidth = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) &&
                    capabilities.linkDownstreamBandwidthKbps < 1000

            _networkState.value = when {
                isLowBandwidth -> NetworkState.SLOW
                isMetered && isWifi -> NetworkState.METERED
                else -> NetworkState.CONNECTED
            }

            Timber.d("🌐 Network capabilities: wifi=$isWifi, metered=$isMetered, low=$isLowBandwidth")
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        Timber.i("📡 OfflineManager: network monitoring started")
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Timber.w("⚠️ OfflineManager: already unregistered")
        }
    }

    /**
     * Vérifie si le réseau est suffisant pour le streaming.
     */
    fun isNetworkSufficient(): Boolean {
        return _networkState.value == NetworkState.CONNECTED
    }

    /**
     * Vérifie si l'app peut tenter un refresh (évite de spammer en offline).
     */
    fun canRefresh(): Boolean {
        return _networkState.value != NetworkState.DISCONNECTED
    }

    /**
     * Retourne un message adapté à l'état du réseau pour l'UI.
     */
    fun getOfflineMessage(): String {
        return when (_networkState.value) {
            NetworkState.CONNECTED -> ""
            NetworkState.SLOW -> "Réseau lent — certaines chaînes peuvent mettre du temps à charger"
            NetworkState.METERED -> "Réseau à données limitées — qualité réduite automatiquement"
            NetworkState.DISCONNECTED -> "Hors-ligne — affichage des chaînes en cache"
        }
    }
}
