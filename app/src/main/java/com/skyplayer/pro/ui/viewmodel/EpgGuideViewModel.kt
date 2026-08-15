package com.skyplayer.pro.ui.viewmodel

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Données combinées pour un élément du guide EPG
 */
data class EpgGuideEntry(
    val channel: Channel,
    val currentProgram: EpgProgram? = null,
    val nextPrograms: List<EpgProgram> = emptyList()
)

@HiltViewModel
class EpgGuideViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<EpgGuideEntry>>(emptyList())
    val entries: StateFlow<List<EpgGuideEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshTime: StateFlow<Long> = _lastRefreshTime.asStateFlow()

    init {
        loadEpgGuide()
    }

    fun loadEpgGuide() {
        viewModelScope.launch {
            _isLoading.value = true
            channelRepository.getLiveChannels()
                .catch { e ->
                    Timber.e(e, "Erreur chargement EPG")
                    _isLoading.value = false
                }
                .collect { channels ->
                    // Extract unique categories
                    val cats = channels.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                    _categories.value = cats

                    // Filter by selected category
                    val filtered = if (_selectedCategory.value != null) {
                        channels.filter { it.category == _selectedCategory.value }
                    } else {
                        channels
                    }

                    // Load EPG programs for each channel (only those with epgId)
                    val entries = filtered.map { channel ->
                        val current = channel.epgId?.let { epgId ->
                            try { epgRepository.getCurrentProgram(epgId) } catch (e: Exception) { null }
                        }
                        val upcoming = channel.epgId?.let { epgId ->
                            try {
                                epgRepository.getUpcomingPrograms(epgId).first()
                                    .filter { prog -> !prog.isCurrent() }
                                    .take(3)
                            } catch (e: Exception) { emptyList() }
                        } ?: emptyList()

                        EpgGuideEntry(
                            channel = channel,
                            currentProgram = current,
                            nextPrograms = upcoming
                        )
                    }

                    _entries.value = entries
                    _lastRefreshTime.value = System.currentTimeMillis()
                    _isLoading.value = false
                }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        loadEpgGuide()
    }

    fun refresh() {
        loadEpgGuide()
    }
}
