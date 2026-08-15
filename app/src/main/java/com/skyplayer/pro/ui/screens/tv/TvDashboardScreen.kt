package com.skyplayer.pro.ui.screens.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.ui.components.tv.TvChannelCard
import com.skyplayer.pro.ui.viewmodel.GroupFilterViewModel
import com.skyplayer.pro.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/**
 * Dashboard TV avec navigation optimisée pour télécommande
 * 
 * Structure :
 * - Menu vertical rétractable à gauche
 * - Grille de cartes à droite (Chaînes)
 * - Focus visuel clair (Scale + Bordure)
 */
@Composable
fun TvDashboardScreen(
    viewModel: GroupFilterViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    onChannelClick: (Channel) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onParentalClick: () -> Unit = {},
    onNavigateToRemoteConfig: () -> Unit = {}
) {
    val groups by viewModel.groups.collectAsState()
    val history by viewModel.watchHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Favoris réels depuis Room
    val favorites by favoritesViewModel.favorites.collectAsState(initial = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // FocusRequesters
    val menuFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }

    // État du menu rétractable
    var isMenuExpanded by remember { mutableStateOf(false) }
    var focusedMenuId by remember { mutableStateOf("live") }

    val menuWidth by animateDpAsState(if (isMenuExpanded) 240.dp else 80.dp)

    LaunchedEffect(Unit) {
        viewModel.loadGroups(ContentType.LIVE_TV)
        menuFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // === MENU LATÉRAL RÉTRACTABLE ===
        // Simplified focus handling
        val menuParentModifier = Modifier
            .width(menuWidth)
            .fillMaxHeight()
            .onFocusChanged {
                // Only expand on initial focus gain
                if (it.isFocused && !isMenuExpanded) {
                    isMenuExpanded = true
                }
            }

        TvSideMenu(
            isExpanded = isMenuExpanded,
            selectedId = focusedMenuId,
            onItemClick = { item ->
                focusedMenuId = item.id
                when (item.id) {
                    "live" -> {
                        viewModel.setContentType(ContentType.LIVE_TV)
                    }
                    "vod" -> {
                        viewModel.setContentType(ContentType.VOD_MOVIE)
                    }
                    "favorites" -> { /* géré par focusedMenuId */ }
                    "settings" -> onSettingsClick()
                    "parental" -> onParentalClick()
                    "config" -> onNavigateToRemoteConfig()
                }
            },
            focusRequester = menuFocusRequester,
            modifier = menuParentModifier
        )

        // === CONTENU PRINCIPAL ===
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Alignment.Center.let { Modifier.align(it) },
                    color = Color.White
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (focusedMenuId == "favorites") {
                        TvChannelsGrid(
                            channels = favorites,
                            selectedGroup = "Mes Favoris",
                            onChannelClick = onChannelClick,
                            onChannelLongClick = { favoritesViewModel.toggleFavorite(it) },
                            firstCardFocusRequester = firstCardFocusRequester,
                            favoriteChannelIds = favoriteIds
                        )
                    } else {
                        // Section Historique (uniquement sur Live TV)
                        if (focusedMenuId == "live" && history.isNotEmpty()) {
                            TvDashboardSection(
                                title = "Reprendre la lecture",
                                channelList = history,
                                onChannelClick = onChannelClick,
                                onChannelLongClick = { favoritesViewModel.toggleFavorite(it) },
                                favoriteChannelIds = favoriteIds
                            )
                        }

                        // Carrousels par catégories
                        groups.forEach { group ->
                            val groupChannels by viewModel.getChannelsByGroup(group.name).collectAsState(initial = emptyList())
                            if (groupChannels.isNotEmpty()) {
                                TvDashboardSection(
                                    title = group.name,
                                    channelList = groupChannels,
                                    onChannelClick = onChannelClick,
                                    onChannelLongClick = { favoritesViewModel.toggleFavorite(it) },
                                    favoriteChannelIds = favoriteIds
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDashboardSection(
    title: String,
    channelList: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    favoriteChannelIds: Set<String>
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        // Utilisation de LazyRow standard
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channelList, key = { it.id }) { channel ->
                var isFocused by remember { mutableStateOf(false) }
                TvChannelCard(
                    channel = channel,
                    isFocused = isFocused,
                    isFavorite = favoriteChannelIds.contains(channel.id),
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onChannelLongClick(channel) },
                    onFocusChanged = { isFocused = it }
                )
            }
        }
    }
}

/**
 * Menu vertical rétractable pour TV
 */
@Composable
private fun TvSideMenu(
    isExpanded: Boolean,
    selectedId: String,
    onItemClick: (MenuItem) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val menuItems = remember {
        listOf(
            MenuItem("live", "Live TV", Icons.Default.PlayArrow),
            MenuItem("favorites", "Favoris", Icons.Default.Star),
            MenuItem("vod", "Films & Séries", Icons.Default.Movie),
            MenuItem("config", "Configuration", Icons.Default.Tv),
            MenuItem("parental", "Code Parental", Icons.Default.Lock),
            MenuItem("settings", "Paramètres", Icons.Default.Settings)
        )
    }

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .background(Color(0xFF121212))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo / Titre
        if (isExpanded) {
            Text(
                text = "SKY PLAYER PRO",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).padding(bottom = 32.dp)
            )
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(menuItems) { index, item ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedId == item.id

                val scale by animateFloatAsState(if (isFocused) 1.1f else 1.0f)

                Surface(
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isExpanded) 16.dp else 8.dp)
                        .scale(scale)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                        .focusable(),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isFocused -> Color.White
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    },
                    contentColor = when {
                        isFocused -> Color.Black
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> Color.White.copy(alpha = 0.6f)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grille de chaînes optimisée pour TV avec focus visuel clair
 */
@Composable
private fun TvChannelsGrid(
    channels: List<Channel>,
    selectedGroup: String?,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    firstCardFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    favoriteChannelIds: Set<String> = emptySet()
) {
    Column(modifier = modifier) {
        Text(
            text = selectedGroup ?: "Toutes les chaînes",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun contenu dans cette catégorie", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isFavorite = favoriteChannelIds.contains(channel.id)
                    
                    TvChannelCard(
                        channel = channel,
                        isFocused = isFocused,
                        isFavorite = isFavorite,
                        onClick = { onChannelClick(channel) },
                        onLongClick = { onChannelLongClick(channel) },
                        onFocusChanged = { isFocused = it },
                        focusRequester = if (index == 0) firstCardFocusRequester else null
                    )
                }
            }
        }
    }
}

private data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)
