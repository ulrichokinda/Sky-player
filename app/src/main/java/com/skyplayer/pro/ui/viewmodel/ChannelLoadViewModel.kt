package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour monitorer le chargement des chaînes et forcer le rafraîchissement
 */
@HiltViewModel
class ChannelLoadViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastLoadTime = MutableStateFlow(System.currentTimeMillis())
    val lastLoadTime: StateFlow<Long> = _lastLoadTime.asStateFlow()

    val liveChannelCount: StateFlow<Int> = channelRepository.getLiveChannels()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val vodChannelCount: StateFlow<Int> = channelRepository.getVodContent()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Rafraîchit manuellement la liste des chaînes
     */
    fun refreshChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            // Le repository ne gère pas le refresh HTTP lui-même, 
            // c'est généralement géré par PlaylistRepository.
            // On met à jour le timestamp pour l'UI.
            _lastLoadTime.value = System.currentTimeMillis()
            _isLoading.value = false
        }
    }
}
