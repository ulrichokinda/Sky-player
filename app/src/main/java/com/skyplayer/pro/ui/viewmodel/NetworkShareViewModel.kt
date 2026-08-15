package com.skyplayer.pro.ui.viewmodel

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.localshare.NetworkShareManager
import com.skyplayer.pro.data.localshare.ShareState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour gérer le partage de playlists en réseau local
 */
@HiltViewModel
class NetworkShareViewModel @Inject constructor(
    private val shareManager: NetworkShareManager
) : ViewModel() {

    val shareState: StateFlow<ShareState> = shareManager.shareState
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = shareManager.discoveredServices

    /**
     * Démarre le partage d'une playlist
     */
    fun startSharingPlaylist(url: String, name: String) {
        viewModelScope.launch {
            shareManager.startSharing(
                NetworkShareManager.ShareData(
                    playlistUrl = url,
                    playlistName = name
                )
            )
        }
    }

    /**
     * Arrête le partage
     */
    fun stopSharing() {
        shareManager.stopSharing()
    }

    /**
     * Démarre la découverte d'appareils
     */
    fun startDiscovery() {
        shareManager.startDiscovery()
    }

    /**
     * Arrête la découverte
     */
    fun stopDiscovery() {
        shareManager.stopDiscovery()
    }

    /**
     * Se connecte à un appareil découvert
     */
    fun connectToDevice(
        serviceInfo: NsdServiceInfo,
        onSuccess: (NetworkShareManager.ReceivedShare) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                shareManager.connectAndReceive(serviceInfo) { receivedShare ->
                    onSuccess(receivedShare)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Erreur de connexion inconnue")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        shareManager.release()
    }
}
