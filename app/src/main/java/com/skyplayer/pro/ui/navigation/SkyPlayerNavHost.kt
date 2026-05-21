package com.skyplayer.pro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.fillMaxSize
import com.skyplayer.pro.ui.screens.favorites.FavoritesScreen
import com.skyplayer.pro.ui.screens.live.LiveTVScreen
import com.skyplayer.pro.ui.screens.player.MultiPlayerScreen
import com.skyplayer.pro.ui.screens.player.PlayerScreen
import com.skyplayer.pro.ui.screens.playlist.AddPlaylistScreen
import com.skyplayer.pro.ui.screens.playlist.ManagePlaylistsScreen
import com.skyplayer.pro.ui.screens.playlist.QRScannerScreen
import com.skyplayer.pro.ui.screens.series.SeriesScreen
import com.skyplayer.pro.ui.screens.remoteconfig.RemoteConfigScreen
import com.skyplayer.pro.ui.screens.settings.SettingsScreen
import com.skyplayer.pro.ui.screens.detail.ContentDetailScreen
import com.skyplayer.pro.ui.screens.license.LicenseScreen
import com.skyplayer.pro.ui.screens.license.TrialExpiredScreen
import com.skyplayer.pro.ui.screens.license.MyLineScreen
import com.skyplayer.pro.ui.screens.home.DashboardScreen
import com.skyplayer.pro.ui.screens.welcome.WelcomeScreen
import com.skyplayer.pro.ui.screens.vod.VODScreen
import com.skyplayer.pro.ui.screens.splash.SplashScreen
import com.skyplayer.pro.ui.screens.splash.DownloadProgressScreen
import com.skyplayer.pro.ui.navigation.enterToPlayer
import com.skyplayer.pro.ui.navigation.exitFromPlayer
import com.skyplayer.pro.ui.navigation.slideInFromBottom
import com.skyplayer.pro.ui.navigation.slideOutToBottom

/**
 * Host de navigation principal de l'application
 * Configure toutes les routes et la structure de navigation
 */
@Composable
fun SkyPlayerNavHost(
    navController: NavHostController,
    startDestination: String = Routes.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
            // Écran de démarrage minimaliste - 3 secondes
            composable(Routes.Splash.route) {
                SplashScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Téléchargement progressif playlist MAC
            composable(Routes.DownloadProgress.route) {
                DownloadProgressScreen(
                    onDownloadComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.DownloadProgress.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Dashboard principal (Style Hot Player)
            composable(Routes.Home.route) {
                DashboardScreen(
                    onNavigateToLive = { navController.navigate(Routes.LiveTV.route) },
                    onNavigateToVOD = { navController.navigate(Routes.VOD.route) },
                    onNavigateToSeries = { navController.navigate(Routes.Series.route) },
                    onNavigateToFavorites = { navController.navigate(Routes.Favorites.route) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                    onNavigateToRemoteConfig = { navController.navigate(Routes.RemoteConfig.route) },
                    onNavigateToAddPlaylist = { navController.navigate(Routes.AddPlaylist.route) },
                    onNavigateToScannerTV = { navController.navigate(Routes.RemoteConfig.route) },
                    onNavigateToEditPlaylist = { navController.navigate(Routes.AddPlaylist.route) }
                )
            }
            
            // Écran de bienvenue (première utilisation)
            composable(Routes.Welcome.route) {
                WelcomeScreen(
                    onAddPlaylist = {
                        navController.navigate(Routes.AddPlaylist.route)
                    },
                    onRemoteConfig = {
                        navController.navigate(Routes.RemoteConfig.route)
                    },
                    onSkip = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Section Live TV (exclusive, Back = Home)
            composable(Routes.LiveTV.route) {
                LiveTVScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Routes.Player.createRoute(channel.id))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.Settings.route)
                    },
                    onNavigateToMultiView = { channelId ->
                        navController.navigate(Routes.MultiPlayer.createRoute(channelId))
                    },
                    onBackToHome = {
                        navController.popBackStack(Routes.Home.route, inclusive = false)
                    }
                )
            }
            
            // Section VOD (exclusive, Back = Home)
            composable(Routes.VOD.route) {
                VODScreen(
                    onContentClick = { content ->
                        navController.navigate(Routes.ContentDetail.createRoute(content.id))
                    },
                    onBackToHome = {
                        navController.popBackStack(Routes.Home.route, inclusive = false)
                    }
                )
            }
            
            // Section Séries (exclusive, Back = Home)
            composable(Routes.Series.route) {
                SeriesScreen(
                    onSeriesClick = { series ->
                        navController.navigate(Routes.ContentDetail.createRoute(series.id))
                    },
                    onBackToHome = {
                        navController.popBackStack(Routes.Home.route, inclusive = false)
                    }
                )
            }
            
            // Section Favoris (exclusive, Back = Home)
            composable(Routes.Favorites.route) {
                FavoritesScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Routes.Player.createRoute(channel.id))
                    },
                    onBackToHome = {
                        navController.popBackStack(Routes.Home.route, inclusive = false)
                    }
                )
            }
            
            // Page de détails Film / Série
            composable(
                route = Routes.ContentDetail.route,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
                ContentDetailScreen(
                    contentId = contentId,
                    onPlayClick = { id ->
                        navController.navigate(Routes.Player.createRoute(id))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Lecteur vidéo avec animations fluides
            composable(
                route = Routes.Player.route,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.StringType }
                ),
                enterTransition = { enterToPlayer() },
                exitTransition = { exitFromPlayer() },
                popEnterTransition = { enterToPlayer() },
                popExitTransition = { exitFromPlayer() }
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                PlayerScreen(
                    channelId = channelId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNavigateToMultiView = {
                        navController.navigate(Routes.MultiPlayer.createRoute(channelId))
                    }
                )
            }
            
            // Multi-lecteur (2-4 chaînes)
            composable(
                route = Routes.MultiPlayer.route,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                MultiPlayerScreen(
                    initialChannelId = channelId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddChannel = {
                        // Navigation vers sélection de chaîne
                        navController.navigate(Routes.LiveTV.route)
                    }
                )
            }
            
            // Ajout de playlist
            composable(Routes.AddPlaylist.route) {
                AddPlaylistScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onPlaylistAdded = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.AddPlaylist.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Gestion des playlists
            composable(Routes.ManagePlaylists.route) {
                ManagePlaylistsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Scanner QR pour TV
            composable(Routes.QRScanner.route) {
                QRScannerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Écran de licence et activation
            composable(Routes.License.route) {
                LicenseScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.License.route) { inclusive = true }
                        }
                    }
                )
            }

            // Écran expiration essai 15 jours
            composable(Routes.TrialExpired.route) {
                TrialExpiredScreen()
            }

            // Ma Ligne: MAC + playlist active + statut abonnement (depuis Paramètres)
            composable(Routes.MyLine.route) {
                MyLineScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            // Paramètres
            composable(Routes.Settings.route) {
                SettingsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNavigateToRemoteConfig = {
                        navController.navigate(Routes.RemoteConfig.route)
                    },
                    onNavigateToMyLine = {
                        // Affiche la fiche d'activation: MAC + playlist active + statut abonnement
                        navController.navigate(Routes.MyLine.route)
                    }
                )
            }

            // Configuration à distance par QR Code (TV)
            composable(Routes.RemoteConfig.route) {
                RemoteConfigScreen(
                    onConfigApplied = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.RemoteConfig.route) { inclusive = true }
                        }
                    }
                )
        }
    }
}
