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
import com.skyplayer.pro.ui.navigation.BottomNavItem
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
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItemData(
            route = Routes.LiveTV.route,
            label = "Live TV",
            icon = Icons.Default.LiveTv,
            selectedColor = LiveTvColor
        ),
        NavItemData(
            route = Routes.VOD.route,
            label = "Films",
            icon = Icons.Default.Movie,
            selectedColor = VodColor
        ),
        NavItemData(
            route = Routes.Series.route,
            label = "Séries",
            icon = Icons.Default.Tv,
            selectedColor = SeriesColor
        ),
        NavItemData(
            route = Routes.Favorites.route,
            label = "Favoris",
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
            val selected = currentRoute == item.route
            
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
                onClick = { onNavigate(item.route) },
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
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedColor: androidx.compose.ui.graphics.Color
)

// Extension pour dp dans NavigationBar
typealias DP = androidx.compose.ui.unit.Dp
private val Int.dp: DP get() = androidx.compose.ui.unit.Dp(value = this.toFloat())
