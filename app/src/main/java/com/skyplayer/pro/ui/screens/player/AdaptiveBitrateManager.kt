package com.skyplayer.pro.ui.screens.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Gestionnaire de qualité adaptative (ABR - Adaptive Bitrate Streaming)
 * Bascule automatiquement entre SD, 420p, 720p, 1080p, 4K selon la stabilité réseau
 * Compatible avec HLS, DASH et SmoothStreaming
 */
@UnstableApi
class AdaptiveBitrateManager(
    private val exoPlayer: ExoPlayer,
    private val trackSelector: DefaultTrackSelector
) {
    // Événements de Safety Mode (ex: passage forcé en SD car buffer faible)
    private val _safetyEvents = MutableSharedFlow<SafetyEvent>()
    val safetyEvents: SharedFlow<SafetyEvent> = _safetyEvents.asSharedFlow()

    // État de la qualité actuelle
    private val _currentQuality = MutableStateFlow<VideoQuality>(VideoQuality.AUTO)
    val currentQuality: StateFlow<VideoQuality> = _currentQuality.asStateFlow()
    
    // Liste des qualités disponibles
    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities.asStateFlow()
    
    // État de la bande passante estimée
    private val _bandwidthEstimate = MutableStateFlow<Long>(0L)
    val bandwidthEstimate: StateFlow<Long> = _bandwidthEstimate.asStateFlow()

    // Mode économie de données (limite la qualité max)
    private var isDataSaverEnabled = false
    
    // Surveillance de la stabilité réseau
    private val _networkStability = MutableStateFlow<NetworkStability>(NetworkStability.UNKNOWN)
    val networkStability: StateFlow<NetworkStability> = _networkStability.asStateFlow()
    
    // Buffer monitor pour décisions de qualité
    private var bufferHistory = mutableListOf<Long>()
    private var lastBufferUpdate = 0L
    
    companion object {
        // Seuils de bande passante en bits/sec pour chaque qualité
        const val THRESHOLD_SD = 800_000L      // 800 kbps - 420p
        const val THRESHOLD_720P = 2_500_000L  // 2.5 Mbps - 720p
        const val THRESHOLD_1080P = 5_000_000L // 5 Mbps - 1080p
        const val THRESHOLD_4K = 15_000_000L   // 15 Mbps - 4K
        
        // Seuils de buffer pour stabilité
        const val BUFFER_CRITICAL = 3_000L     // 3s - qualité doit baisser
        const val BUFFER_LOW = 8_000L        // 8s - qualité stable
        const val BUFFER_GOOD = 15_000L     // 15s - qualité peut augmenter
        const val BUFFER_EXCELLENT = 25_000L // 25s+ - qualité max
    }
    
    /**
     * Qualités vidéo supportées
     */
    enum class VideoQuality(
        val label: String,
        val height: Int,
        val minBandwidth: Long,
        val description: String
    ) {
        AUTO("Auto", 0, 0, "Qualité adaptative"),
        SD_240("240p", 240, 400_000, "Très basse qualité"),
        SD_360("360p", 360, 600_000, "Basse qualité"),
        SD_420("420p", 420, 800_000, "Qualité standard (SD)"),
        HD_720("720p", 720, 2_500_000, "Haute définition (HD)"),
        FHD_1080("1080p", 1080, 5_000_000, "Full HD"),
        UHD_4K("4K", 2160, 15_000_000, "Ultra HD 4K");
        
        companion object {
            fun fromHeight(height: Int): VideoQuality? {
                return values().find { it.height == height && it != AUTO }
            }
            
            fun fromBandwidth(bandwidth: Long): VideoQuality {
                return when {
                    bandwidth >= THRESHOLD_4K -> UHD_4K
                    bandwidth >= THRESHOLD_1080P -> FHD_1080
                    bandwidth >= THRESHOLD_720P -> HD_720
                    bandwidth >= THRESHOLD_SD -> SD_420
                    bandwidth >= 600_000 -> SD_360
                    bandwidth >= 400_000 -> SD_240
                    else -> SD_240
                }
            }
        }
    }
    
    enum class NetworkStability {
        UNKNOWN,
        EXCELLENT,    // Buffer > 25s, bande passante stable
        GOOD,         // Buffer 15-25s
        STABLE,       // Buffer 8-15s
        UNSTABLE,     // Buffer 3-8s, fluctuations
        POOR          // Buffer < 3s, risque de buffering
    }
    
    /**
     * Analyse les pistes disponibles et extrait les qualités
     */
    fun analyzeAvailableTracks(tracks: Tracks) {
        val qualities = mutableSetOf<VideoQuality>()
        
        tracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    format.height.takeIf { it > 0 }?.let { height ->
                        VideoQuality.fromHeight(height)?.let { quality ->
                            qualities.add(quality)
                        }
                    }
                }
            }
        }
        
        val sortedQualities = qualities.sortedBy { it.height }
        _availableQualities.value = listOf(VideoQuality.AUTO) + sortedQualities
        
        Timber.i("Qualités disponibles: ${sortedQualities.joinToString { it.label }}")
    }
    
    /**
     * Met à jour l'estimation de bande passante depuis le BandwidthMeter
     */
    fun updateBandwidthEstimate(estimateKbps: Long) {
        _bandwidthEstimate.value = estimateKbps * 1000 // Convertir en bps
        
        // En mode auto, évaluer si changement de qualité nécessaire
        if (_currentQuality.value == VideoQuality.AUTO) {
            evaluateAutoQuality()
        }
    }
    
    /**
     * Met à jour l'état du buffer pour évaluer la stabilité
     */
    fun updateBufferState(bufferedDurationMs: Long) {
        val now = System.currentTimeMillis()
        
        // Ajouter à l'historique (garder les 10 dernières valeurs)
        bufferHistory.add(bufferedDurationMs)
        if (bufferHistory.size > 10) {
            bufferHistory.removeAt(0)
        }
        
        // Évaluer la stabilité
        val stability = when {
            bufferedDurationMs >= BUFFER_EXCELLENT -> NetworkStability.EXCELLENT
            bufferedDurationMs >= BUFFER_GOOD -> NetworkStability.GOOD
            bufferedDurationMs >= BUFFER_LOW -> NetworkStability.STABLE
            bufferedDurationMs >= BUFFER_CRITICAL -> NetworkStability.UNSTABLE
            else -> NetworkStability.POOR
        }
        
        // Ne changer que si la stabilité change significativement
        if (stability != _networkStability.value) {
            _networkStability.value = stability
            Timber.d("Stabilité réseau: $stability (buffer: ${bufferedDurationMs/1000}s)")
        }
        
        lastBufferUpdate = now
    }
    
    /**
     * Active/Désactive le mode économie de données
     */
    fun setDataSaverEnabled(enabled: Boolean) {
        this.isDataSaverEnabled = enabled
        if (_currentQuality.value == VideoQuality.AUTO) {
            evaluateAutoQuality()
        }
    }

    /**
     * Évalue et ajuste la qualité en mode automatique
     */
    private fun evaluateAutoQuality() {
        val bandwidth = _bandwidthEstimate.value
        val stability = _networkStability.value
        val buffer = bufferHistory.lastOrNull() ?: 0L
        
        // Sélectionner la qualité optimale basée sur les conditions réseau
        var targetQuality = when {
            // Buffer critique → baisser immédiatement la qualité
            buffer < BUFFER_CRITICAL -> {
                val current = getCurrentPlaybackQuality()
                val lowerQuality = VideoQuality.values()
                    .filter { it != VideoQuality.AUTO && it.height < current.height }
                    .maxByOrNull { it.height }
                
                val target = lowerQuality ?: VideoQuality.SD_240
                
                // Déclencher événement Safety Mode
                if (target.height < current.height) {
                    _safetyEvents.tryEmit(SafetyEvent.QUALITY_DOWNGRADE_BUFFER_LOW)
                }
                
                target
            }
            
            // Buffer excellent + bonne bande passante → qualité max possible
            stability == NetworkStability.EXCELLENT && bandwidth >= THRESHOLD_4K -> {
                getMaxAvailableQuality()
            }
            
            // Bonne stabilité → adapter à la bande passante
            stability == NetworkStability.GOOD || stability == NetworkStability.STABLE -> {
                val bandwidthQuality = VideoQuality.fromBandwidth(bandwidth)
                // Choisir la plus basse entre bande passante et qualité max disponible
                listOf(bandwidthQuality, getMaxAvailableQuality()).minByOrNull { it.height } ?: bandwidthQuality
            }
            
            // Instable → être conservateur, utiliser qualité inférieure
            stability == NetworkStability.UNSTABLE -> {
                val bandwidthQuality = VideoQuality.fromBandwidth(bandwidth)
                // Descendre d'un niveau pour stabilité
                val saferQuality = VideoQuality.values()
                    .filter { it != VideoQuality.AUTO && it.height < bandwidthQuality.height }
                    .maxByOrNull { it.height }
                saferQuality ?: VideoQuality.SD_420
            }
            
            // Mauvais réseau → qualité minimale
            else -> VideoQuality.SD_360
        }

        // Appliquer la limite Économie de Data si activé
        if (isDataSaverEnabled && targetQuality.height > VideoQuality.SD_420.height) {
            targetQuality = VideoQuality.SD_420
        }
        
        // Appliquer la qualité si différente
        val currentHeight = getCurrentPlaybackHeight()
        if (targetQuality.height != currentHeight && targetQuality != VideoQuality.AUTO) {
            Timber.i("Auto-switch qualité: ${currentHeight}p → ${targetQuality.label} " +
                    "(bande passante: ${bandwidth/1000}kbps, buffer: ${buffer/1000}s, stabilité: $stability)")
            applyQuality(targetQuality)
        }
    }
    
    /**
     * Force une qualité spécifique (manuel ou auto)
     */
    fun setQuality(quality: VideoQuality) {
        _currentQuality.value = quality
        
        when (quality) {
            VideoQuality.AUTO -> enableAdaptiveBitrate()
            else -> applyQuality(quality)
        }
        
        Timber.i("Qualité changée: ${quality.label}")
    }
    
    /**
     * Active le bitrate adaptatif natif d'ExoPlayer
     */
    private fun enableAdaptiveBitrate() {
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSizeSd() // Par défaut, ExoPlayer gère
                .clearVideoSizeConstraints()
        )
    }
    
    /**
     * Applique une qualité vidéo spécifique
     */
    private fun applyQuality(quality: VideoQuality) {
        val parameters = trackSelector.buildUponParameters()
        
        if (quality == VideoQuality.AUTO) {
            parameters.clearVideoSizeConstraints()
        } else {
            // Restreindre à la hauteur max souhaitée
            parameters.setMaxVideoSize(quality.height, Int.MAX_VALUE)
            // Minimum pour éviter qualités trop basses si possible
            if (quality.height >= 720) {
                parameters.setMinVideoSize(480, Int.MAX_VALUE)
            }
        }
        
        trackSelector.setParameters(parameters)
    }
    
    /**
     * Récupère la qualité max disponible parmi les pistes
     */
    private fun getMaxAvailableQuality(): VideoQuality {
        val available = _availableQualities.value.filter { it != VideoQuality.AUTO }
        return available.maxByOrNull { it.height } ?: VideoQuality.FHD_1080
    }
    
    /**
     * Récupère la hauteur actuelle de lecture
     */
    private fun getCurrentPlaybackHeight(): Int {
        // ExoPlayer ne fournit pas directement la hauteur actuelle en ABR
        // On utilise la qualité cible comme approximation
        return _currentQuality.value.height
    }
    
    /**
     * Récupère la qualité actuelle de lecture
     */
    private fun getCurrentPlaybackQuality(): VideoQuality {
        val height = getCurrentPlaybackHeight()
        return VideoQuality.fromHeight(height) ?: VideoQuality.AUTO
    }
    
    /**
     * Obtient une recommandation de qualité basée sur les conditions actuelles
     */
    fun getQualityRecommendation(): QualityRecommendation {
        val bandwidth = _bandwidthEstimate.value
        val stability = _networkStability.value
        
        return QualityRecommendation(
            recommendedQuality = VideoQuality.fromBandwidth(bandwidth),
            currentStability = stability,
            reason = when (stability) {
                NetworkStability.EXCELLENT -> "Excellent réseau — Qualité maximale."
                NetworkStability.GOOD -> "Bon réseau — Qualité adaptée."
                NetworkStability.STABLE -> "Réseau stable."
                NetworkStability.UNSTABLE -> "Réseau instable — Qualité réduite."
                NetworkStability.POOR -> "Connexion faible — Qualité minimale."
                NetworkStability.UNKNOWN -> "Analyse en cours..."
            }
        )
    }
    
    data class QualityRecommendation(
        val recommendedQuality: VideoQuality,
        val currentStability: NetworkStability,
        val reason: String
    )

    /**
     * Événements de sécurité pour l'UI
     */
    enum class SafetyEvent {
        QUALITY_DOWNGRADE_BUFFER_LOW,
        NETWORK_UNSTABLE_STAYING_LOW,
        MIRROR_SWITCH_RECOMMENDED
    }
    
    /**
     * Réinitialise le gestionnaire
     */
    fun reset() {
        bufferHistory.clear()
        _currentQuality.value = VideoQuality.AUTO
        _networkStability.value = NetworkStability.UNKNOWN
        _bandwidthEstimate.value = 0L
        enableAdaptiveBitrate()
    }
}
