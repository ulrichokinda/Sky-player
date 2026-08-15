package com.skyplayer.pro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.navigation.MainTab
import com.skyplayer.pro.ui.navigation.Routes
import com.skyplayer.pro.ui.theme.FavoritesColor
import com.skyplayer.pro.ui.theme.LiveTvColor
import com.skyplayer.pro.ui.theme.SeriesColor
import com.skyplayer.pro.ui.theme.VodColor

/**
 * Barre de navigation inférieure personnalisée
 * Navigation fluide entre les sections principales
 */
@Composable
fun BottomNavBar(
    currentTab: MainTab,
    onNavigate: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItemData(
            tab = MainTab.LIVE,
            label = stringResource(R.string.nav_live_tv),
            icon = Icons.Default.LiveTv,
            selectedColor = LiveTvColor
        ),
        NavItemData(
            tab = MainTab.VOD,
            label = stringResource(R.string.nav_vod),
            icon = Icons.Default.Movie,
            selectedColor = VodColor
        ),
        NavItemData(
            tab = MainTab.SERIES,
            label = stringResource(R.string.nav_series),
            icon = Icons.Default.Tv,
            selectedColor = SeriesColor
        ),
        NavItemData(
            tab = MainTab.FAVORITES,
            label = stringResource(R.string.nav_favorites),
            icon = Icons.Default.Favorite,
            selectedColor = FavoritesColor
        )
    )
    
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentTab == item.tab
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) item.selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (selected) item.selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = selected,
                onClick = { onNavigate(item.tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = item.selectedColor,
                    selectedTextColor = item.selectedColor,
                    indicatorColor = item.selectedColor.copy(alpha = 0.1f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private data class NavItemData(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
    val selectedColor: androidx.compose.ui.graphics.Color
)

// Extension pour dp dans NavigationBar
typealias DP = androidx.compose.ui.unit.Dp
private val Int.dp: DP get() = androidx.compose.ui.unit.Dp(value = this.toFloat())
