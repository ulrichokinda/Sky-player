package com.skyplayer.pro.ui.navigation

/**
 * Routes de navigation de l'application
 * Définit toutes les destinations possibles
 */
sealed class Routes(val route: String) {

    // Écrans principaux
    object Splash : Routes("splash")
    object Home : Routes("home")
    object Welcome : Routes("welcome")
    object Onboarding : Routes("onboarding")

    // Sections de contenu (Phase 2 — navigation par gestes)
    object MainSections : Routes("main_sections/{tab}") {
        fun createRoute(tab: MainTab) = "main_sections/${tab.route}"
    }

    // Routes legacy — redirigent vers MainSections
    object LiveTV : Routes("live_tv")
    object VOD : Routes("vod")
    object Series : Routes("series")
    object Favorites : Routes("favorites")

    // Lecteur
    object Player : Routes("player/{channelId}") {
        fun createRoute(channelId: String) = "player/$channelId"
    }

    // Page de détails Film ou Série
    object ContentDetail : Routes("content_detail/{contentId}") {
        fun createRoute(contentId: String) = "content_detail/$contentId"
    }

    // Multi-lecteur (2-4 chaînes simultanées)
    object MultiPlayer : Routes("multi_player/{channelId}") {
        fun createRoute(channelId: String) = "multi_player/$channelId"
    }

    // Gestion des playlists
    object AddPlaylist : Routes("add_playlist")
    object XtreamLogin : Routes("xtream_login")
    object ManagePlaylists : Routes("manage_playlists")

    // Scanner QR pour TV
    object QRScanner : Routes("qr_scanner")

    // Licence et activation
    object License : Routes("license")
    object Activation : Routes("activation")
    object TrialExpired : Routes("trial_expired")
    object MyLine : Routes("my_line")

    // Téléchargement progressif playlist MAC
    object DownloadProgress : Routes("download_progress")

    // Configuration à distance par QR Code (Expert)
    object RemoteConfig : Routes("remote_config")

    // Paramètres et sécurité
    object Settings : Routes("settings")
    object ParentalLock : Routes("parental_lock")
    object PinEntry : Routes("pin_entry/{destination}") {
        fun createRoute(destination: String) = "pin_entry/$destination"
    }

    // Recherche
    object Search : Routes("search")

    // Guide EPG
    object EpgGuide : Routes("epg_guide")

    // Historique
    object History : Routes("history")
}

/**
 * Éléments de la barre de navigation inférieure
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: String // Nom de l'icône Material
) {
    object LiveTV : BottomNavItem(
        route = Routes.LiveTV.route,
        title = "Live TV",
        icon = "live_tv"
    )

    object VOD : BottomNavItem(
        route = Routes.VOD.route,
        title = "VOD",
        icon = "movie"
    )

    object Series : BottomNavItem(
        route = Routes.Series.route,
        title = "Séries",
        icon = "tv"
    )

    object Favorites : BottomNavItem(
        route = Routes.Favorites.route,
        title = "Favoris",
        icon = "favorite"
    )

    companion object {
        val items = listOf(LiveTV, VOD, Series, Favorites)
    }
}

/** Routes affichant la barre de navigation inférieure */
val bottomNavRoutes = setOf(
    Routes.MainSections.route
)

/** Vrai pour main_sections/live, main_sections/vod, etc. */
fun isMainSectionsRoute(route: String?): Boolean =
    route?.startsWith("main_sections/") == true

/** Extrait l'onglet actif depuis la route courante */
fun currentMainTab(route: String?): MainTab {
    if (route == null) return MainTab.LIVE
    return when {
        route.startsWith("main_sections/") -> {
            val tab = route.removePrefix("main_sections/")
            MainTab.fromRoute(tab)
        }
        route == Routes.LiveTV.route -> MainTab.LIVE
        route == Routes.VOD.route -> MainTab.VOD
        route == Routes.Series.route -> MainTab.SERIES
        route == Routes.Favorites.route -> MainTab.FAVORITES
        else -> MainTab.LIVE
    }
}
