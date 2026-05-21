package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel pour monitorer le chargement des chaînes
 * Fournit des statistiques en temps réel sur Live TV et VOD
 */
@HiltViewModel
class ChannelLoadViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _liveChannelCount = MutableStateFlow(0)
    val liveChannelCount: StateFlow<Int> = _liveChannelCount.asStateFlow()

    private val _vodChannelCount = MutableStateFlow(0)
    val vodChannelCount: StateFlow<Int> = _vodChannelCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastLoadTime = MutableStateFlow("")
    val lastLoadTime: StateFlow<String> = _lastLoadTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observer les chaînes en temps réel
        observeChannels()
        // Charger le nombre initial
        refreshCounts()
    }

    /**
     * Observe les changements de chaînes en temps réel
     */
    private fun observeChannels() {
        viewModelScope.launch {
            channelRepository.getLiveChannels().collectLatest { channels ->
                _liveChannelCount.value = channels.size
                updateLastLoadTime()
                Timber.d("📺 Live channels updated: ${channels.size}")
            }
        }

        viewModelScope.launch {
            channelRepository.getVodContent().collectLatest { channels ->
                _vodChannelCount.value = channels.size
                updateLastLoadTime()
                Timber.d("🎬 VOD channels updated: ${channels.size}")
            }
        }
    }

    /**
     * Rafraîchit les compteurs manuellement
     */
    fun refreshCounts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val liveCount = withContext(Dispatchers.IO) {
                    channelRepository.getChannelCount()
                }

                Timber.i("📊 Channel count refreshed: $liveCount total channels")
                updateLastLoadTime()
            } catch (e: Exception) {
                Timber.e(e, "❌ Error refreshing channel counts")
                _errorMessage.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Force le rechargement des chaînes depuis les playlists
     */
    fun refreshChannels() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                Timber.i("🔄 Manual channel refresh triggered")

                // Vérifier combien de playlists existent
                val playlistCount = withContext(Dispatchers.IO) {
                    playlistRepository.getPlaylistCount()
                }

                Timber.i("📂 Found $playlistCount playlists")

                if (playlistCount == 0) {
                    _errorMessage.value = "Aucune playlist. Ajoutez une playlist d'abord."
                    return@launch
                }

                // Les chaînes sont déjà observées via Flow
                // mais on force une vérification
                refreshCounts()

            } catch (e: Exception) {
                Timber.e(e, "❌ Error during channel refresh")
                _errorMessage.value = "Erreur rafraîchissement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Charge les chaînes VOD explicitement
     */
    fun loadVodChannels() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Observer spécifiquement les VOD
                channelRepository.getVodContent().collectLatest { vodList ->
                    _vodChannelCount.value = vodList.size
                    Timber.i("🎬 VOD loaded: ${vodList.size} films")
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Error loading VOD")
                _errorMessage.value = "Erreur VOD: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Réinitialise toutes les chaînes (pour debug)
     */
    fun clearAllChannels() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    channelRepository.deleteAllChannels()
                }
                _liveChannelCount.value = 0
                _vodChannelCount.value = 0
                Timber.w("🗑️ All channels cleared")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error clearing channels")
            }
        }
    }

    private fun updateLastLoadTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _lastLoadTime.value = sdf.format(Date())
    }
}
