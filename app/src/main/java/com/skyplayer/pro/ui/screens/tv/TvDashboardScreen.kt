package com.skyplayer.pro.ui.screens.tv

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.ui.components.tv.TvChannelCard
import com.skyplayer.pro.ui.viewmodel.GroupFilterViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Dashboard TV avec navigation optimisée pour télécommande
 * 
 * Structure :
 * - Menu vertical à gauche (Categories)
 * - Grille de cartes à droite (Chaînes)
 * - Focus automatique sur le premier élément
 */
@Composable
fun TvDashboardScreen(
    viewModel: GroupFilterViewModel = hiltViewModel(),
    onChannelClick: (Channel) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val groups by viewModel.groups.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // État pour les favoris (dans une vraie app, viendrait du ViewModel)
    var favoriteChannelIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // FocusRequesters pour la navigation TV
    val menuFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }
    
    // État pour suivre le focus actuel
    var focusedMenuIndex by remember { mutableStateOf(0) }
    var isMenuFocused by remember { mutableStateOf(true) }
    
    // Fonction pour toggle les favoris
    val toggleFavorite = { channel: Channel ->
        favoriteChannelIds = if (favoriteChannelIds.contains(channel.id)) {
            favoriteChannelIds - channel.id
        } else {
            favoriteChannelIds + channel.id
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadGroups(ContentType.LIVE_TV)
        // Focus automatique sur le menu au lancement
        menuFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // === MENU VERTICAL GAUCHE ===
        TvSideMenu(
            selectedIndex = focusedMenuIndex,
            onItemClick = { index, item ->
                focusedMenuIndex = index
                when (item.id) {
                    "live" -> viewModel.setContentType(ContentType.LIVE_TV)
                    "vod" -> viewModel.setContentType(ContentType.VOD_MOVIE)
                    "settings" -> onSettingsClick()
                }
            },
            onFocusChanged = { hasFocus ->
                isMenuFocused = hasFocus
            },
            focusRequester = menuFocusRequester,
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
        )
        
        // === GRILLE DE CHAINES DROITE ===
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TvChannelsGrid(
                    channels = channels,
                    selectedGroup = selectedGroup,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = toggleFavorite,
                    firstCardFocusRequester = firstCardFocusRequester,
                    modifier = Modifier.fillMaxSize(),
                    favoriteChannelIds = favoriteChannelIds
                )
            }
        }
    }
}

/**
 * Menu vertical à gauche optimisé pour TV
 */
@Composable
private fun TvSideMenu(
    selectedIndex: Int,
    onItemClick: (Int, MenuItem) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val menuItems = remember {
        listOf(
            MenuItem("live", "Live TV", Icons.Default.PlayArrow),
            MenuItem("vod", "Films & Séries", Icons.Default.Movie),
            MenuItem("settings", "Paramètres", Icons.Default.Settings)
        )
    }
    
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(vertical = 32.dp)
    ) {
        // Logo
        Text(
            text = "SKY PLAYER",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Menu items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(menuItems) { index, item ->
                val itemFocusRequester = remember { FocusRequester() }
                var isFocused by remember { mutableStateOf(false) }
                
                // Auto-focus sur le premier élément
                LaunchedEffect(Unit) {
                    if (index == 0) {
                        itemFocusRequester.requestFocus()
                    }
                }
                
                Card(
                    onClick = { onItemClick(index, item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            if (index == 0) focusRequester else itemFocusRequester
                        )
                        .onFocusChanged { 
                            isFocused = it.isFocused
                            onFocusChanged(it.isFocused)
                        }
                        .focusable(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFocused) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.Transparent,
                        contentColor = if (isFocused) 
                            Color.Black 
                        else 
                            Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isFocused) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grille de chaînes optimisée pour TV
 */
@Composable
private fun TvChannelsGrid(
    channels: List<Channel>,
    selectedGroup: String?,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit = {},
    firstCardFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    favoriteChannelIds: Set<String> = emptySet()
) {
    Column(modifier = modifier) {
        // Titre du groupe sélectionné
        Text(
            text = selectedGroup ?: "Toutes les chaînes",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(channels) { index, channel ->
                val cardFocusRequester = remember { FocusRequester() }
                var isFocused by remember { mutableStateOf(false) }
                val isFavorite = remember(favoriteChannelIds, channel.id) {
                    favoriteChannelIds.contains(channel.id)
                }
                
                // Focus automatique sur la première carte
                LaunchedEffect(Unit) {
                    if (index == 0) {
                        firstCardFocusRequester.requestFocus()
                    }
                }
                
                TvChannelCard(
                    channel = channel,
                    isFocused = isFocused,
                    isFavorite = isFavorite,
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onChannelLongClick(channel) },
                    focusRequester = if (index == 0) firstCardFocusRequester else cardFocusRequester,
                    onFocusChanged = { focused -> isFocused = focused }
                )
            }
        }
    }
}


/**
 * Data class pour les items du menu
 */
private data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)
