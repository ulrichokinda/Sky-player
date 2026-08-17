package com.skyplayer.pro.ui.screens.favorites

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.theme.FavoritesColor
import com.skyplayer.pro.ui.components.SectionTopBar
import com.skyplayer.pro.ui.components.TrustAction
import com.skyplayer.pro.ui.components.TrustStateView
import com.skyplayer.pro.ui.theme.PureBlack
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.delay

/**
 * Écran Favoris - Liste des chaînes favorites
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onChannelClick: (Channel) -> Unit,
    onBackToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLive: () -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    BackHandler { onBackToHome() }
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Focus initial sur la liste pour la télécommande TV
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val contentFocusRequester = remember { FocusRequester() }
    // On ne demande le focus que lorsque la liste est réellement composée
    // (pas pendant le shimmer de chargement) pour rester robuste.
    LaunchedEffect(isTV, isLoading, favorites.isEmpty()) {
        if (isTV && !isLoading && favorites.isNotEmpty()) {
            delay(150)
            contentFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FavoritesColor.copy(alpha = 0.07f),
                        PureBlack,
                        PureBlack
                    )
                )
            )
    ) {
        SectionTopBar(
            title = stringResource(R.string.section_favorites),
            subtitle = stringResource(R.string.section_items_count, favorites.size),
            accentColor = FavoritesColor,
            onNavigateHome = onBackToHome,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToSettings = onNavigateToSettings
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LoadingState()
            } else if (favorites.isEmpty()) {
                TrustStateView(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.trust_empty_favorites_title),
                    message = stringResource(R.string.trust_empty_favorites_message),
                    iconTint = FavoritesColor.copy(alpha = 0.45f),
                    primaryAction = TrustAction(
                        label = stringResource(R.string.trust_action_explore_live),
                        onClick = onNavigateToLive
                    )
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(contentFocusRequester)
                ) {
                    items(favorites, key = { it.id }) { channel ->
                        FavoriteChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onRemove = { viewModel.removeFromFavorites(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = FavoritesColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            // Bouton supprimer
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Retirer des favoris",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = FavoritesColor)
    }
}

