package com.skyplayer.pro.ui.viewmodel

import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.prefetch.PrefetchStats
import com.skyplayer.pro.data.prefetch.StreamPrefetchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel de lecture avec pré-chargement pour zapping instantané
 *
 * Intègre StreamPrefetchManager pour :
 * - Zapping < 500ms sur 3G/4G
 * - Buffering silencieux des voisins
 * - Transition fluide entre chaînes
 */
@UnstableApi
@HiltViewModel
class PrefetchPlayerViewModel @Inject constructor(
    private val prefetchManager: StreamPrefetchManager
) : ViewModel() {

    // État du player actuel
    private val _currentPlayer = MutableStateFlow<ExoPlayer?>(null)
    val currentPlayer: StateFlow<ExoPlayer?> = _currentPlayer

    // Canal actuellement lu
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel

    // Statistiques de pré-chargement
    private val _prefetchStats = MutableStateFlow<PrefetchStats?>(null)
    val prefetchStats: StateFlow<PrefetchStats?> = _prefetchStats

    // Historique des temps de zapping pour métriques
    private val _zappingTimes = MutableStateFlow<List<Long>>(emptyList())
    val zappingTimes: StateFlow<List<Long>> = _zappingTimes

    // Liste de tous les canaux (pour navigation)
    private var allChannels: List<Channel> = emptyList()

    /**
     * Définit la liste complète des canaux disponibles
     */
    fun setChannelList(channels: List<Channel>) {
        allChannels = channels
        Timber.i("📺 Liste de ${channels.size} canaux définie pour pré-chargement")
    }

    /**
     * Change de canal avec pré-chargement optimisé
     * Objectif : < 500ms même sur connexion lente
     */
    fun switchToChannel(channel: Channel) {
        val startTime = System.currentTimeMillis()
        val previousChannelId = _currentChannel.value?.id

        viewModelScope.launch {
            // 1. Vérifier si le canal est déjà pré-chargé
            val prefetchedPlayer = prefetchManager.getPrefetchedPlayer(channel.id)

            if (prefetchedPlayer != null) {
                // 🚀 ZAPPING INSTANTANÉ (cache hit)
                Timber.i("⚡ Zapping instantané: ${channel.name} (pré-chargé)")

                // Libérer l'ancien player
                previousChannelId?.let { prefetchManager.releasePlayer(it) }

                // Utiliser le player pré-chargé
                _currentPlayer.value = prefetchedPlayer
                _currentChannel.value = channel

                // Mettre à jour le pré-chargement pour les nouveaux voisins
                prefetchManager.updateCurrentPosition(channel, allChannels)

            } else {
                // ⏳ Zapping standard (cache miss)
                Timber.w("⏳ Zapping standard: ${channel.name} (non pré-chargé)")

                // Libérer l'ancien player
                previousChannelId?.let { prefetchManager.releasePlayer(it) }

                // Créer nouveau player (prendra plus de temps)
                // Note: Dans une implémentation complète, on utiliserait
                // un PlayerRepository pour créer le player
                _currentChannel.value = channel

                // Lancer le pré-chargement pour ce canal et ses voisins
                prefetchManager.updateCurrentPosition(channel, allChannels)
            }

            // Calculer le temps de zapping
            val zappingTime = System.currentTimeMillis() - startTime
            _zappingTimes.value = _zappingTimes.value + zappingTime

            Timber.d("⏱️ Temps de zapping: ${zappingTime}ms (moyenne: ${getAverageZappingTime()}ms)")
        }
    }

    /**
     * Navigue vers le canal suivant
     */
    fun nextChannel() {
        val current = _currentChannel.value ?: return
        val currentIndex = allChannels.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return

        val nextIndex = if (currentIndex < allChannels.size - 1) currentIndex + 1 else 0
        switchToChannel(allChannels[nextIndex])
    }

    /**
     * Navigue vers le canal précédent
     */
    fun previousChannel() {
        val current = _currentChannel.value ?: return
        val currentIndex = allChannels.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else allChannels.size - 1
        switchToChannel(allChannels[prevIndex])
    }

    /**
     * Met à jour la position pour pré-chargement (appelé pendant navigation)
     */
    fun updatePrefetchPosition(channel: Channel) {
        if (allChannels.isEmpty()) return

        prefetchManager.updateCurrentPosition(channel, allChannels)

        // Mettre à jour les stats pour debug
        _prefetchStats.value = prefetchManager.getStats()
    }

    /**
     * Récupère le temps moyen de zapping
     */
    fun getAverageZappingTime(): Long {
        val times = _zappingTimes.value
        return if (times.isNotEmpty()) times.average().toLong() else 0L
    }

    /**
     * Vérifie si un canal est prêt (pré-chargé)
     */
    fun isChannelReady(channelId: String): Boolean {
        return prefetchManager.isChannelReady(channelId)
    }

    /**
     * Libère toutes les ressources
     */
    override fun onCleared() {
        super.onCleared()
        prefetchManager.releaseAll()
        Timber.i("🧹 PrefetchPlayerViewModel nettoyé")
    }
}
