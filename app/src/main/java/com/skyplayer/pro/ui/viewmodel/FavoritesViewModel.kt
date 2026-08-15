package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel unifié pour les favoris — source unique via [ChannelRepository].
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val favorites: StateFlow<List<Channel>> = channelRepository.getFavorites()
        .onEach { _isLoading.value = false }
        .catch { e ->
            Timber.e(e, "Erreur chargement favoris")
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            try {
                channelRepository.toggleFavorite(channel.id, channel.isFavorite)
            } catch (e: Exception) {
                Timber.e(e, "Erreur toggle favori: ${channel.id}")
            }
        }
    }

    fun removeFromFavorites(channel: Channel) {
        toggleFavorite(channel.copy(isFavorite = true))
    }
}
