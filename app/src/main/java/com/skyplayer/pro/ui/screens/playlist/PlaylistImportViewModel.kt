package com.skyplayer.pro.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.parser.M3UParserFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel pour l'import de playlists avec parsing asynchrone par paquets
 * 
 * Démonstration du parsing de 50,000 entrées sans bloquer l'UI
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class PlaylistImportViewModel @Inject constructor() : ViewModel() {

    private val parser = M3UParserFlow()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // === ÉTAT UI ===
    
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()
    
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    
    private val _progress = MutableStateFlow(ImportProgress())
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Import une playlist M3U avec affichage progressif des résultats
     * Les chaînes apparaissent par paquets de 100 dès le début
     */
    fun importPlaylist(url: String, playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _importState.value = ImportState.Loading
            _channels.value = emptyList()
            _progress.value = ImportProgress()
            
            val startTime = System.currentTimeMillis()
            var totalChannels = 0
            var firstBatchReceived = false
            
            try {
                parser.parseFromUrlAsFlow(url, playlistId, okHttpClient)
                    .flowOn(Dispatchers.IO)
                    .collect { batch: List<Channel> ->
                        // Afficher les premiers résultats instantanément (~100ms)
                        if (!firstBatchReceived) {
                            val firstBatchTime = System.currentTimeMillis() - startTime
                            Timber.i("🚀 Premier paquet reçu en ${firstBatchTime}ms: ${batch.size} chaînes")
                            firstBatchReceived = true
                            _importState.value = ImportState.Streaming
                        }
                        
                        // Accumuler les chaînes
                        totalChannels += batch.size
                        _channels.value = _channels.value + batch
                        
                        // Mettre à jour la progression
                        _progress.value = ImportProgress(
                            receivedChannels = totalChannels,
                            lastBatchSize = batch.size,
                            elapsedMs = System.currentTimeMillis() - startTime
                        )
                        
                        Timber.d("📦 Paquet reçu: ${batch.size} chaînes (Total: $totalChannels)")
                    }
                
                val totalTime = System.currentTimeMillis() - startTime
                _importState.value = ImportState.Success(totalChannels, totalTime)
                Timber.i("✅ Import terminé: $totalChannels chaînes en ${totalTime}ms (${totalChannels * 1000 / totalTime}/s)")
                
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Erreur inconnue")
                Timber.e(e, "❌ Erreur import playlist")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Import avec parsing depuis contenu texte (fichier local)
     */
    fun importFromContent(content: String, playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _importState.value = ImportState.Loading
            _channels.value = emptyList()
            
            val startTime = System.currentTimeMillis()
            
            parser.parseAsFlow(content, playlistId)
                .flowOn(Dispatchers.IO)
                .buffer(10) // Buffer pour éviter le backpressure
                .collect { batch: List<Channel> ->
                    _channels.value = _channels.value + batch
                    
                    // Afficher la progression toutes les 1000 chaînes
                    val total = _channels.value.size
                    if (total % 1000 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        Timber.i("⏱️ Parsing: $total chaînes en ${elapsed}ms")
                    }
                }
            
            _isLoading.value = false
            _importState.value = ImportState.Success(
                _channels.value.size, 
                System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Version avec debounce pour limiter les mises à jour UI
     * Utile si la liste est très longue et l'UI lag
     */
    fun importWithThrottling(url: String, playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            
            parser.parseFromUrlAsFlow(url, playlistId, okHttpClient)
                .debounce(100) // Limiter à 10 mises à jour/seconde max
                .flowOn(Dispatchers.IO)
                .collect { batch: List<Channel> ->
                    _channels.value = _channels.value + batch
                }
            
            _isLoading.value = false
        }
    }
    
    fun reset() {
        _channels.value = emptyList()
        _importState.value = ImportState.Idle
        _progress.value = ImportProgress()
        _isLoading.value = false
    }
}

// === CLASSES D'ÉTAT ===

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState() // Téléchargement en cours
    object Streaming : ImportState() // Parsing et affichage progressif
    data class Success(val totalChannels: Int, val durationMs: Long) : ImportState()
    data class Error(val message: String) : ImportState()
}

data class ImportProgress(
    val receivedChannels: Int = 0,
    val lastBatchSize: Int = 0,
    val elapsedMs: Long = 0
) {
    val channelsPerSecond: Int
        get() = if (elapsedMs > 0) (receivedChannels * 1000 / elapsedMs).toInt() else 0
}
