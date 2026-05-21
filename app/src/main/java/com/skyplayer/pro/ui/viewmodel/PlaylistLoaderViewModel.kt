package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.repository.RefreshState
import com.skyplayer.pro.data.repository.SmartPlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour le chargement intelligent des playlists
 * 
 * Démonstrateur du pattern "Load from Local First, Refresh in Background"
 */
@HiltViewModel
class PlaylistLoaderViewModel @Inject constructor(
    private val smartRepository: SmartPlaylistRepository
) : ViewModel() {

    // === ÉTAT UI ===
    
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _lastUpdateTime = MutableStateFlow<Long?>(null)
    val lastUpdateTime: StateFlow<Long?> = _lastUpdateTime.asStateFlow()
    
    // Statistiques pour debug
    private val _stats = MutableStateFlow(LoadStats())
    val stats: StateFlow<LoadStats> = _stats.asStateFlow()

    /**
     * Charge une playlist avec la stratégie intelligente :
     * 1. Affichage immédiat depuis la base locale
     * 2. Rafraîchissement silencieux en arrière-plan
     */
    fun loadPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            _stats.value = LoadStats()
            
            val startTime = System.currentTimeMillis()
            var firstContentShown = false
            
            smartRepository.loadChannelsSmart(playlist)
                .collect { channels ->
                    val elapsed = System.currentTimeMillis() - startTime
                    
                    // Premier affichage (données locales)
                    if (!firstContentShown) {
                        firstContentShown = true
                        _stats.value = _stats.value.copy(
                            localLoadTimeMs = elapsed,
                            localChannelCount = channels.size
                        )
                        Timber.i("⚡ Premier affichage en ${elapsed}ms: ${channels.size} chaînes")
                    } 
                    // Mise à jour après rafraîchissement
                    else {
                        _stats.value = _stats.value.copy(
                            totalRefreshTimeMs = elapsed,
                            refreshedChannelCount = channels.size
                        )
                        Timber.i("🔄 Mise à jour après rafraîchissement: ${channels.size} chaînes")
                    }
                    
                    _channels.value = channels
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Force un rafraîchissement manuel (swipe-to-refresh)
     */
    fun forceRefresh(playlist: Playlist) {
        viewModelScope.launch {
            try {
                smartRepository.forceRefresh(playlist)
                _lastUpdateTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur rafraîchissement manuel")
            }
        }
    }
    
    /**
     * Précharge une playlist (premier lancement de l'app)
     */
    fun preloadPlaylist(playlist: Playlist, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = smartRepository.preloadPlaylist(playlist)
            
            result.onSuccess { count ->
                Timber.i("✅ Préchargement réussi: $count chaînes")
                _lastUpdateTime.value = System.currentTimeMillis()
                onComplete(count)
            }.onFailure { error ->
                Timber.e(error, "❌ Échec préchargement")
            }
            
            _isLoading.value = false
        }
    }
    
    init {
        // Observer l'état de rafraîchissement
        viewModelScope.launch {
            smartRepository.refreshState.collect { state ->
                _refreshState.value = state
            }
        }
        
        viewModelScope.launch {
            smartRepository.isRefreshing.collect { refreshing ->
                _isRefreshing.value = refreshing
            }
        }
    }
}

/**
 * Statistiques de chargement pour monitoring
 */
data class LoadStats(
    val localLoadTimeMs: Long = 0,
    val localChannelCount: Int = 0,
    val totalRefreshTimeMs: Long = 0,
    val refreshedChannelCount: Int = 0
) {
    val speedImprovement: String
        get() = if (totalRefreshTimeMs > 0) {
            "${(totalRefreshTimeMs / localLoadTimeMs)}x plus rapide"
        } else "N/A"
}
