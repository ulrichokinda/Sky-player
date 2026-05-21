package com.skyplayer.pro.ui.screens.player

import androidx.media3.common.util.UnstableApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.PlayerConnectionState
import com.skyplayer.pro.data.monitor.StreamHealthMonitor
import com.skyplayer.pro.data.prefetch.StreamPrefetchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow

/**
 * ViewModel du lecteur avec gestion de la reconnexion exponentielle
 * Optimisé pour les réseaux instables et zapping instantané (Suggestion 1)
 */
@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: ExoPlayer,
    private val trackSelector: DefaultTrackSelector,
    private val channelRepository: com.skyplayer.pro.data.repository.ChannelRepository,
    private val epgRepository: com.skyplayer.pro.data.repository.EpgRepository,
    private val streamHealthMonitor: StreamHealthMonitor,
    private val prefetchManager: StreamPrefetchManager,
    private val encryptedPrefs: com.skyplayer.pro.data.encrypted.EncryptedPrefs
) : ViewModel() {

    // EPG
    private val _currentProgram = MutableStateFlow<com.skyplayer.pro.data.model.EpgProgram?>(null)
    val currentProgram: StateFlow<com.skyplayer.pro.data.model.EpgProgram?> = _currentProgram.asStateFlow()

    private val _exoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val exoPlayer: StateFlow<ExoPlayer?> = _exoPlayer.asStateFlow()
    
    // Gestionnaire de qualité adaptative (ABR)
    val adaptiveBitrateManager = AdaptiveBitrateManager(player, trackSelector)

    private val _connectionState = MutableStateFlow<PlayerConnectionState>(PlayerConnectionState.Idle)
    val connectionState: StateFlow<PlayerConnectionState> = _connectionState.asStateFlow()

    // Mode Économie de Données (Turbo)
    private val _isDataSaverEnabled = MutableStateFlow(encryptedPrefs.isDataSaverEnabled())
    val isDataSaverEnabled: StateFlow<Boolean> = _isDataSaverEnabled.asStateFlow()

    // États de santé du stream (Failover)
    val healthState = streamHealthMonitor.healthState
    val fallbackInfo = streamHealthMonitor.fallbackInfo

    private var currentChannel: Channel? = null
    private var retryCount = 0
    private var retryJob: Job? = null
    private var bufferMonitorJob: Job? = null
    private val maxRetries = 15 // Augmenté pour réseaux très instables
    
    // Configuration de la reconnexion exponentielle
    private val baseDelayMs = 2000L // 2 secondes initiales
    private val maxDelayMs = 120000L // 2 minutes max entre tentatives
    
    // État du buffer pour l'UI
    private val _bufferState = MutableStateFlow(BufferState())
    val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    _connectionState.value = PlayerConnectionState.Idle
                    isPlaying = false
                    stopBufferMonitoring()
                }
                Player.STATE_BUFFERING -> {
                    _connectionState.value = PlayerConnectionState.Buffering
                    startBufferMonitoring()
                }
                Player.STATE_READY -> {
                    _connectionState.value = PlayerConnectionState.Ready
                    retryCount = 0
                    isPlaying = player.playWhenReady
                    startBufferMonitoring()
                }
                Player.STATE_ENDED -> {
                    isPlaying = false
                    stopBufferMonitoring()
                    if (currentChannel?.type?.name == "LIVE_TV") {
                        attemptReconnect()
                    }
                }
            }
        }
        
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            isPlaying = playWhenReady
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            error?.let {
                Timber.e(it, "Erreur lecture ExoPlayer: ${it.errorCodeName} - ${it.message}")
                
                // Log détaillé pour debugging réseau
                when (it.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        Timber.w("Connexion réseau échouée - Tentative reconnexion...")
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        Timber.w("Timeout réseau - Connexion trop lente ou instable")
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        Timber.w("Erreur HTTP serveur - Vérifier URL playlist")
                    PlaybackException.ERROR_CODE_TIMEOUT ->
                        Timber.w("Timeout général - Buffer insuffisant ou réseau coupé")
                }
                
                // Déterminer si l'erreur est récupérable
                val isRecoverable = when (it.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                    PlaybackException.ERROR_CODE_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true
                    else -> it.cause is IOException || it.cause is java.net.SocketTimeoutException
                }
                
                if (isRecoverable && retryCount < maxRetries) {
                    _connectionState.value = PlayerConnectionState.Error(it, retryCount)
                    attemptReconnect()
                } else {
                    _connectionState.value = PlayerConnectionState.Error(it, retryCount)
                    if (retryCount >= maxRetries) {
                        Timber.e("Nombre max de tentatives ($maxRetries) atteint - Abandon")
                    }
                }
            }
        }
        
        override fun onIsLoadingChanged(isLoading: Boolean) {
            Timber.d("Chargement réseau: $isLoading")
        }
        
        override fun onTracksChanged(tracks: Tracks) {
            // Analyser les pistes disponibles pour extraction des qualités
            adaptiveBitrateManager.analyzeAvailableTracks(tracks)
        }
    }
    
    /**
     * Surveillance du buffer pour affichage UI et debugging
     */
    private fun startBufferMonitoring() {
        bufferMonitorJob?.cancel()
        bufferMonitorJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val bufferedPosition = player.bufferedPosition
                    val totalBufferedDuration = player.totalBufferedDuration
                    
                    _bufferState.value = BufferState(
                        bufferedPositionMs = bufferedPosition,
                        totalBufferedDurationMs = totalBufferedDuration,
                        bufferedPercentage = player.bufferedPercentage
                    )
                    
                    // Mettre à jour le gestionnaire de qualité adaptative
                    adaptiveBitrateManager.updateBufferState(totalBufferedDuration)
                    
                    // Log toutes les 5 secondes
                    if (totalBufferedDuration > 0) {
                        val seconds = totalBufferedDuration / 1000
                        Timber.d("Buffer: ${seconds}s / 120s max (${player.bufferedPercentage}%)")
                    }
                    
                    delay(1000) // Mettre à jour chaque seconde
                } catch (e: Exception) {
                    // Ignorer les erreurs de monitoring
                }
            }
        }
    }
    
    private fun stopBufferMonitoring() {
        bufferMonitorJob?.cancel()
        bufferMonitorJob = null
    }

    init {
        player.addListener(playerListener)
        _exoPlayer.value = player

        // Appliquer Turbo Mode si activé par défaut
        if (_isDataSaverEnabled.value) {
            adaptiveBitrateManager.setQuality(AdaptiveBitrateManager.VideoQuality.SD_420)
        }

        // Configurer les callbacks du Health Monitor pour le failover automatique
        streamHealthMonitor.onSwitchToMirror = { mirrorUrl ->
            switchToMirror(mirrorUrl)
        }

        streamHealthMonitor.onSwitchToAlternative = { alternative ->
            switchToAlternative(alternative)
        }
    }

    /**
     * Bascule transparente vers un lien miroir (même chaîne)
     */
    private fun switchToMirror(mirrorUrl: String) {
        viewModelScope.launch {
            Timber.i("🔄 Failover: Bascule vers miroir $mirrorUrl")
            val currentPos = player.currentPosition
            
            player.stop()
            player.setMediaItem(MediaItem.fromUri(mirrorUrl))
            player.prepare()
            player.playWhenReady = true
            player.seekTo(currentPos)
        }
    }

    /**
     * Bascule vers une chaîne alternative similaire
     */
    private fun switchToAlternative(alternative: Channel) {
        viewModelScope.launch {
            Timber.i("🔄 Failover: Bascule vers alternative ${alternative.name}")
            currentChannel = alternative
            
            player.stop()
            player.setMediaItem(MediaItem.fromUri(alternative.url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    /**
     * Charge un canal pour la lecture avec gestion intelligente du buffer et Health Monitoring
     * Intègre le pré-chargement pour zapping instantané (Suggestion 1)
     */
    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = PlayerConnectionState.Connecting
                
                val channel = channelRepository.getChannelById(channelId)
                currentChannel = channel
                
                channel?.let {
                    Timber.d("Chargement canal: ${it.name} - URL: ${it.url}")
                    
                    // 1. Tenter récupération du player pré-chargé (Zapping instantané)
                    val prefetchedPlayer = prefetchManager.getPrefetchedPlayer(it.id)
                    
                    if (prefetchedPlayer != null) {
                        Timber.i("⚡ Zapping INSTANTANÉ pour ${it.name}")
                    }
                    
                    // 2. Créer MediaItem et DÉMARRER IMMÉDIATEMENT (priorité = playback rapide)
                        val mediaItem = MediaItem.Builder()
                            .setUri(it.url)
                            .setLiveConfiguration(
                                MediaItem.LiveConfiguration.Builder()
                                    .setMaxPlaybackSpeed(1.02f) // Léger catch-up pour live
                                    .setMinPlaybackSpeed(0.98f)
                                    .build()
                            )
                            .build()
                        
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true
                        
                        Timber.i("Lecture démarrée: ${it.name}")
                        
                    // 3. Opérations non-bloquantes en arrière-plan (ne retardent pas le playback)
                    viewModelScope.launch {
                        try {
                            val allChannels = channelRepository.getAllChannels().first()
                            streamHealthMonitor.startMonitoring(player, it, allChannels)
                            prefetchManager.updateCurrentPosition(it, allChannels)
                        } catch (e: Exception) {
                            Timber.w(e, "Health/Prefetch init secondaire échoué (non bloquant)")
                        }
                    }
                        
                        // Enregistrer dans l'historique
                        channelRepository.updateLastWatched(channelId)
                        
                        // Charger EPG
                        loadEpg(it.epgId)
                } ?: run {
                    _connectionState.value = PlayerConnectionState.Error(
                        Exception("Canal non trouvé"), 0
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Erreur chargement canal: $channelId")
                _connectionState.value = PlayerConnectionState.Error(e, retryCount)
                attemptReconnect()
            }
        }
    }

    /**
     * Tente une reconnexion avec backoff exponentiel adaptatif
     * Stratégie: Délai croissant mais avec reset si réseau stable entre temps
     * Formule: min(baseDelay * 2^retryCount, maxDelay)
     */
    private fun attemptReconnect() {
        retryJob?.cancel()
        
        if (retryCount >= maxRetries) {
            Timber.w("Nombre max de tentatives atteint ($maxRetries)")
            _connectionState.value = PlayerConnectionState.Error(
                Exception("Connexion impossible après $maxRetries tentatives. Vérifiez votre réseau."),
                retryCount
            )
            return
        }
        
        retryCount++
        
        // Calcul du délai exponentiel avec jitter pour éviter les storms
        val baseDelay = baseDelayMs * (2.0.pow(retryCount - 1)).toLong()
        val jitter = (0..2000).random() // Jitter aléatoire 0-2s
        val delayMs = min(baseDelay + jitter, maxDelayMs)
        
        Timber.d("Tentative de reconnexion $retryCount/$maxRetries dans ${delayMs/1000}s (buffer: ${_bufferState.value.totalBufferedDurationMs/1000}s)")
        
        retryJob = viewModelScope.launch {
            _connectionState.value = PlayerConnectionState.Reconnecting
            delay(delayMs)
            
            currentChannel?.let { channel ->
                try {
                    Timber.d("Reconnexion à: ${channel.url}")
                    
                    // Reset complet du player pour clean state
                    player.stop()
                    player.clearMediaItems()
                    
                    // Recréer le MediaItem
                    val mediaItem = MediaItem.Builder()
                        .setUri(channel.url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setMaxPlaybackSpeed(1.02f)
                                .setMinPlaybackSpeed(0.98f)
                                .build()
                        )
                        .build()
                    
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                    
                    Timber.i("Reconnexion réussie tentative $retryCount")
                    
                } catch (e: Exception) {
                    Timber.e(e, "Échec reconnexion tentative $retryCount")
                    if (retryCount < maxRetries) {
                        attemptReconnect()
                    } else {
                        _connectionState.value = PlayerConnectionState.Error(e, retryCount)
                    }
                }
            }
        }
    }
    
    /**
     * Données du buffer pour monitoring
     */
    data class BufferState(
        val bufferedPositionMs: Long = 0L,
        val totalBufferedDurationMs: Long = 0L,
        val bufferedPercentage: Int = 0
    ) {
        fun formatDuration(): String {
            val seconds = totalBufferedDurationMs / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return if (minutes > 0) {
                "${minutes}m ${remainingSeconds}s"
            } else {
                "${seconds}s"
            }
        }
    }

    /**
     * Relance manuelle de la lecture
     */
    fun retry() {
        retryCount = 0
        currentChannel?.let {
            loadChannel(it.id)
        }
    }
    
    // === CONTRÔLES LECTURE ===
    
    var isPlaying by mutableStateOf(false)
        private set
    
    var playbackSpeed by mutableStateOf(1.0f)
        private set
    
    /**
     * Toggle Play/Pause
     */
    fun togglePlayPause() {
        player.let {
            it.playWhenReady = !it.playWhenReady
            isPlaying = it.playWhenReady
            Timber.d("Lecture: ${if (isPlaying) "Play" else "Pause"}")
        }
    }
    
    /**
     * Seek backward 10s
     */
    fun seekBackward() {
        player.let {
            val newPosition = (it.currentPosition - 10000).coerceAtLeast(0)
            it.seekTo(newPosition)
            Timber.d("Retour -10s: ${newPosition/1000}s")
        }
    }
    
    /**
     * Seek forward 10s
     */
    fun seekForward() {
        player.let {
            val newPosition = it.currentPosition + 10000
            it.seekTo(newPosition)
            Timber.d("Avance +10s: ${newPosition/1000}s")
        }
    }
    
    /**
     * Définir la vitesse de lecture
     */
    fun updatePlaybackSpeed(speed: Float) {
        player.let {
            it.setPlaybackSpeed(speed)
            playbackSpeed = speed
            Timber.d("Vitesse changée: ${speed}x")
        }
    }

    /**
     * Définir la qualité vidéo
     */
    fun setVideoQuality(quality: AdaptiveBitrateManager.VideoQuality) {
        adaptiveBitrateManager.setQuality(quality)
        Timber.d("Qualité vidéo changée: ${quality}")
    }

    /**
     * Bascule le mode économie de données (force SD/480p)
     */
    fun toggleDataSaver() {
        val newState = !_isDataSaverEnabled.value
        _isDataSaverEnabled.value = newState
        encryptedPrefs.saveDataSaverEnabled(newState)
        
        if (newState) {
            adaptiveBitrateManager.setQuality(AdaptiveBitrateManager.VideoQuality.SD_420)
            Timber.i("🚀 Mode Turbo activé (Persisté)")
        } else {
            adaptiveBitrateManager.setQuality(AdaptiveBitrateManager.VideoQuality.AUTO)
            Timber.i("🚀 Mode Turbo désactivé")
        }
    }
    
    /**
     * Charge l'EPG pour le canal actuel
     */
    private fun loadEpg(epgId: String?) {
        if (epgId == null) return
        viewModelScope.launch {
            _currentProgram.value = epgRepository.getCurrentProgram(epgId)
        }
    }
    fun releasePlayer() {
        retryJob?.cancel()
        adaptiveBitrateManager.reset()
        streamHealthMonitor.stopMonitoring()
        player.removeListener(playerListener)
        player.stop()
        player.clearMediaItems()
        // NOTE: Ne pas appeler player.release() ici si player est injecté via Hilt sans @Singleton
        // Car Hilt pourrait fournir la même instance à d'autres composants.
        // On laisse le Garbage Collector ou le module gérer la libération.
        _exoPlayer.value = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
