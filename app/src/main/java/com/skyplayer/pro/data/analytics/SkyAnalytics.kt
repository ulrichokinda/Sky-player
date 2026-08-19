package com.skyplayer.pro.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A6: Analytics métier Firebase — événements de lecture, zapping, recherche.
 *
 * Chaque événement est loggé avec des paramètres utiles pour le dashboard.
 * Les noms d'événements respectent les limites Firebase (40 chars, snake_case).
 */
@Singleton
class SkyAnalytics @Inject constructor() {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // ══════════════════════════════════════════════════════════════════
    // ÉVÉNEMENTS DE LECTURE
    // ══════════════════════════════════════════════════════════════════

    fun logPlayStart(
        channelName: String,
        channelType: String,     // "live", "vod", "series"
        streamType: String,      // "m3u", "xtream"
        category: String? = null
    ) {
        analytics.logEvent("play_start") {
            param("channel_name", channelName)
            param("channel_type", channelType)
            param("stream_type", streamType)
            category?.let { param("category", it) }
        }
        Timber.d("📊 Analytics: play_start — $channelName ($channelType)")
    }

    fun logPlayStop(
        channelName: String,
        durationSeconds: Long,
        completionPercent: Int     // % de la vidéo regardée
    ) {
        analytics.logEvent("play_stop") {
            param("channel_name", channelName)
            param("duration_sec", durationSeconds)
            param("completion_pct", completionPercent.toLong())
        }
        Timber.d("📊 Analytics: play_stop — $channelName, ${durationSeconds}s, ${completionPercent}%")
    }

    fun logPlayError(channelName: String, errorType: String, errorMessage: String) {
        analytics.logEvent("play_error") {
            param("channel_name", channelName)
            param("error_type", errorType)
            param("error_message", errorMessage.take(100))
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // ZAPPING (Live TV)
    // ══════════════════════════════════════════════════════════════════

    fun logZap(fromChannel: String, toChannel: String, method: String) {
        // method: "dpad", "sidebar", "click", "epg"
        analytics.logEvent("channel_zap") {
            param("from_channel", fromChannel)
            param("to_channel", toChannel)
            param("method", method)
        }
    }

    fun logCategoryChange(category: String, channelCount: Int) {
        analytics.logEvent("category_change") {
            param("category", category)
            param("channel_count", channelCount.toLong())
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // RECHERCHE
    // ══════════════════════════════════════════════════════════════════

    fun logSearch(query: String, resultCount: Int) {
        analytics.logEvent("search") {
            param("query", query.take(50))
            param("result_count", resultCount.toLong())
        }
    }

    fun logSearchResultClick(query: String, position: Int, channelName: String) {
        analytics.logEvent("search_result_click") {
            param("query", query.take(50))
            param("position", position.toLong())
            param("channel_name", channelName)
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // FAVORIS
    // ══════════════════════════════════════════════════════════════════

    fun logFavoriteToggle(channelName: String, isFavorite: Boolean) {
        analytics.logEvent("favorite_toggle") {
            param("channel_name", channelName)
            param("is_favorite", if (isFavorite) "true" else "false")
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // QUALITÉ / STREAM
    // ══════════════════════════════════════════════════════════════════

    fun logQualityChange(channelName: String, fromQuality: String, toQuality: String) {
        analytics.logEvent("quality_change") {
            param("channel_name", channelName)
            param("from_quality", fromQuality)
            param("to_quality", toQuality)
        }
    }

    fun logStreamHealthChange(channelName: String, healthStatus: String) {
        // healthStatus: "excellent", "good", "poor", "dead"
        analytics.logEvent("stream_health") {
            param("channel_name", channelName)
            param("status", healthStatus)
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SESSION / NAVIGATION
    // ══════════════════════════════════════════════════════════════════

    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    fun logPlaylistLoad(playlistType: String, channelCount: Int, loadTimeMs: Long) {
        analytics.logEvent("playlist_loaded") {
            param("playlist_type", playlistType)
            param("channel_count", channelCount.toLong())
            param("load_time_ms", loadTimeMs)
        }
    }

    fun logEPGLoad(programCount: Int, loadTimeMs: Long) {
        analytics.logEvent("epg_loaded") {
            param("program_count", programCount.toLong())
            param("load_time_ms", loadTimeMs)
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LICENCE / DEVICE
    // ══════════════════════════════════════════════════════════════════

    fun logDeviceCheck(status: String, daysRemaining: Int?) {
        analytics.logEvent("device_check") {
            param("status", status)
            daysRemaining?.let { param("days_remaining", it.toLong()) }
        }
    }

    fun logAppLaunch(isTrial: Boolean, isActivated: Boolean) {
        analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, Bundle())
        analytics.logEvent("app_launch_context") {
            param("is_trial", if (isTrial) "true" else "false")
            param("is_activated", if (isActivated) "true" else "false")
        }
    }
}
