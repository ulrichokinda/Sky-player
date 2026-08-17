package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.organizer.SeriesItem
import com.skyplayer.pro.data.organizer.SmartContentOrganizer
import com.skyplayer.pro.data.repository.ChannelRepository
import com.skyplayer.pro.data.repository.EpgRepository
import com.skyplayer.pro.data.remote.XtreamCodesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OrganizedContentViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val contentOrganizer: SmartContentOrganizer
) : ViewModel() {

    private val _liveCategories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val liveCategories: StateFlow<List<ChannelCategory>> = _liveCategories.asStateFlow()

    private val _selectedLiveCategory = MutableStateFlow<String?>(null)
    val selectedLiveCategory: StateFlow<String?> = _selectedLiveCategory.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<Channel>>(emptyList())
    val liveChannels: StateFlow<List<Channel>> = _liveChannels.asStateFlow()

    // Onglets séparés pour VOD
    private val _movies = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val movies: StateFlow<List<ChannelCategory>> = _movies.asStateFlow()

    private val _series = MutableStateFlow<List<SeriesItem>>(emptyList())
    val series: StateFlow<List<SeriesItem>> = _series.asStateFlow()

    private val _currentPrograms = MutableStateFlow<Map<String, EpgProgram>>(emptyMap())
    val currentPrograms: StateFlow<Map<String, EpgProgram>> = _currentPrograms.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Détails VOD sélectionnés (Movie ou Series)
    private val _selectedVodDetails = MutableStateFlow<Any?>(null)
    val selectedVodDetails: StateFlow<Any?> = _selectedVodDetails.asStateFlow()

    private var allChannels: List<Channel> = emptyList()
    private var currentSearchQuery: String = ""

    init {
        loadAllChannels()
    }

    @OptIn(FlowPreview::class)
    private fun loadAllChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            // On récupère toutes les chaînes pour les organiser en onglets séparés.
            // Debounce : un refresh par lots (SmartPlaylistRepository) invalide Room à
            // chaque insert → on ne réorganise que sur l'état stable, pas à chaque batch.
            channelRepository.getAllChannels()
                .catch { e ->
                    Timber.e(e, "Erreur chargement contenu")
                    _isLoading.value = false
                }
                .debounce(200)
                .collect { channels ->
                    allChannels = channels

                    // Organisation des 3 listes : lourde en CPU (regex de
                    // classification + tris) → hors du thread principal.
                    val organized = withContext(Dispatchers.Default) {
                        contentOrganizer.organizeChannels(channels)
                    }

                    _liveCategories.value = organized.liveChannels
                    _movies.value = organized.movies
                    _series.value = organized.series

                    // Défaut = TOUT : toutes les catégories sont affichées dans une
                    // seule liste triée, le scroll traverse les sections et le sidebar suit.
                    _selectedLiveCategory.value = null

                    applyFilters()
                    _isLoading.value = false
                }
        }
    }

    fun selectLiveCategory(categoryName: String) {
        _selectedLiveCategory.value = if (categoryName == "ALL") null else categoryName
        applyFilters()
    }

    fun searchLive(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    fun refresh() {
        loadAllChannels()
    }

    /**
     * Récupère les détails d'un film ou d'une série de manière asynchrone (Lazy)
     * Stocke le résultat dans selectedVodDetails.
     */
    fun fetchVodDetails(channel: Channel, xtreamApi: XtreamCodesApi, credentials: Triple<String, String, String>) {
        viewModelScope.launch {
            try {
                _selectedVodDetails.value = null // Reset previous

                // Extraire l'ID Xtream (le dernier segment après l'underscore)
                val id = channel.id.substringAfterLast("_").toIntOrNull() ?: return@launch

                if (channel.type == ContentType.VOD_MOVIE) {
                    val response = xtreamApi.getVodDetails(
                        fullUrl = credentials.first,
                        username = credentials.second,
                        password = credentials.third,
                        action = "get_vod_info",
                        vodId = id
                    )
                    if (response.isSuccessful) {
                        _selectedVodDetails.value = response.body()
                    }
                } else if (channel.type == ContentType.VOD_SERIES || channel.type == ContentType.SERIES_EPISODE) {
                    val response = xtreamApi.getSeriesDetails(
                        fullUrl = credentials.first,
                        username = credentials.second,
                        password = credentials.third,
                        seriesId = id
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            _selectedVodDetails.value = XtreamCodesApi.parseSeriesDetailsStream(body)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Erreur fetch métadonnées VOD pour ${channel.name}")
            }
        }
    }

    private fun applyFilters() {
        val categoryFilter = _selectedLiveCategory.value
        val query = currentSearchQuery
        viewModelScope.launch {
            // Filtre + tri sur la liste complète (potentiellement des dizaines de
            // milliers de chaînes) → hors du thread principal.
            val filteredLive = withContext(Dispatchers.Default) {
                allChannels
                    .filter { it.type in ChannelRepository.LIVE_CONTENT_TYPES }
                    .filter { channel ->
                        categoryFilter == null || channel.category == categoryFilter || channel.groupTitle == categoryFilter
                    }
                    .filter { channel ->
                        query.isBlank() ||
                            channel.name.contains(query, ignoreCase = true) ||
                            channel.category.contains(query, ignoreCase = true)
                    }
                    .sortedWith(compareBy<Channel> { it.category }.thenBy { it.name })
            }

            _liveChannels.value = filteredLive
            loadEpgForVisibleChannels(filteredLive)
        }
    }

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
}
