package com.skyplayer.pro.ui.screens.tv

import androidx.media3.common.util.UnstableApi
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.input.key.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.components.ChannelLogo
import com.skyplayer.pro.ui.screens.player.PlayerViewModel
import com.skyplayer.pro.ui.viewmodel.PrefetchPlayerViewModel
import com.skyplayer.pro.ui.theme.PremiumEmerald
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Écran de lecture TV optimisé pour télécommande
 *
 * Fonctionnalités :
 * - Zapping avec touches Haut/Bas du D-Pad
 * - Barre d'info temporaire lors du changement de chaîne
 * - Navigation fluide sans retour au menu
 * - Affichage du nom et logo de la chaîne
 */
@UnstableApi
@Composable
fun TvPlayerScreen(
    currentChannel: Channel,
    allChannels: List<Channel>,
    onChannelChange: (Channel) -> Unit,
    onBackToGuide: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    prefetchViewModel: PrefetchPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val haptic = LocalHapticFeedback.current
    
    val exoPlayer by playerViewModel.exoPlayer.collectAsStateWithLifecycle()
    val prefetchStats by prefetchViewModel.prefetchStats.collectAsStateWithLifecycle()
    val zappingTimes by prefetchViewModel.zappingTimes.collectAsStateWithLifecycle()
    
    // Configurer la liste des canaux pour le pré-chargement
    LaunchedEffect(allChannels) {
        prefetchViewModel.setChannelList(allChannels)
    }
    
    // État pour la barre d'info
    var showInfoBar by remember { mutableStateOf(true) }
    var currentChannelIndex by remember { mutableIntStateOf(allChannels.indexOf(currentChannel)) }
    var showSideSettings by remember { mutableStateOf(false) }
    
    // FocusRequester pour capturer les événements clavier
    val focusRequester = remember { FocusRequester() }
    
    // Configuration plein écran TV
    DisposableEffect(Unit) {
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            activity?.let {
                it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            playerViewModel.releasePlayer()
        }
    }
    
    // Charger la chaîne initiale avec pré-chargement
    LaunchedEffect(currentChannel.id) {
        val startTime = System.currentTimeMillis()
        playerViewModel.loadChannel(currentChannel.id)
        
        // Mettre à jour le pré-chargement pour les voisins
        prefetchViewModel.updatePrefetchPosition(currentChannel)
        
        // Afficher la barre d'info au démarrage
        showInfoBar = true
        delay(3000)
        showInfoBar = false
        
        // Log performance
        val loadTime = System.currentTimeMillis() - startTime
        Timber.d("⏱️ Chargement initial: ${loadTime}ms")
    }
    
    // Fonction pour changer de chaîne
    val changeChannel = { direction: Int ->
        val newIndex = (currentChannelIndex + direction).coerceIn(0, allChannels.size - 1)
        if (newIndex != currentChannelIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            currentChannelIndex = newIndex
            val newChannel = allChannels[newIndex]
            onChannelChange(newChannel)
            playerViewModel.loadChannel(newChannel.id)
            showInfoBar = true
            Timber.d("📺 Changement chaîne: ${newChannel.name} (index: $newIndex)")
        }
    }
    
    // Navigation D-Pad
    // Zapping par pavé numérique
    var numPadInput by remember { mutableStateOf("") }
    
    LaunchedEffect(numPadInput) {
        if (numPadInput.isNotEmpty()) {
            delay(2000) // Attendre 2 secondes après le dernier chiffre
            val channelNumber = numPadInput.toIntOrNull()
            if (channelNumber != null) {
                // Logique de zapping direct par numéro
                playerViewModel.zapToNumber(channelNumber)
            }
            numPadInput = ""
        }
    }

    val handleKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
        val isKeyUp = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
        val keyCode = keyEvent.nativeKeyEvent.keyCode
        
        when {
            keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                if (isKeyUp) changeChannel(-1)
                true
            }
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isKeyUp) changeChannel(1)
                true
            }
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> {
                if (isKeyUp) showInfoBar = !showInfoBar
                true
            }
            keyCode == KeyEvent.KEYCODE_BACK -> {
                if (isKeyUp) {
                    if (showInfoBar || showSideSettings) {
                        showInfoBar = false
                        showSideSettings = false
                    } else {
                        onBackToGuide()
                    }
                }
                true
            }
            keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9 -> {
                if (isKeyUp) {
                    val digit = (keyCode - KeyEvent.KEYCODE_0).toString()
                    numPadInput += digit
                    showInfoBar = true
                }
                true
            }
            keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS -> {
                if (isKeyUp) showSideSettings = !showSideSettings
                true
            }
            else -> false
        }
    }
    
    // Masquer la barre d'info après 3 secondes
    LaunchedEffect(showInfoBar) {
        if (showInfoBar) {
            delay(3000)
            showInfoBar = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { handleKeyEvent(it) }
    ) {
        // Lecteur vidéo
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Panel Latéral de Paramètres
        AnimatedVisibility(
            visible = showSideSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(300.dp)
        ) {
            PlayerSideSettings(
                onClose = { showSideSettings = false },
                onSwitchPlayer = { playerViewModel.togglePlayerEngine() }, // ExoPlayer <-> VLC
                onSelectAudio = { /* logic */ },
                onSelectSubtitle = { /* logic */ }
            )
        }
        
        // Overlay de chargement
        if (exoPlayer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        // Barre d'information temporaire (zapping info)
        AnimatedVisibility(
            visible = showInfoBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ChannelInfoBar(
                channel = allChannels.getOrNull(currentChannelIndex) ?: currentChannel,
                channelNumber = currentChannelIndex + 1,
                totalChannels = allChannels.size,
                onBackClick = onBackToGuide,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        // Indicateur de navigation (toujours visible en haut)
        if (!showInfoBar) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Changer chaîne",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
        
        // Indicateur de pré-chargement (debug/performance)
        if (!showInfoBar && prefetchStats != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "⚡ Pré-chargé: ${prefetchStats?.readyCount}/${prefetchStats?.totalPrefetched}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if ((prefetchStats?.readyCount ?: 0) > 0) 
                            Color.Green else Color.Yellow
                    )
                    val avgTime = if (zappingTimes.isNotEmpty()) {
                        zappingTimes.average().toLong()
                    } else 0L
                    if (avgTime > 0) {
                        Text(
                            text = "⏱️ Zapping: ${avgTime}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (avgTime < 500) Color.Green else Color.Yellow
                        )
                    }
                }
            }
        }
    }
    
    // Request focus pour capturer les événements clavier
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Barre d'information de la chaîne affichée lors du zapping
 */
@Composable
private fun ChannelInfoBar(
    channel: Channel,
    channelNumber: Int,
    totalChannels: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo avec Coil et fallback sur initiales
            ChannelLogo(
                channel = channel,
                size = 68,
                isFocused = true
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informations chaîne
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Numéro + Nom
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$channelNumber / $totalChannels",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Groupe et info supplémentaire
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    channel.groupTitle?.let { group ->
                        if (group.isNotBlank()) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    if (channel.isFavorite) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favori",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Bouton retour guide
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour au guide",
                    tint = Color.White
                )
            }
        }
    }
}


@Composable
private fun PlayerSideSettings(
    onClose: () -> Unit,
    onSwitchPlayer: () -> Unit,
    onSelectAudio: () -> Unit,
    onSelectSubtitle: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxHeight().width(300.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Paramètres", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            
            SettingsItem(label = "Pistes Audio", icon = Icons.Default.MusicNote, onClick = onSelectAudio)
            SettingsItem(label = "Sous-titres", icon = Icons.Default.Subtitles, onClick = onSelectSubtitle)
            SettingsItem(label = "Changer de Lecteur", icon = Icons.Default.RocketLaunch, onClick = onSwitchPlayer)
            
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Fermer")
            }
        }
    }
}

@Composable
private fun SettingsItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = Color.White)
        }
    }
}
private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
