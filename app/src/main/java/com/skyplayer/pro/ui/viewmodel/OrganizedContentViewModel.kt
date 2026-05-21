package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.organizer.OrganizedContent
import com.skyplayer.pro.data.organizer.SmartContentOrganizer
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour gérer le contenu organisé avec tri régional et EPG
 */
@HiltViewModel
class OrganizedContentViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val contentOrganizer: SmartContentOrganizer
) : ViewModel() {

    // EPG
    private val _currentPrograms = MutableStateFlow<Map<String, EpgProgram>>(emptyMap())
    val currentPrograms: StateFlow<Map<String, EpgProgram>> = _currentPrograms.asStateFlow()

    // Live TV
    private val _liveCategories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val liveCategories: StateFlow<List<ChannelCategory>> = _liveCategories.asStateFlow()

    private val _selectedLiveCategory = MutableStateFlow<String?>(null)
    val selectedLiveCategory: StateFlow<String?> = _selectedLiveCategory.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<Channel>>(emptyList())
    val liveChannels: StateFlow<List<Channel>> = _liveChannels.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Movies
    private val _movieCategories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val movieCategories: StateFlow<List<ChannelCategory>> = _movieCategories.asStateFlow()

    private val _selectedMovieCategory = MutableStateFlow<String?>(null)
    val selectedMovieCategory: StateFlow<String?> = _selectedMovieCategory.asStateFlow()

    private val _movies = MutableStateFlow<List<Channel>>(emptyList())
    val movies: StateFlow<List<Channel>> = _movies.asStateFlow()

    // Series
    private val _seriesCategories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val seriesCategories: StateFlow<List<ChannelCategory>> = _seriesCategories.asStateFlow()

    private val _selectedSeriesCategory = MutableStateFlow<String?>(null)
    val selectedSeriesCategory: StateFlow<String?> = _selectedSeriesCategory.asStateFlow()

    private val _series = MutableStateFlow<List<Channel>>(emptyList())
    val series: StateFlow<List<Channel>> = _series.asStateFlow()

    // État global
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sidebarExpanded = MutableStateFlow(true)
    val sidebarExpanded: StateFlow<Boolean> = _sidebarExpanded.asStateFlow()

    init {
        loadAllContent()
    }

    /**
     * Charge et organise tout le contenu
     */
    fun loadAllContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Observer les chaînes depuis le repository
                channelRepository.getAllChannels().collectLatest { channels ->
                    organizeContent(channels)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur chargement contenu")
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Organise le contenu de manière asynchrone
     */
    private suspend fun organizeContent(channels: List<Channel>) {
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            val organized = contentOrganizer.organizeChannels(channels)

            // Charger l'EPG pour les chaînes Live
            loadEpgForChannels(channels.filter { it.type == ContentType.LIVE_TV })

            // Mettre à jour Live TV
            _liveCategories.value = organized.liveChannels
            if (_selectedLiveCategory.value == null && organized.liveChannels.isNotEmpty()) {
                _selectedLiveCategory.value = organized.liveChannels.firstOrNull { it.isRegionalPriority }?.name
                    ?: organized.liveChannels.first().name
            }
            updateLiveChannels()

            // Mettre à jour Movies
            _movieCategories.value = organized.movies
            if (_selectedMovieCategory.value == null && organized.movies.isNotEmpty()) {
                _selectedMovieCategory.value = organized.movies.first().name
            }
            updateMovies()

            // Mettre à jour Series
            _seriesCategories.value = organized.series
            if (_selectedSeriesCategory.value == null && organized.series.isNotEmpty()) {
                _selectedSeriesCategory.value = organized.series.first().name
            }
            updateSeries()

            val duration = System.currentTimeMillis() - startTime
            Timber.i("✅ Contenu organisé en ${duration}ms: ${organized.totalLiveChannels} live, ${organized.totalMovies} films, ${organized.totalSeries} séries")
        }
    }

    // ========== Live TV ==========
    fun selectLiveCategory(categoryName: String) {
        _selectedLiveCategory.value = categoryName
        _searchQuery.value = "" // Réinitialiser recherche lors du changement de catégorie
        updateLiveChannels()
    }

    fun searchLive(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                updateLiveChannels()
            } else {
                channelRepository.searchChannels(query).collectLatest { results ->
                    _liveChannels.value = results.filter { it.type == ContentType.LIVE_TV }
                }
            }
        }
    }

    private fun updateLiveChannels() {
        val category = _liveCategories.value.find { it.name == _selectedLiveCategory.value }
        _liveChannels.value = category?.channels ?: emptyList()
    }

    // ========== Movies ==========
    fun selectMovieCategory(categoryName: String) {
        _selectedMovieCategory.value = categoryName
        updateMovies()
    }

    private fun updateMovies() {
        val category = _movieCategories.value.find { it.name == _selectedMovieCategory.value }
        _movies.value = category?.channels ?: emptyList()
    }

    // ========== Series ==========
    fun selectSeriesCategory(categoryName: String) {
        _selectedSeriesCategory.value = categoryName
        updateSeries()
    }

    private fun updateSeries() {
        val category = _seriesCategories.value.find { it.name == _selectedSeriesCategory.value }
        _series.value = category?.channels ?: emptyList()
    }

    // ========== UI ==========
    fun toggleSidebar() {
        _sidebarExpanded.value = !_sidebarExpanded.value
    }

    fun setSidebarExpanded(expanded: Boolean) {
        _sidebarExpanded.value = expanded
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Charge l'EPG actuel pour une liste de chaînes
     */
    private fun loadEpgForChannels(channels: List<Channel>) {
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
}

/**
 * Data class pour les états UI
 */
data class ContentUiState(
    val categories: List<ChannelCategory> = emptyList(),
    val selectedCategory: String? = null,
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
