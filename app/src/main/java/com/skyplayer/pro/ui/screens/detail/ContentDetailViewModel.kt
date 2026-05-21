package com.skyplayer.pro.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class ContentDetailUiState {
    object Loading : ContentDetailUiState()
    data class Success(val channel: Channel, val metadata: ContentMetadata?) : ContentDetailUiState()
    data class Error(val message: String) : ContentDetailUiState()
}

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContentDetailUiState>(ContentDetailUiState.Loading)
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    fun loadContent(contentId: String) {
        viewModelScope.launch {
            _uiState.value = ContentDetailUiState.Loading
            try {
                val channel = channelRepository.getChannelById(contentId)
                if (channel == null) {
                    _uiState.value = ContentDetailUiState.Error("Contenu introuvable: $contentId")
                    return@launch
                }
                val metadata = database.contentMetadataDao().getMetadata(contentId)
                _uiState.value = ContentDetailUiState.Success(channel, metadata)
                Timber.d("ContentDetail chargé: ${channel.name}")
            } catch (e: Exception) {
                Timber.e(e, "Erreur chargement ContentDetail: $contentId")
                _uiState.value = ContentDetailUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
}
