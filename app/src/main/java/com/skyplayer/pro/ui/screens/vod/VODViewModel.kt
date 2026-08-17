package com.skyplayer.pro.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.organizer.SmartContentOrganizer
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @OptIn(FlowPreview::class)
    private fun loadAllMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            // Debounce : un refresh par lots invalide Room à chaque insert → on ne
            // réorganise que sur l'état stable, pas à chaque batch.
            channelRepository.getVodContent()
                .catch { e ->
                    Timber.e(e, "Erreur chargement films VOD")
                    _isLoading.value = false
                }
                .debounce(200)
                .collectLatest { allMovies ->
                    // Organisation lourde en CPU (nettoyage catégories + tris) → hors thread principal
                    val organized = withContext(Dispatchers.Default) {
                        contentOrganizer.organizeChannels(allMovies)
                    }
                    _categories.value = organized.movies
                    // Défaut = TOUT : toutes les catégories groupées dans la grille,
                    // le scroll traverse les sections et le sidebar suit.
                    _selectedCategory.value = null
                    updateCurrentMovies()
                    _isLoading.value = false
                }
        }
    }

    fun selectCategory(categoryName: String) {
        // "ALL" = mode TOUT (toutes les catégories groupées)
        _selectedCategory.value = if (categoryName == "ALL") null else categoryName
        updateCurrentMovies()
    }

    private fun updateCurrentMovies() {
        val category = _categories.value.find { it.name == _selectedCategory.value }
        // Mode TOUT : toutes les catégories, déjà groupées dans l'ordre des catégories
        _movies.value = category?.channels ?: _categories.value.flatMap { it.channels }
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
