package com.skyplayer.pro.data.model

import android.content.Context
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn

/**
 * Représente les différentes qualités vidéo disponibles
 * Pour optimiser la connexion sur réseaux lents
 */
enum class VideoQuality(val label: String, val bitrate: Int, val height: Int) {
    AUTO("Auto (Adaptatif)", -1, -1),
    LOW("Basse (480p)", 800_000, 480),      // ~0.8 Mbps
    MEDIUM("Moyenne (720p)", 2_500_000, 720), // ~2.5 Mbps
    HIGH("Haute (1080p)", 5_000_000, 1080),   // ~5 Mbps
    UHD("Ultra HD (4K)", 15_000_000, 2160);   // ~15 Mbps

    companion object {
        fun fromString(value: String): VideoQuality {
            return entries.find { it.name == value } ?: AUTO
        }

        /**
         * Détermine la meilleure qualité selon la vitesse de connexion
         */
        fun fromBandwidth(bandwidthKbps: Long): VideoQuality {
            return when {
                bandwidthKbps < 1_000 -> LOW      // < 1 Mbps
                bandwidthKbps < 3_000 -> MEDIUM   // < 3 Mbps
                bandwidthKbps < 6_000 -> HIGH     // < 6 Mbps
                else -> UHD                       // > 6 Mbps
            }
        }
    }
}

/**
 * Préférences de streaming pour optimiser la lecture
 */
data class StreamingPreferences(
    val preferredQuality: VideoQuality = VideoQuality.AUTO,
    val autoAdjustQuality: Boolean = true,
    val bufferDurationSeconds: Int = 30,
    val maxBufferDurationSeconds: Int = 60,
    val connectionTimeoutMs: Int = 15000,
    val retryCount: Int = 3,
    val preferH265: Boolean = false,  // HEVC pour meilleure compression
    val lowLatencyMode: Boolean = false // Réduit la latence mais augmente le risque de buffering
) {
    /**
     * Retourne la qualité effective selon les préférences et le réseau
     */
    fun getEffectiveQuality(currentBandwidthKbps: Long): VideoQuality {
        return if (autoAdjustQuality && preferredQuality == VideoQuality.AUTO) {
            VideoQuality.fromBandwidth(currentBandwidthKbps)
        } else {
            preferredQuality
        }
    }
}

/**
 * Extension pour convertir la qualité en paramètre ExoPlayer
 */
@OptIn(UnstableApi::class)
fun VideoQuality.toTrackSelectionParameters(context: Context): TrackSelectionParameters.Builder {
    val builder = TrackSelectionParameters.Builder(context)

    return when (this) {
        VideoQuality.AUTO -> builder
        VideoQuality.LOW -> builder.setMaxVideoSizeSd()
        VideoQuality.MEDIUM -> builder.setMaxVideoSize(1280, 720)
        VideoQuality.HIGH -> builder.setMaxVideoSize(1920, 1080)
        VideoQuality.UHD -> builder // Pas de limite pour 4K
    }
}

/**
 * Extension pour convertir la qualité globale en qualité spécifique ABR
 */
fun VideoQuality.toAdaptiveQuality(): com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality {
    return when (this) {
        VideoQuality.AUTO -> com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality.AUTO
        VideoQuality.LOW -> com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality.SD_420
        VideoQuality.MEDIUM -> com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality.HD_720
        VideoQuality.HIGH -> com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality.FHD_1080
        VideoQuality.UHD -> com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality.UHD_4K
    }
}
