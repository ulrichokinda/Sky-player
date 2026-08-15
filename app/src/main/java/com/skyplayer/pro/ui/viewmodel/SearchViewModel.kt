package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/**
 * Filtre de type de contenu pour la recherche
 */
enum class SearchFilter(val label: String) {
    ALL("Tout"),
    LIVE("Live TV"),
    VOD("Films"),
    SERIES("Séries")
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Channel>> = _query
        .debounce(280)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                // Lorsque la recherche est vide, afficher les chaînes récemment regardées
                channelRepository.getRecentlyWatched(limit = 30)
            } else {
                channelRepository.searchChannels(q)
            }
        }
        .combine(_filter) { channels, filter ->
            when (filter) {
                SearchFilter.ALL -> channels
                SearchFilter.LIVE -> channels.filter {
                    it.type == ContentType.LIVE_TV ||
                    it.type == ContentType.LIVE_SPORTS ||
                    it.type == ContentType.LIVE_NEWS
                }
                SearchFilter.VOD -> channels.filter { it.type == ContentType.VOD_MOVIE }
                SearchFilter.SERIES -> channels.filter { it.type == ContentType.VOD_SERIES }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isShowingRecents: StateFlow<Boolean> = _query
        .map { it.isBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /**
     * Traite le résultat de la recherche vocale
     */
    fun onVoiceResult(text: String) {
        Timber.i("🎤 Recherche vocale : $text")
        _query.value = text
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _filter.value = newFilter
    }

    fun clearQuery() {
        _query.value = ""
    }
}
