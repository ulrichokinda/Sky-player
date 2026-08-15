package com.skyplayer.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import timber.log.Timber

/**
 * BroadcastReceiver pour surveiller l'état du réseau
 * Déclenche des actions en cas de perte/reprise de connexion
 */
class NetworkReceiver : BroadcastReceiver() {
    
    companion object {
        var isConnected = false
            private set

        private var networkCallback: ConnectivityManager.NetworkCallback? = null

        /**
         * Enregistre un callback pour surveiller le réseau de manière moderne (API 24+)
         */
        fun registerNetworkCallback(context: Context) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Éviter les doubles enregistrements
            if (networkCallback != null) return

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (!isConnected) {
                        Timber.i("Connexion réseau rétablie (Callback)")
                        isConnected = true
                        // onConnectionRestored() - Appel direct si nécessaire
                    }
                }

                override fun onLost(network: Network) {
                    if (isConnected) {
                        Timber.i("Connexion réseau perdue (Callback)")
                        isConnected = false
                        // onConnectionLost() - Appel direct si nécessaire
                    }
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    if (hasInternet != isConnected) {
                        isConnected = hasInternet
                        if (isConnected) {
                            Timber.i("Internet validé")
                        } else {
                            Timber.i("Internet non validé")
                        }
                    }
                }
            }

            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
            } catch (e: Exception) {
                Timber.e(e, "Erreur lors de l'enregistrement du callback réseau")
            }
        }

        /**
         * Désinscrit le callback réseau
         */
        fun unregisterNetworkCallback(context: Context) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                } catch (e: Exception) {
                    Timber.w("Erreur lors de la désinscription du callback réseau")
                }
                networkCallback = null
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // CONNECTIVITY_ACTION est déprécié depuis Android 7.0 (API 24)
        // Mais conservé pour la compatibilité si enregistré via Manifest
        @Suppress("DEPRECATION")
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val currentlyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            // Détecter le changement d'état
            if (currentlyConnected && !isConnected) {
                // Connexion rétablie
                Timber.i("Connexion réseau rétablie (Broadcast)")
                onConnectionRestored()
            } else if (!currentlyConnected && isConnected) {
                // Connexion perdue
                Timber.i("Connexion réseau perdue (Broadcast)")
                onConnectionLost()
            }
            
            isConnected = currentlyConnected
        }
    }
    
    private fun onConnectionRestored() {
        // Notifier les services pour reprise de lecture
        // Cette logique peut être étendue avec un EventBus ou Flow
    }
    
    private fun onConnectionLost() {
        // Notifier les services pour mise en pause intelligente
    }
}
