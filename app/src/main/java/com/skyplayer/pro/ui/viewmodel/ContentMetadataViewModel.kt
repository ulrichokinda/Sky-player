package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.toContentMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour gérer les métadonnées des films et séries
 */
@HiltViewModel
class ContentMetadataViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val metadataDao = database.contentMetadataDao()

    private val _selectedMetadata = MutableStateFlow<ContentMetadata?>(null)
    val selectedMetadata: StateFlow<ContentMetadata?> = _selectedMetadata.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Charge les métadonnées pour un contenu spécifique
     */
    fun loadMetadata(channel: Channel) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Chercher en local d'abord
                val localMetadata = metadataDao.getMetadata(channel.id)

                if (localMetadata != null) {
                    _selectedMetadata.value = localMetadata
                    Timber.i("📋 Métadonnées chargées pour: ${channel.name}")
                } else {
                    // Si pas en local et c'est un Xtream, on pourrait fetch depuis l'API
                    // Pour l'instant, on crée des métadonnées basiques
                    val basicMetadata = createBasicMetadata(channel)
                    _selectedMetadata.value = basicMetadata
                    Timber.i("📋 Métadonnées basiques créées pour: ${channel.name}")
                }
            } catch (e: Exception) {
                _error.value = "Erreur chargement métadonnées: ${e.message}"
                Timber.e(e, "❌ Erreur chargement métadonnées")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sauvegarde les métadonnées enrichies depuis Xtream API
     */
    fun saveMetadata(metadata: ContentMetadata) {
        viewModelScope.launch {
            try {
                metadataDao.insertMetadata(metadata)
                _selectedMetadata.value = metadata
                Timber.i("💾 Métadonnées sauvegardées: ${metadata.title}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur sauvegarde métadonnées")
            }
        }
    }

    /**
     * Crée des métadonnées basiques depuis un Channel
     */
    private fun createBasicMetadata(channel: Channel): ContentMetadata {
        return ContentMetadata(
            contentId = channel.id,
            title = channel.name,
            plot = null,
            posterUrl = channel.logoUrl,
            sourceType = "local"
        )
    }

    /**
     * Efface la sélection actuelle
     */
    fun clearSelection() {
        _selectedMetadata.value = null
        _error.value = null
    }
}
