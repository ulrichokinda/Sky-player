package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.repository.ChannelGroupRepository
import com.skyplayer.pro.data.repository.GroupCategory
import com.skyplayer.pro.data.repository.GroupInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour le filtrage par groupe et recherche
 * 
 * Démonstrateur des fonctionnalités :
 * - Liste des groupes avec compteur
 * - Filtrage par groupe
 * - Recherche intelligente (sans accents, insensible à la casse)
 * - Combinaison groupe + recherche
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class GroupFilterViewModel @Inject constructor(
    private val groupRepository: ChannelGroupRepository
) : ViewModel() {

    // === ÉTAT UI ===
    
    private val _groups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val groups: StateFlow<List<GroupInfo>> = _groups.asStateFlow()
    
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    
    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _contentType = MutableStateFlow(ContentType.LIVE_TV)
    val contentType: StateFlow<ContentType> = _contentType.asStateFlow()

    // Recherche avec debounce pour performance
    private val _searchQueryDebounced = _searchQuery
        .debounce(300) // 300ms de délai pour éviter les recherches trop fréquentes
        .distinctUntilChanged()

    init {
        // Observer les changements de groupe et recherche
        viewModelScope.launch {
            combine(
                _selectedGroup,
                _searchQueryDebounced,
                _contentType
            ) { group, query, type ->
                Triple(group, query, type)
            }.collect { (group, query, type) ->
                loadChannels(group, query, type)
            }
        }
    }

    /**
     * Charge la liste des groupes disponibles
     */
    fun loadGroups(contentType: ContentType = ContentType.LIVE_TV) {
        _contentType.value = contentType
        
        viewModelScope.launch {
            _isLoading.value = true
            
            groupRepository.getAllGroups(contentType)
                .collect { groupsList ->
                    _groups.value = groupsList
                    _isLoading.value = false
                    Timber.d("📁 ${groupsList.size} groupes chargés")
                }
        }
    }

    /**
     * Sélectionne un groupe pour filtrage
     */
    fun selectGroup(groupName: String?) {
        _selectedGroup.value = groupName
        Timber.d("📂 Groupe sélectionné: $groupName")
    }

    /**
     * Met à jour la requête de recherche
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Change le type de contenu (Live TV, VOD, etc.)
     */
    fun setContentType(type: ContentType) {
        _contentType.value = type
        loadGroups(type)
    }

    /**
     * Charge les chaînes selon les filtres actifs
     */
    private suspend fun loadChannels(
        group: String?,
        query: String,
        type: ContentType
    ) {
        _isLoading.value = true
        
        val flow = when {
            // Groupe + recherche
            group != null && query.isNotBlank() -> {
                groupRepository.searchInGroup(group, query, type)
            }
            // Uniquement groupe
            group != null -> {
                groupRepository.getChannelsByGroup(type, group)
            }
            // Uniquement recherche
            query.isNotBlank() -> {
                groupRepository.searchChannels(query, type)
            }
            // Aucun filtre - tous les canaux
            else -> {
                groupRepository.getChannelsByGroup(type, "") // Retourne tous
            }
        }
        
        flow.collect { channelsList ->
            _channels.value = channelsList
            _isLoading.value = false
            Timber.d("📺 ${channelsList.size} chaînes affichées")
        }
    }

    /**
     * Recherche globale dans tous les groupes
     */
    fun performGlobalSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchQuery.value = query
            
            groupRepository.searchChannels(query, _contentType.value)
                .collect { results ->
                    _channels.value = results
                    _isLoading.value = false
                    Timber.i("🔍 Recherche '$query': ${results.size} résultats")
                }
        }
    }

    /**
     * Filtre par catégorie prédéfinie
     */
    fun filterByCategory(category: GroupCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            
            groupRepository.getChannelsByCategory(category, _contentType.value)
                .collect { channelsList ->
                    _channels.value = channelsList
                    _isLoading.value = false
                    Timber.i("🏷️ Catégorie $category: ${channelsList.size} chaînes")
                }
        }
    }

    /**
     * Réinitialise tous les filtres
     */
    fun clearFilters() {
        _selectedGroup.value = null
        _searchQuery.value = ""
        loadGroups(_contentType.value)
    }

    /**
     * Vérifie si des filtres sont actifs
     */
    fun hasActiveFilters(): Boolean {
        return _selectedGroup.value != null || _searchQuery.value.isNotBlank()
    }
}

/**
 * UI State pour l'écran de filtrage
 */
data class GroupFilterUiState(
    val groups: List<GroupInfo> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val selectedGroup: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val contentType: ContentType = ContentType.LIVE_TV,
    val totalChannelCount: Int = 0,
    val filteredChannelCount: Int = 0
)
