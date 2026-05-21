package com.skyplayer.pro.ui.screens.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.ui.components.CategorySidebar
import com.skyplayer.pro.ui.components.ChannelLoadMonitor
import com.skyplayer.pro.ui.components.ChannelListShimmer
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.LiveTvColor
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.viewmodel.OrganizedContentViewModel
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.skyplayer.pro.ui.components.PinDialog
import com.skyplayer.pro.ui.viewmodel.ParentalViewModel
import androidx.activity.compose.BackHandler

/**
 * Écran Live TV (Style Hot Player)
 * Sidebar à gauche, Grille de chaînes à droite
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun LiveTVScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMultiView: (String) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onBackToHome: () -> Unit = {},
    viewModel: OrganizedContentViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    BackHandler { onBackToHome() }
    val categories by viewModel.liveCategories.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val channels by viewModel.liveChannels.collectAsState()
    val currentPrograms by viewModel.currentPrograms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<ChannelCategory?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Split-screen state: premier clic = preview, deuxième clic = fullscreen
    var previewChannel by remember { mutableStateOf<Channel?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Sidebar Latérale (Catégories)
        CategorySidebar(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                if (parentalViewModel.manager.isSensitiveCategory(category.name)) {
                    pendingCategory = category
                    showPinDialog = true
                } else {
                    viewModel.selectLiveCategory(category.name)
                }
            },
            onSearchQueryChange = { viewModel.searchLive(it) }
        )

        // Contenu Principal (Grille) — réduit quand preview ouvert
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(if (previewChannel != null) 0.55f else 1f)
        ) {
            // Header Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedCategory?.uppercase() ?: "TOUTES LES CHAÎNES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "${channels.size} chaînes disponibles",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigateToMultiView("") },
                        modifier = Modifier.background(CardBlack, CircleShape)
                    ) {
                        Icon(Icons.Default.Dashboard, null, tint = ElectricSkyBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.background(CardBlack, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = Color.White)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        ChannelListShimmer()
                    }
                    categories.isEmpty() -> {
                        EmptyStateWithMonitoring()
                    }
                    channels.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune chaîne trouvée",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = channels,
                                key = { it.id }
                            ) { channel ->
                                val program = currentPrograms[channel.epgId]
                                ChannelTile(
                                    channel = channel,
                                    currentProgram = program,
                                    isSelected = previewChannel?.id == channel.id,
                                    onClick = {
                                        if (previewChannel?.id == channel.id) {
                                            // 2ème clic → plein écran
                                            onChannelClick(channel)
                                        } else {
                                            // 1er clic → split-screen preview
                                            previewChannel = channel
                                        }
                                    },
                                    onLongClick = { onNavigateToMultiView(channel.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Panneau split-screen preview (droite)
        previewChannel?.let { ch ->
            SplitScreenPreviewPanel(
                channel = ch,
                epgPrograms = currentPrograms[ch.epgId]?.let { listOf(it) } ?: emptyList(),
                onPlayFullscreen = { onChannelClick(ch) },
                onClose = { previewChannel = null },
                modifier = Modifier.weight(0.45f)
            )
        }
    }

    if (showPinDialog) {
        PinDialog(
            onConfirm = { pin ->
                if (parentalViewModel.manager.checkPin(pin)) {
                    pendingCategory?.let { viewModel.selectLiveCategory(it.name) }
                    showPinDialog = false
                    pinError = null
                } else {
                    pinError = "Code PIN incorrect"
                }
            },
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            error = pinError
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChannelTile(
    channel: Channel,
    currentProgram: com.skyplayer.pro.data.model.EpgProgram? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isSelected) ElectricSkyBlue else Color.White.copy(alpha = 0.1f)
    val borderWidth = if (isSelected) 2.dp else 0.5.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardBlack.copy(alpha = 0.9f) else CardBlack
        ),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fond avec un léger gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(ElevatedBlack, CardBlack)
                        )
                    )
            )
            
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logoUrl != null) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxHeight().widthIn(max = 100.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                // Nom de la chaîne
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // EPG Info
                if (currentProgram != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentProgram.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricSkyBlue.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = currentProgram.getProgress(),
                        modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).clip(CircleShape),
                        color = ElectricSkyBlue,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            // Indicateur Favori discret
            if (channel.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateWithMonitoring() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucune chaîne disponible",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. Ajoutez une playlist (Xtream ou M3U)\n" +
                       "2. Les chaînes se chargeront automatiquement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            ChannelLoadMonitor()
        }
    }
}

/**
 * Panneau split-screen droite:
 * - Mini-lecteur ExoPlayer (lecture réelle de la chaîne)
 * - Clic sur la vignette → plein écran
 * - EPG des programmes à venir
 * - Bouton fermer (X) pour retourner à la grille seule
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun SplitScreenPreviewPanel(
    channel: Channel,
    epgPrograms: List<com.skyplayer.pro.data.model.EpgProgram>,
    onPlayFullscreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── Créer et gérer le cycle de vie ExoPlayer ──────────
    val exoPlayer = remember(channel.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(channel.id) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF080808))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── En-tête: nom chaîne + bouton fermer ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EN DIRECT — ${channel.name}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Fermer",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Mini-lecteur ExoPlayer ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable { onPlayFullscreen() }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay "Appuyez pour plein écran"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Appuyez pour le plein écran",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        }

        // ── Guide EPG ─────────────────────────────────────
        Text(
            text = "GUIDE DES PROGRAMMES",
            style = MaterialTheme.typography.labelSmall.copy(
                color = ElectricSkyBlue,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )

        if (epgPrograms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedBlack, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun programme EPG disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        } else {
            epgPrograms.take(6).forEach { program ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElevatedBlack, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val progress = program.getProgress()
                        if (progress > 0f) {
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(CircleShape),
                                color = ElectricSkyBlue,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
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
            // Logo de la chaîne
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
                        tint = LiveTvColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informations
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
            
            // Bouton favori
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (channel.isFavorite) 
                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (channel.isFavorite) "Retirer favori" else "Ajouter favori",
                    tint = if (channel.isFavorite) LiveTvColor 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
