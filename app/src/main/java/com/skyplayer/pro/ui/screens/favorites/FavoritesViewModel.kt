package com.skyplayer.pro.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'écran Favoris
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Channel>>(emptyList())
    val favorites: StateFlow<List<Channel>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            
            channelRepository.getFavorites()
                .catch { e ->
                    Timber.e(e, "Erreur chargement favoris")
                    _isLoading.value = false
                }
                .collect { favorites ->
                    _favorites.value = favorites
                    _isLoading.value = false
                }
        }
    }

    fun removeFromFavorites(channel: Channel) {
        viewModelScope.launch {
            try {
                channelRepository.toggleFavorite(channel.id, true) // isFavorite = true, donc toggle va le retirer
            } catch (e: Exception) {
                Timber.e(e, "Erreur suppression favori: ${channel.id}")
            }
        }
    }

    fun refresh() {
        loadFavorites()
    }
}
