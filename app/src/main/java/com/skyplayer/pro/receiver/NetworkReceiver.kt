package com.skyplayer.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val currentlyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            // Détecter le changement d'état
            if (currentlyConnected && !isConnected) {
                // Connexion rétablie
                Timber.i("Connexion réseau rétablie")
                onConnectionRestored()
            } else if (!currentlyConnected && isConnected) {
                // Connexion perdue
                Timber.i("Connexion réseau perdue")
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
