package com.skyplayer.pro.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.organizer.SeriesItem
import com.skyplayer.pro.data.organizer.SmartContentOrganizer
import com.skyplayer.pro.data.repository.ChannelRepository
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
 * ViewModel pour l'écran Séries
 */
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val contentOrganizer: SmartContentOrganizer
) : ViewModel() {

    private val _series = MutableStateFlow<List<Channel>>(emptyList())
    val series: StateFlow<List<Channel>> = _series.asStateFlow()

    private val _categories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val categories: StateFlow<List<ChannelCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllSeries()
    }

    private fun loadAllSeries() {
        viewModelScope.launch {
            _isLoading.value = true
            channelRepository.getSeries()
                .catch { e ->
                    Timber.e(e, "Erreur chargement séries")
                    _isLoading.value = false
                }
                .collectLatest { allSeries ->
                    val organized = contentOrganizer.organizeChannels(allSeries)
                    
                    // Convertir SeriesItem en ChannelCategory pour la Sidebar
                    val seriesCategories = organized.series
                        .groupBy { it.category }
                        .map { (catName, items) ->
                            ChannelCategory(
                                name = catName,
                                channels = items.map { seriesItem ->
                                    val firstEp = seriesItem.seasons.firstOrNull()?.episodes?.firstOrNull()
                                    Channel(
                                        id = "series_${seriesItem.title}",
                                        name = seriesItem.title,
                                        url = firstEp?.url ?: "",
                                        logoUrl = seriesItem.coverUrl,
                                        category = seriesItem.category,
                                        type = ContentType.VOD_SERIES,
                                        groupTitle = seriesItem.category
                                    )
                                },
                                type = ContentType.VOD_SERIES
                            )
                        }
                        .sortedBy { it.name }

                    _categories.value = seriesCategories

                    // Défaut = TOUT : toutes les catégories groupées dans la grille,
                    // le scroll traverse les sections et le sidebar suit.
                    _selectedCategory.value = null
                    updateCurrentSeries()
                    _isLoading.value = false
                }
        }
    }

    fun selectCategory(categoryName: String) {
        // "ALL" = mode TOUT (toutes les catégories groupées)
        _selectedCategory.value = if (categoryName == "ALL") null else categoryName
        updateCurrentSeries()
    }

    private fun updateCurrentSeries() {
        val category = _categories.value.find { it.name == _selectedCategory.value }
        // Mode TOUT : toutes les catégories, déjà groupées dans l'ordre des catégories
        _series.value = category?.channels ?: _categories.value.flatMap { it.channels }
    }

    fun searchSeries(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                updateCurrentSeries()
            } else {
                channelRepository.searchChannels(query).collectLatest { results ->
                    _series.value = results.filter { it.type == ContentType.VOD_SERIES }
                }
            }
        }
    }

    fun refresh() {
        loadAllSeries()
    }
}
