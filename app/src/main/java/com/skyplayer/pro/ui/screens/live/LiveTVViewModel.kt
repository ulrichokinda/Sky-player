package com.skyplayer.pro.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'écran Live TV avec support EPG
 */
@HiltViewModel
class LiveTVViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    // Cache pour les programmes actuels par epgId
    private val _currentPrograms = MutableStateFlow<Map<String, EpgProgram>>(emptyMap())
    val currentPrograms: StateFlow<Map<String, EpgProgram>> = _currentPrograms.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            
            channelRepository.getLiveChannels()
                .catch { e ->
                    Timber.e(e, "Erreur chargement chaînes Live")
                    _isLoading.value = false
                }
                .collect { channels ->
                    _channels.value = channels
                    _isLoading.value = false
                    loadEpgForVisibleChannels(channels)
                }
        }
    }

    /**
     * Charge les programmes EPG actuels pour les chaînes listées
     */
    private fun loadEpgForVisibleChannels(channels: List<Channel>) {
        viewModelScope.launch {
            val programsMap = mutableMapOf<String, EpgProgram>()
            channels.forEach { channel ->
                channel.epgId?.let { epgId ->
                    epgRepository.getCurrentProgram(epgId)?.let { program ->
                        programsMap[epgId] = program
                    }
                }
            }
            _currentPrograms.value = programsMap
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            try {
                channelRepository.toggleFavorite(channel.id, channel.isFavorite)
            } catch (e: Exception) {
                Timber.e(e, "Erreur toggle favori: ${channel.id}")
            }
        }
    }

    fun refresh() {
        loadChannels()
    }
}
