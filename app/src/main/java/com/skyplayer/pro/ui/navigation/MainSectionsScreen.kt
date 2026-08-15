package com.skyplayer.pro.ui.navigation

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.screens.favorites.FavoritesScreen
import com.skyplayer.pro.ui.screens.live.LiveTVScreen
import com.skyplayer.pro.ui.screens.series.SeriesScreen
import com.skyplayer.pro.ui.screens.vod.VODScreen
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Conteneur Phase 2 — navigation par gestes (swipe) entre les sections principales.
 * Synchronise le HorizontalPager avec la barre de navigation inférieure.
 */
@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun MainSectionsScreen(
    initialTab: MainTab,
    onTabChanged: (MainTab) -> Unit,
    onNavigateToHome: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onContentClick: (Channel) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    onNavigateToMultiView: (String) -> Unit,
    onNavigateToEpgGuide: () -> Unit,
    onNavigateToLive: () -> Unit
) {
    val context = LocalContext.current
    val isTV = (context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
        .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    val pagerState = rememberPagerState(
        initialPage = initialTab.index,
        pageCount = { MainTab.entries.size }
    )

    LaunchedEffect(initialTab) {
        if (pagerState.currentPage != initialTab.index) {
            pagerState.animateScrollToPage(initialTab.index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val tab = MainTab.fromIndex(page)
                if (tab != initialTab) {
                    onTabChanged(tab)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !isTV,
        beyondViewportPageCount = 1
    ) { page ->
        when (MainTab.fromIndex(page)) {
            MainTab.LIVE -> LiveTVScreen(
                onChannelClick = onChannelClick,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToMultiView = onNavigateToMultiView,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToEpgGuide = onNavigateToEpgGuide,
                onNavigateToAddPlaylist = onNavigateToAddPlaylist,
                onBackToHome = onNavigateToHome
            )

            MainTab.VOD -> VODScreen(
                onContentClick = onContentClick,
                onBackToHome = onNavigateToHome,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAddPlaylist = onNavigateToAddPlaylist
            )

            MainTab.SERIES -> SeriesScreen(
                onSeriesClick = onContentClick,
                onBackToHome = onNavigateToHome,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAddPlaylist = onNavigateToAddPlaylist
            )

            MainTab.FAVORITES -> FavoritesScreen(
                onChannelClick = onChannelClick,
                onBackToHome = onNavigateToHome,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToLive = onNavigateToLive
            )
        }
    }
}
