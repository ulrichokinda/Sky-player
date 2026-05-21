package com.skyplayer.pro.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.organizer.ChannelCategory
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
 * ViewModel pour l'écran VOD
 */
@HiltViewModel
class VODViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val contentOrganizer: SmartContentOrganizer
) : ViewModel() {

    private val _movies = MutableStateFlow<List<Channel>>(emptyList())
    val movies: StateFlow<List<Channel>> = _movies.asStateFlow()

    private val _categories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val categories: StateFlow<List<ChannelCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllMovies()
    }

    private fun loadAllMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            channelRepository.getVodContent()
                .catch { e ->
                    Timber.e(e, "Erreur chargement films VOD")
                    _isLoading.value = false
                }
                .collectLatest { allMovies ->
                    val organized = contentOrganizer.organizeChannels(allMovies)
                    _categories.value = organized.movies
                    if (_selectedCategory.value == null && organized.movies.isNotEmpty()) {
                        _selectedCategory.value = organized.movies.first().name
                    }
                    updateCurrentMovies()
                    _isLoading.value = false
                }
        }
    }

    fun selectCategory(categoryName: String) {
        _selectedCategory.value = categoryName
        updateCurrentMovies()
    }

    private fun updateCurrentMovies() {
        val category = _categories.value.find { it.name == _selectedCategory.value }
        _movies.value = category?.channels ?: emptyList()
    }

    fun searchMovies(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                updateCurrentMovies()
            } else {
                channelRepository.searchChannels(query).collectLatest { results ->
                    _movies.value = results.filter { it.type == ContentType.VOD_MOVIE }
                }
            }
        }
    }

    fun refresh() {
        loadAllMovies()
    }
}
