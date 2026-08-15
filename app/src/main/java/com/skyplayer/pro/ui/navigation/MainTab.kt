package com.skyplayer.pro.ui.navigation

/**
 * Onglets principaux de l'application (Phase 2 — navigation par gestes).
 */
enum class MainTab(val route: String, val index: Int) {
    LIVE("live", 0),
    VOD("vod", 1),
    SERIES("series", 2),
    FAVORITES("favorites", 3);

    companion object {
        fun fromRoute(route: String?): MainTab =
            entries.find { it.route == route } ?: LIVE

        fun fromIndex(index: Int): MainTab =
            entries.getOrElse(index) { LIVE }
    }
}
