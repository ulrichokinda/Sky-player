package com.skyplayer.pro.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.navigation.MainTab
import com.skyplayer.pro.ui.theme.FavoritesColor
import com.skyplayer.pro.ui.theme.LiveTvColor
import com.skyplayer.pro.ui.theme.SeriesColor
import com.skyplayer.pro.ui.theme.VodColor

/**
 * Barre de navigation inférieure personnalisée
 * Indicateur d'onglet actif qui glisse en douceur entre les sections,
 * icône active avec léger rebond — navigation fluide et captivante.
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

    val selectedIndex = items.indexOfFirst { it.tab == currentTab }.coerceAtLeast(0)
    val selectedColor = items[selectedIndex].selectedColor

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MaterialTheme.colorScheme.surface)
            .shadow(16.dp, ambientColor = Color.Black.copy(alpha = 0.5f))
    ) {
        val itemWidth = maxWidth / items.size

        // Indicateur qui glisse horizontalement vers l'onglet actif
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "navIndicator"
        )

        // Indicateur glissant en dégradé derrière l'onglet actif
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            selectedColor.copy(alpha = 0.22f),
                            selectedColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(1.dp, selectedColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex

                // Léger rebond de l'icône quand l'onglet devient actif
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navIconScale"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.tab) }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale),
                        tint = if (selected) item.selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) item.selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class NavItemData(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
    val selectedColor: Color
)
