package com.skyplayer.pro.ui.navigation

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.skyplayer.pro.ui.components.TrustStatusBanner
import com.skyplayer.pro.ui.viewmodel.AppStatusViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.skyplayer.pro.ui.components.BottomNavBar
import com.skyplayer.pro.ui.screens.detail.ContentDetailScreen
import com.skyplayer.pro.ui.screens.home.DashboardScreen
import com.skyplayer.pro.ui.screens.license.LicenseScreen
import com.skyplayer.pro.ui.screens.license.MyLineScreen
import com.skyplayer.pro.ui.screens.license.TrialExpiredScreen
import com.skyplayer.pro.ui.screens.player.MultiPlayerScreen
import com.skyplayer.pro.ui.screens.player.PlayerScreen
import com.skyplayer.pro.ui.screens.playlist.AddPlaylistScreen
import com.skyplayer.pro.ui.screens.playlist.ManagePlaylistsScreen
import com.skyplayer.pro.ui.screens.playlist.QRScannerScreen
import com.skyplayer.pro.ui.screens.epg.EpgGuideScreen
import com.skyplayer.pro.ui.screens.remoteconfig.RemoteConfigScreen
import com.skyplayer.pro.ui.screens.search.SearchScreen
import com.skyplayer.pro.ui.screens.settings.SettingsScreen
import com.skyplayer.pro.ui.screens.splash.DownloadProgressScreen
import com.skyplayer.pro.ui.screens.splash.SplashScreen
import com.skyplayer.pro.ui.screens.onboarding.OnboardingScreen
import com.skyplayer.pro.ui.screens.parental.SimplifiedParentalSetupScreen
import com.skyplayer.pro.ui.screens.tv.TvDashboardScreen
import com.skyplayer.pro.ui.screens.tv.TvPlayerScreen

/**
 * Host de navigation principal de l'application.
 * Les sections Live / VOD / Séries / Favoris partagent une barre de navigation inférieure.
 */
@UnstableApi
@Composable
fun SkyPlayerNavHost(
    navController: NavHostController,
    startDestination: String = Routes.Splash.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val isTV = (context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
        .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    val currentTab = currentMainTab(currentRoute)
    val showBottomBar = !isTV && (
        currentRoute?.startsWith("main_sections/") == true ||
        currentRoute in setOf(
            Routes.LiveTV.route,
            Routes.VOD.route,
            Routes.Series.route,
            Routes.Favorites.route
        )
    )
    val showStatusBanner = !isTV && (
        currentRoute == Routes.Home.route || isMainSectionsRoute(currentRoute)
    )

    val appStatusViewModel: AppStatusViewModel = hiltViewModel()
    val appStatus by appStatusViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentRoute) {
        if (showStatusBanner) {
            appStatusViewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            if (showStatusBanner) {
                TrustStatusBanner(
                    state = appStatus,
                    onClick = { navController.navigate(Routes.MyLine.route) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentTab = currentTab,
                    onNavigate = { tab ->
                        val targetRoute = Routes.MainSections.createRoute(tab)
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Routes.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromRight() },
            popExitTransition = { slideOutToLeft() }
        ) {
            composable(Routes.Splash.route) {
                SplashScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToWelcome = {
                        navController.navigate(Routes.Welcome.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DownloadProgress.route) {
                DownloadProgressScreen(
                    onDownloadComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.DownloadProgress.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Home.route) {
                if (isTV) {
                    TvDashboardScreen(
                        onChannelClick = { channel ->
                            navController.navigate(Routes.Player.createRoute(channel.id))
                        },
                        onSettingsClick = {
                            navController.navigate(Routes.Settings.route)
                        },
                        onParentalClick = {
                            navController.navigate(Routes.ParentalLock.route)
                        },
                        onNavigateToRemoteConfig = {
                            navController.navigate(Routes.RemoteConfig.route)
                        }
                    )
                } else {
                    DashboardScreen(
                        onNavigateToLive = { navController.navigate(Routes.MainSections.createRoute(MainTab.LIVE)) },
                        onNavigateToVOD = { navController.navigate(Routes.MainSections.createRoute(MainTab.VOD)) },
                        onNavigateToSeries = { navController.navigate(Routes.MainSections.createRoute(MainTab.SERIES)) },
                        onNavigateToFavorites = { navController.navigate(Routes.MainSections.createRoute(MainTab.FAVORITES)) },
                        onNavigateToParentalSetup = { navController.navigate(Routes.ParentalLock.route) },
                        onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                        onNavigateToRemoteConfig = { navController.navigate(Routes.RemoteConfig.route) },
                        onNavigateToSearch = { navController.navigate(Routes.Search.route) },
                        onNavigateToAddPlaylist = { navController.navigate(Routes.AddPlaylist.route) },
                        onNavigateToScannerTV = { navController.navigate(Routes.RemoteConfig.route) },
                        onNavigateToEditPlaylist = { navController.navigate(Routes.ManagePlaylists.route) },
                        onPlayChannel = { channel ->
                            navController.navigate(Routes.Player.createRoute(channel.id))
                        },
                        onPlayContent = { channel ->
                            navController.navigate(Routes.ContentDetail.createRoute(channel.id))
                        }
                    )
                }
            }

            composable(Routes.Welcome.route) {
                OnboardingScreen(
                    onAddPlaylist = {
                        navController.navigate(Routes.AddPlaylist.route)
                    },
                    onRemoteConfig = {
                        navController.navigate(Routes.RemoteConfig.route)
                    },
                    onComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onAddPlaylist = {
                        navController.navigate(Routes.AddPlaylist.route)
                    },
                    onRemoteConfig = {
                        navController.navigate(Routes.RemoteConfig.route)
                    },
                    onComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.MainSections.route,
                arguments = listOf(
                    navArgument("tab") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val tab = MainTab.fromRoute(backStackEntry.arguments?.getString("tab"))
                MainSectionsScreen(
                    initialTab = tab,
                    onTabChanged = { newTab ->
                        val newRoute = Routes.MainSections.createRoute(newTab)
                        if (currentRoute != newRoute) {
                            navController.navigate(newRoute) {
                                popUpTo(Routes.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onChannelClick = { channel ->
                        navController.navigate(Routes.Player.createRoute(channel.id))
                    },
                    onContentClick = { content ->
                        navController.navigate(Routes.ContentDetail.createRoute(content.id))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.Settings.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Routes.Search.route)
                    },
                    onNavigateToAddPlaylist = {
                        navController.navigate(Routes.AddPlaylist.route)
                    },
                    onNavigateToMultiView = { channelId ->
                        navController.navigate(Routes.MultiPlayer.createRoute(channelId))
                    },
                    onNavigateToEpgGuide = {
                        navController.navigate(Routes.EpgGuide.route)
                    },
                    onNavigateToLive = {
                        navController.navigate(Routes.MainSections.createRoute(MainTab.LIVE)) {
                            popUpTo(Routes.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Redirections legacy vers MainSections (compatibilité)
            composable(Routes.LiveTV.route) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.MainSections.createRoute(MainTab.LIVE)) {
                        popUpTo(Routes.LiveTV.route) { inclusive = true }
                    }
                }
            }
            composable(Routes.VOD.route) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.MainSections.createRoute(MainTab.VOD)) {
                        popUpTo(Routes.VOD.route) { inclusive = true }
                    }
                }
            }
            composable(Routes.Series.route) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.MainSections.createRoute(MainTab.SERIES)) {
                        popUpTo(Routes.Series.route) { inclusive = true }
                    }
                }
            }
            composable(Routes.Favorites.route) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.MainSections.createRoute(MainTab.FAVORITES)) {
                        popUpTo(Routes.Favorites.route) { inclusive = true }
                    }
                }
            }

            composable(Routes.ParentalLock.route) {
                SimplifiedParentalSetupScreen(
                    onBackClick = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() }
                )
            }

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
                        navController.navigate(Routes.MainSections.createRoute(MainTab.LIVE))
                    }
                )
            }

            composable(Routes.AddPlaylist.route) {
                AddPlaylistScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onPlaylistAdded = {
                        appStatusViewModel.refresh()
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.AddPlaylist.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ManagePlaylists.route) {
                ManagePlaylistsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.QRScanner.route) {
                QRScannerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.License.route) {
                LicenseScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.License.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.TrialExpired.route) {
                TrialExpiredScreen()
            }

            composable(Routes.MyLine.route) {
                MyLineScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.Settings.route) {
                SettingsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNavigateToRemoteConfig = {
                        navController.navigate(Routes.RemoteConfig.route)
                    },
                    onNavigateToMyLine = {
                        navController.navigate(Routes.MyLine.route)
                    },
                    onNavigateToParentalSetup = {
                        navController.navigate(Routes.ParentalLock.route)
                    }
                )
            }

            composable(Routes.RemoteConfig.route) {
                RemoteConfigScreen(
                    onConfigApplied = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.RemoteConfig.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Search.route) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onChannelClick = { channel ->
                        navController.navigate(Routes.Player.createRoute(channel.id))
                    },
                    onContentClick = { content ->
                        navController.navigate(Routes.ContentDetail.createRoute(content.id))
                    }
                )
            }

            composable(Routes.EpgGuide.route) {
                EpgGuideScreen(
                    onBackClick = { navController.popBackStack() },
                    onChannelClick = { channel ->
                        navController.navigate(Routes.Player.createRoute(channel.id))
                    }
                )
            }
        }
    }
}
