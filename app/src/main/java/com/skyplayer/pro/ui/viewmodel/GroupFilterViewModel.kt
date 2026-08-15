package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.repository.ChannelGroupRepository
import com.skyplayer.pro.data.repository.GroupInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour gérer le filtrage par groupes et catégories sur TV
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupFilterViewModel @Inject constructor(
    private val repository: ChannelGroupRepository,
    private val channelRepository: com.skyplayer.pro.data.repository.ChannelRepository
) : ViewModel() {

    private val _contentType = MutableStateFlow(ContentType.LIVE_TV)
    val contentType: StateFlow<ContentType> = _contentType.asStateFlow()
    
    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Historique de lecture
     */
    val watchHistory: StateFlow<List<Channel>> = channelRepository.getRecentlyWatched()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Liste des groupes pour le type de contenu actuel
     */
    val groups: StateFlow<List<GroupInfo>> = _contentType
        .flatMapLatest { type -> repository.getAllGroups(type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Récupère les chaînes pour un groupe spécifique (pour les carrousels)
     */
    fun getChannelsByGroup(groupName: String): Flow<List<Channel>> {
        return repository.getChannelsByGroup(_contentType.value, groupName)
    }

    /**
     * Liste des chaînes filtrées par le groupe sélectionné
     */
    val channels: StateFlow<List<Channel>> = combine(_contentType, _selectedGroup) { type, group ->
        type to group
    }.flatMapLatest { (type, group) ->
        if (group == null) {
            flowOf(emptyList<Channel>())
        } else {
            repository.getChannelsByGroup(type, group)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Change le type de contenu (Live TV, VOD, etc.)
     */
    fun setContentType(type: ContentType) {
        _contentType.value = type
        _selectedGroup.value = null // Reset selection
    }

    /**
     * Charge les groupes au démarrage
     */
    fun loadGroups(type: ContentType) {
        _contentType.value = type
    }

    /**
     * Sélectionne un groupe
     */
    fun selectGroup(groupName: String) {
        _selectedGroup.value = groupName
    }
}
