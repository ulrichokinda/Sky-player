package com.skyplayer.pro.ui.screens.live

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
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
import com.skyplayer.pro.ui.components.SectionTopBar
import com.skyplayer.pro.ui.components.HorizontalCategoryTabs
import com.skyplayer.pro.ui.components.TrustAction
import com.skyplayer.pro.ui.components.TrustStateView
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
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

import androidx.compose.ui.res.stringResource
import com.skyplayer.pro.R

/**
 * Écran Live TV (Style Hot Player)
 * Sidebar à gauche, Grille de chaînes à droite
 */
@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LiveTVScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMultiView: (String) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToEpgGuide: () -> Unit = {},
    onNavigateToAddPlaylist: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    viewModel: OrganizedContentViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    BackHandler { onBackToHome() }
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    
    val categories by viewModel.liveCategories.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val channels by viewModel.liveChannels.collectAsState()
    val currentPrograms by viewModel.currentPrograms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<ChannelCategory?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var recoveryError by remember { mutableStateOf<String?>(null) }

    // État pour le split-screen preview
    var previewChannel by remember { mutableStateOf<Channel?>(null) }

    // Vérifier si le PIN est configuré au premier clic sur une catégorie sensible
    val onCategoryClick = { category: ChannelCategory ->
        if (parentalViewModel.manager.isSensitiveCategory(category.name)) {
            pendingCategory = category
            if (parentalViewModel.manager.isPinSet()) {
                showPinDialog = true
            } else {
                showSetupPinDialog = true
            }
        } else {
            viewModel.selectLiveCategory(category.name)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Navigation conditionnelle: Sidebar pour TV/Tablette, Onglets horizontaux pour Mobile
        if (isTV) {
            // Sidebar Latérale (Catégories) pour TV/Tablette
            CategorySidebar(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategoryClick,
                onSearchQueryChange = { viewModel.searchLive(it) }
            )
        }

        // Contenu Principal (Grille) — réduit quand preview ouvert
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(if (previewChannel != null) 0.55f else 1f)
        ) {
            // Onglets horizontaux pour mobile uniquement
            if (!isTV) {
                HorizontalCategoryTabs(
                    categories = categories.map { it.name },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        val categoryObj = categories.find { it.name == category }
                        categoryObj?.let { onCategoryClick(it) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            SectionTopBar(
                title = selectedCategory?.uppercase()
                    ?: stringResource(R.string.section_live_tv),
                subtitle = stringResource(R.string.section_channels_count, channels.size),
                accentColor = LiveTvColor,
                onNavigateHome = onBackToHome,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings,
                extraActions = {
                    IconButton(
                        onClick = onNavigateToEpgGuide,
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBlack, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onNavigateToMultiView("") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBlack, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.player_multi),
                            tint = PremiumEmerald
                        )
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        ChannelListShimmer()
                    }
                    categories.isEmpty() -> {
                        TrustStateView(
                            icon = Icons.Default.Tv,
                            title = stringResource(R.string.trust_empty_channels_title),
                            message = stringResource(R.string.trust_empty_channels_message),
                            primaryAction = TrustAction(
                                label = stringResource(R.string.trust_action_add_playlist),
                                onClick = onNavigateToAddPlaylist
                            ),
                            footer = { ChannelLoadMonitor() }
                        )
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
                        LazyColumn(
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = channels,
                                key = { it.id }
                            ) { channel ->
                                val program = currentPrograms[channel.epgId]
                                CompactChannelRow(
                                    channel = channel,
                                    currentProgram = program,
                                    isSelected = previewChannel?.id == channel.id,
                                    onClick = {
                                        if (previewChannel?.id == channel.id) {
                                            onChannelClick(channel)
                                        } else {
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
            onForgotPassword = {
                showPinDialog = false
                showRecoveryDialog = true
            },
            error = pinError
        )
    }

    if (showSetupPinDialog) {
        com.skyplayer.pro.ui.components.SetupPinDialog(
            onSetupComplete = { pin, question, answer ->
                parentalViewModel.manager.setupParentalControl(pin, question, answer)
                pendingCategory?.let { viewModel.selectLiveCategory(it.name) }
                showSetupPinDialog = false
            },
            onDismiss = { showSetupPinDialog = false }
        )
    }

    if (showRecoveryDialog) {
        val question = parentalViewModel.manager.getSecurityQuestion() ?: "Question non configurée"
        com.skyplayer.pro.ui.components.RecoveryPinDialog(
            question = question,
            onVerify = { answer ->
                if (parentalViewModel.manager.verifySecurityAnswer(answer)) {
                    showRecoveryDialog = false
                    showSetupPinDialog = true // Permettre de redéfinir le PIN
                    recoveryError = null
                } else {
                    recoveryError = "Réponse incorrecte"
                }
            },
            onDismiss = {
                showRecoveryDialog = false
                recoveryError = null
            },
            error = recoveryError
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CompactChannelRow(
    channel: Channel,
    currentProgram: com.skyplayer.pro.data.model.EpgProgram? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isSelected) PremiumEmerald else Color.Transparent
    val backgroundColor = if (isSelected) CardBlack.copy(alpha = 0.9f) else ElevatedBlack.copy(alpha = 0.4f)
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo compact (40x40) avec cache Coil
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .diskCacheKey(channel.logoUrl) // Cache Coil activé
                            .memoryCacheKey(channel.logoUrl)
                            .build(),
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nom et EPG
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) PremiumEmerald else Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (currentProgram != null) {
                    Text(
                        text = currentProgram.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Indicateur de progression EPG si présent
            if (currentProgram != null) {
                Box(modifier = Modifier.width(60.dp).padding(start = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { currentProgram.getProgress() },
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                        color = PremiumEmerald,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            if (channel.isFavorite) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
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
    val borderColor = if (isSelected) PremiumEmerald else Color.White.copy(alpha = 0.1f)
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
                            color = PremiumEmerald.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { currentProgram.getProgress() },
                        modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).clip(CircleShape),
                        color = PremiumEmerald,
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

/**
 * Panneau split-screen droite:
 * - Mini-lecteur ExoPlayer (lecture réelle de la chaîne)
 * - Clic sur la vignette → plein écran
 * - EPG des programmes à venir
 * - Bouton fermer (X) pour retourner à la grille seule
 */
@UnstableApi
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
                color = PremiumEmerald,
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
                                color = PremiumEmerald,
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
