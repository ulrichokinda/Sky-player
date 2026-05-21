package com.skyplayer.pro.ui.viewmodel

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.localshare.NetworkShareManager
import com.skyplayer.pro.data.localshare.ShareState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour le partage réseau local (Wi-Fi Direct / NSD)
 *
 * Gère :
 * - Démarrage/arrêt du service de partage
 * - Découverte des appareils disponibles
 * - Connexion et transfert des données
 */
@HiltViewModel
class NetworkShareViewModel @Inject constructor(
    private val networkShareManager: NetworkShareManager
) : ViewModel() {

    val shareState: StateFlow<ShareState> = networkShareManager.shareState
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = networkShareManager.discoveredServices

    /**
     * Démarre le partage d'une playlist
     */
    fun startSharingPlaylist(playlistUrl: String, playlistName: String) {
        val shareData = NetworkShareManager.ShareData(
            playlistUrl = playlistUrl,
            playlistName = playlistName,
            credentials = emptyMap() // À compléter selon besoin
        )

        viewModelScope.launch {
            networkShareManager.startSharing(shareData)
            Timber.i("📡 Partage démarré pour: $playlistName")
        }
    }

    /**
     * Arrête le service de partage
     */
    fun stopSharing() {
        networkShareManager.stopSharing()
        Timber.i("🛑 Partage arrêté")
    }

    /**
     * Démarre la découverte des appareils sur le réseau
     */
    fun startDiscovery() {
        viewModelScope.launch {
            networkShareManager.startDiscovery()
            Timber.i("🔍 Recherche d'appareils sur le réseau local...")
        }
    }

    /**
     * Arrête la découverte
     */
    fun stopDiscovery() {
        networkShareManager.stopDiscovery()
    }

    /**
     * Connecte à un appareil et récupère les données partagées
     */
    fun connectToDevice(
        serviceInfo: NsdServiceInfo,
        onSuccess: (NetworkShareManager.ReceivedShare) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                networkShareManager.connectAndReceive(serviceInfo) { receivedShare ->
                    onSuccess(receivedShare)
                    Timber.i("✅ Playlist reçue: ${receivedShare.shareData.playlistName}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Erreur de connexion")
                Timber.e(e, "❌ Erreur connexion à l'appareil")
            }
        }
    }

    /**
     * Vérifie si le Wi-Fi est activé
     */
    fun isWifiEnabled(): Boolean {
        // Vérification simplifiée - à implémenter avec WifiManager
        return true
    }

    override fun onCleared() {
        super.onCleared()
        networkShareManager.release()
        Timber.i("🧹 NetworkShareViewModel nettoyé")
    }
}
