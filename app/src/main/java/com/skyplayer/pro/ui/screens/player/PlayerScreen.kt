package com.skyplayer.pro.ui.screens.player

import androidx.media3.common.util.UnstableApi
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skyplayer.pro.data.model.PlayerConnectionState
import com.skyplayer.pro.data.monitor.FallbackInfo
import com.skyplayer.pro.data.monitor.StreamHealth
import com.skyplayer.pro.data.monitor.StreamIssue
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GlassWhite
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.WarningOrange
import kotlinx.coroutines.delay

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.foundation.focusable
import android.view.KeyEvent

/**
 * Écran de lecture vidéo avancé avec ExoPlayer
 * Fonctionnalités : PiP, vitesse ajustable, pistes audio, sous-titres
 * Qualité adaptative : SD vers 4K avec indicateur en overlay
 * Gestion manuelle rapide + dialogue complet
 */
@SuppressLint("SourceLockedOrientationActivity")
@UnstableApi
@Composable
fun PlayerScreen(
    channelId: String,
    onBackClick: () -> Unit,
    onNavigateToMultiView: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = PlayerConnectionState.Idle)
    val exoPlayer by viewModel.exoPlayer.collectAsStateWithLifecycle(initialValue = null)
    val bufferState by viewModel.bufferState.collectAsStateWithLifecycle(initialValue = PlayerViewModel.BufferState())
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle(initialValue = null)

    // Failover intelligent
    val healthState by viewModel.healthState.collectAsStateWithLifecycle(initialValue = StreamHealth.Healthy)
    val fallbackInfo by viewModel.fallbackInfo.collectAsStateWithLifecycle(initialValue = null)

    // Qualité adaptative - collecte des états
    val currentQuality by viewModel.abrManager.currentQuality.collectAsStateWithLifecycle(initialValue = AdaptiveBitrateManager.VideoQuality.AUTO)
    val networkStability by viewModel.abrManager.networkStability.collectAsStateWithLifecycle(initialValue = AdaptiveBitrateManager.NetworkStability.UNKNOWN)
    val availableQualities by viewModel.abrManager.availableQualities.collectAsStateWithLifecycle(initialValue = emptyList())
    val bandwidthEstimate by viewModel.abrManager.bandwidthEstimate.collectAsStateWithLifecycle(initialValue = 0L)
    val isDataSaverEnabled by viewModel.isDataSaverEnabled.collectAsStateWithLifecycle(initialValue = false)
    val currentProgram by viewModel.currentProgram.collectAsStateWithLifecycle(initialValue = null)
    val safetyMessage by viewModel.safetyMessage.collectAsStateWithLifecycle(initialValue = null)

    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(true) }
    var isCinemaMode by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showQuickQualitySelector by remember { mutableStateOf(false) }
        var isInPipMode by remember { mutableStateOf(false) }
        var showHealthDashboard by remember { mutableStateOf(false) }
    
    // OSD Zapping
    var showOsd by remember { mutableStateOf(false) }
    
    LaunchedEffect(showOsd) {
        if (showOsd) {
            delay(3000)
            showOsd = false
        }
    }

    // Contrôles timeout (plus rapide en mode cinéma)
    LaunchedEffect(showControls, isCinemaMode) {
        if (showControls) {
            delay(if (isCinemaMode) 2000 else 5000)
            showControls = false
        }
    }

    // Configuration plein écran optimisée (API moderne)
    DisposableEffect(isFullscreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                // Cacher les barres système.
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                // Réafficher les barres système.
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }

        onDispose {
            activity?.let { act ->
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            if (!isInPipMode) {
                viewModel.releasePlayer()
            }
        }
    }

    // Gestion du retour arrière.
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBackClick()
        }
    }

    // Charger le canal
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.zapPrevious()
                            showOsd = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.zapNext()
                            showOsd = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            showControls = !showControls
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable(enabled = !isInPipMode) { showControls = !showControls }
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

        // Overlay de connexion et failover
        ConnectionStateOverlay(
            state = connectionState,
            bufferState = bufferState,
            healthState = healthState,
            fallbackInfo = fallbackInfo,
            modifier = Modifier.align(Alignment.Center)
        )

        // Message de sécurité (Safety Mode)
        AnimatedVisibility(
            visible = safetyMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            safetyMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, WarningOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Contrôles overlay
        AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControlsOverlay(
                isFullscreen = isFullscreen,
                isCinemaMode = isCinemaMode,
                onBackClick = {
                    if (isFullscreen) {
                        isFullscreen = false
                    } else {
                        onBackClick()
                    }
                },
                onToggleFullscreen = {
                    isFullscreen = !isFullscreen
                    activity?.let {
                        it.requestedOrientation = if (isFullscreen) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                },
                onToggleCinemaMode = { isCinemaMode = !isCinemaMode },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.let { enterPipMode(it) }
                        isInPipMode = true
                    }
                },
                onShowSpeedDialog = { showSpeedDialog = true },
                onShowAudioDialog = { showAudioTrackDialog = true },
                onShowSubtitleDialog = { showSubtitleDialog = true },
                onShowQualityDialog = { showQualityDialog = true },
                onQuickQualityClick = { showQuickQualitySelector = true },
                onMultiViewClick = onNavigateToMultiView,
                onShowHealthDashboard = { showHealthDashboard = true },
                onSeekBackward = { viewModel.seekBackward() },
                onSeekForward = { viewModel.seekForward() },
                onTogglePlay = { viewModel.togglePlayPause() },
                onToggleTurbo = { viewModel.toggleTurboMode() },
                isTurboActive = isDataSaverEnabled,
                isPlaying = viewModel.isPlaying,
                currentQuality = currentQuality,
                currentProgram = currentProgram,
                modifier = Modifier.fillMaxSize()
            )
        }

        // OSD Zapping
        AnimatedVisibility(
            visible = showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
        ) {
            PlayerOsd(
                channel = currentChannel,
                program = currentProgram
            )
        }

        // Indicateur de qualité (toujours visible, même sans contrôles sauf en mode cinéma)
        AnimatedVisibility(
            visible = !isCinemaMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            QualityIndicator(
                quality = currentQuality,
                networkStability = networkStability,
                bandwidthKbps = bandwidthEstimate / 1000,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
            )
        }

        // Quick Quality Selector (changement rapide de qualité)
        if (showQuickQualitySelector) {
            QuickQualitySelector(
                currentQuality = currentQuality,
                availableQualities = availableQualities,
                onQualitySelected = { quality ->
                    viewModel.setVideoQuality(quality)
                    showQuickQualitySelector = false
                },
                onDismiss = { showQuickQualitySelector = false }
            )
        }

        // Dialogues
        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = viewModel.playbackSpeed,
                onSpeedSelected = { viewModel.updatePlaybackSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showAudioTrackDialog) {
            AudioTrackDialog(
                exoPlayer = exoPlayer,
                onDismiss = { showAudioTrackDialog = false }
            )
        }

        if (showSubtitleDialog) {
            SubtitleDialog(
                exoPlayer = exoPlayer,
                onDismiss = { showSubtitleDialog = false }
            )
        }

        if (showQualityDialog) {
            QualitySelectionDialog(
                viewModel = viewModel,
                onDismiss = { showQualityDialog = false }
            )
        }

        // Stream Health Dashboard
        if (showHealthDashboard) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showHealthDashboard = false },
                contentAlignment = Alignment.Center
            ) {
                com.skyplayer.pro.ui.components.player.StreamHealthDashboard(
                    healthState = healthState,
                    currentQuality = currentQuality.name,
                    bandwidthEstimate = bandwidthEstimate,
                    bufferHealth = bufferState.bufferedPercentage / 100f,
                    onDismiss = { showHealthDashboard = false }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun enterPipMode(activity: Activity?) {
    try {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity?.enterPictureInPictureMode(params)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@UnstableApi
@Composable
private fun ConnectionStateOverlay(
    state: PlayerConnectionState,
    bufferState: PlayerViewModel.BufferState,
    healthState: StreamHealth,
    fallbackInfo: FallbackInfo?,
    modifier: Modifier = Modifier
) {
    // Message de santé prioritaire
    val healthMessage = when (healthState) {
        is StreamHealth.Healthy -> null
        is StreamHealth.Degraded -> when (healthState.issue) {
            StreamIssue.DeadLink -> "Lien mort détecté, recherche d'un miroir..."
            StreamIssue.BufferUnderrun -> "Connexion instable, optimisation..."
            is StreamIssue.PlayerError -> "Erreur de lecture, tentative de récupération..."
            StreamIssue.NetworkUnavailable -> "Aucune connexion réseau disponible"
        }
        is StreamHealth.UsingAlternative -> "Bascule sur une chaîne alternative..."
        is StreamHealth.Unrecoverable -> "Flux indisponible actuellement"
    }

    val showOverlay = state is PlayerConnectionState.Connecting ||
                      state is PlayerConnectionState.Buffering ||
                      state is PlayerConnectionState.Reconnecting ||
                      state is PlayerConnectionState.Error ||
                      healthMessage != null

    if (!showOverlay) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state !is PlayerConnectionState.Error && healthState !is StreamHealth.Unrecoverable) {
                CircularProgressIndicator(
                    color = ElectricSkyBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Message principal
            Text(
                text = healthMessage ?: when (state) {
                    is PlayerConnectionState.Connecting -> "Connexion au flux..."
                    is PlayerConnectionState.Buffering -> "Mise en mémoire tampon..."
                    is PlayerConnectionState.Reconnecting -> "Reconnexion réseau..."
                    is PlayerConnectionState.Error -> "Problème de lecture"
                    else -> ""
                },
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            // Détails secondaires
            val detailText = when {
                healthMessage != null && state is PlayerConnectionState.Buffering ->
                    "Buffer: ${bufferState.formatDuration()}"
                state is PlayerConnectionState.Buffering ->
                    "${bufferState.bufferedPercentage}% chargé (${bufferState.formatDuration()})"
                state is PlayerConnectionState.Error ->
                    "Échec après ${state.retryCount} tentatives"
                else -> null
            }

            if (detailText != null) {
                Text(
                    text = detailText,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Info sur le fallback en cours
            fallbackInfo?.let {
                val fallbackText = when (it) {
                    is FallbackInfo.Mirror -> "Essai du serveur miroir..."
                    is FallbackInfo.Alternative -> "Chargement de : ${it.channel.name}"
                    is FallbackInfo.NoneAvailable -> "Aucune source alternative trouvée"
                }
                Text(
                    text = fallbackText,
                    color = ElectricSkyBlue,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun PlayerControlsOverlay(
    isFullscreen: Boolean,
    isCinemaMode: Boolean,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleCinemaMode: () -> Unit,
    onEnterPip: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onShowAudioDialog: () -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowQualityDialog: () -> Unit,
    onQuickQualityClick: () -> Unit,
    onMultiViewClick: () -> Unit,
    onShowHealthDashboard: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleTurbo: () -> Unit,
    isTurboActive: Boolean,
    currentQuality: AdaptiveBitrateManager.VideoQuality,
    modifier: Modifier = Modifier,
    currentProgram: com.skyplayer.pro.data.model.EpgProgram? = null
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        // Overlay info programme (en bas à gauche)
        if (currentProgram != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 100.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "EN DIRECT",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentProgram.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Barre de progression du programme
                LinearProgressIndicator(
                    progress = { currentProgram.getProgress() },
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = ElectricSkyBlue,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LongClickIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Row {
                // Bouton Mode Cinéma
                LongClickIconButton(
                    onClick = onToggleCinemaMode,
                    modifier = Modifier.background(
                        if (isCinemaMode) PremiumGold.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = "Mode Cinéma",
                        tint = if (isCinemaMode) PremiumGold else Color.White
                    )
                }
                // Bouton Turbo (Data Saver)
                LongClickIconButton(
                    onClick = onToggleTurbo,
                    modifier = Modifier.background(
                        if (isTurboActive) WarningOrange.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Mode Turbo",
                        tint = if (isTurboActive) WarningOrange else Color.White
                    )
                }
                // Vitesse
                LongClickIconButton(onClick = onShowSpeedDialog) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Vitesse",
                        tint = Color.White
                    )
                }
                // Piste audio
                LongClickIconButton(onClick = onShowAudioDialog) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.White
                    )
                }
                // Sous-titres
                LongClickIconButton(onClick = onShowSubtitleDialog) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Sous-titres",
                        tint = Color.White
                    )
                }
                // PiP
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LongClickIconButton(onClick = onEnterPip) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "PiP",
                            tint = Color.White
                        )
                    }
                }
                // Santé du streaming
                LongClickIconButton(onClick = onShowHealthDashboard) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = "Santé du streaming",
                        tint = Color.White
                    )
                }
                // Multi-vue (2-4 chaînes)
                LongClickIconButton(onClick = onMultiViewClick) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Multi-vue",
                        tint = Color.White
                    )
                }
                // Qualité vidéo - clic court = quick select, clic long = dialog complet
                LongClickIconButton(
                    onClick = onQuickQualityClick,
                    onLongClick = onShowQualityDialog
                ) {
                    QualityBadge(
                        quality = currentQuality,
                        tint = Color.White
                    )
                }
                // Fullscreen
                LongClickIconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Plein écran",
                        tint = Color.White
                    )
                }
            }
        }

        // Centre - Contrôles lecture
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retour 10s
            FilledIconButton(
                onClick = onSeekBackward,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.FastRewind, null)
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Play/Pause
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Avance 10s
            FilledIconButton(
                onClick = onSeekForward,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.FastForward, null)
            }
        }
    }
}

/**
 * Dialogue pour sélectionner la vitesse de lecture
 */
@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vitesse de lecture") },
        text = {
            Column {
                speeds.forEach { speed ->
                    TextButton(
                        onClick = {
                            onSpeedSelected(speed)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${speed}x ${if (speed == currentSpeed) "✓" else ""}",
                            color = if (speed == currentSpeed) {
                                MaterialTheme.colorScheme.primary
                            } else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Dialogue pour sélectionner la piste audio
 */
@Composable
@UnstableApi
private fun AudioTrackDialog(
    exoPlayer: ExoPlayer?,
    onDismiss: () -> Unit
) {
    var selectedTrack by remember { mutableIntStateOf(0) }

    val tracks = remember(exoPlayer) {
        exoPlayer?.let { player ->
            val trackGroups = player.currentTracks.groups
            val audioTracks = mutableListOf<Pair<String, Int>>()

            trackGroups.forEachIndexed { _, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.language?.let {
                            "${it.uppercase()} - ${format.label ?: "Piste ${i+1}"}"
                        } ?: "Piste ${i+1}"
                        audioTracks.add(label to i)
                    }
                }
            }
            audioTracks.ifEmpty { listOf("Piste par défaut" to 0) }
        } ?: listOf("Aucune piste" to 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Piste audio") },
        text = {
            Column {
                tracks.forEachIndexed { index, (label, _) ->
                    TextButton(
                        onClick = {
                            exoPlayer?.let { player ->
                                val trackGroups = player.currentTracks.groups
                                trackGroups.forEachIndexed { _, group ->
                                    if (group.type == C.TRACK_TYPE_AUDIO) {
                                        val params = player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(
                                                TrackSelectionOverride(group.mediaTrackGroup, index)
                                            )
                                            .build()
                                        player.trackSelectionParameters = params
                                    }
                                }
                            }
                            selectedTrack = index
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$label ${if (index == selectedTrack) "✓" else ""}",
                            color = if (index == selectedTrack) {
                                MaterialTheme.colorScheme.primary
                            } else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Dialogue pour sélectionner les sous-titres
 */
@UnstableApi
@Composable
private fun SubtitleDialog(
    exoPlayer: ExoPlayer?,
    onDismiss: () -> Unit
) {
    var selectedSub by remember { mutableIntStateOf(-1) }

    val subtitles = remember(exoPlayer) {
        exoPlayer?.let { player ->
            val trackGroups = player.currentTracks.groups
            val textTracks = mutableListOf<Pair<String, Int>>()

            trackGroups.forEachIndexed { _, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.language?.let {
                            "${it.uppercase()} - ${format.label ?: "ST ${i+1}"}"
                        } ?: "Sous-titre ${i+1}"
                        textTracks.add(label to i)
                    }
                }
            }
            textTracks.ifEmpty { listOf("Aucun sous-titre" to -1) }
        } ?: listOf("Aucun sous-titre" to -1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sous-titres") },
        text = {
            Column {
                // Option désactiver
                TextButton(
                    onClick = {
                        exoPlayer?.let { player ->
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        }
                        selectedSub = -1
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Désactivé ${if (selectedSub == -1) "✓" else ""}",
                        color = if (selectedSub == -1) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }

                subtitles.forEachIndexed { index, (label, _) ->
                    TextButton(
                        onClick = {
                            exoPlayer?.let { player ->
                                val trackGroups = player.currentTracks.groups
                                trackGroups.forEachIndexed { _, group ->
                                    if (group.type == C.TRACK_TYPE_TEXT) {
                                        val params = player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(
                                                TrackSelectionOverride(group.mediaTrackGroup, index)
                                            )
                                            .build()
                                        player.trackSelectionParameters = params
                                    }
                                }
                            }
                            selectedSub = index
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$label ${if (index == selectedSub) "✓" else ""}",
                            color = if (index == selectedSub) {
                                MaterialTheme.colorScheme.primary
                            } else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

// Extension pour trouver l'Activity
private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Indicateur de qualité en overlay (toujours visible).
 * Affiche la qualité actuelle et l'état du réseau.
 */
@UnstableApi
@Composable
private fun QualityIndicator(
    quality: AdaptiveBitrateManager.VideoQuality,
    networkStability: AdaptiveBitrateManager.NetworkStability,
    bandwidthKbps: Long,
    modifier: Modifier = Modifier
) {
    val (indicatorColor, statusText) = when (networkStability) {
        AdaptiveBitrateManager.NetworkStability.EXCELLENT -> Color(0xFF00E676) to "HD"
        AdaptiveBitrateManager.NetworkStability.GOOD -> Color(0xFF00AEEF) to "HD"
        AdaptiveBitrateManager.NetworkStability.STABLE -> Color(0xFF7C4DFF) to "SD+"
        AdaptiveBitrateManager.NetworkStability.UNSTABLE -> Color(0xFFFFD700) to "SD"
        AdaptiveBitrateManager.NetworkStability.POOR -> Color(0xFFFF3D71) to "LOW"
        AdaptiveBitrateManager.NetworkStability.UNKNOWN -> Color.Gray to quality.label
    }

    // N'afficher que si on a une qualité définie (pas au démarrage)
    if (quality == AdaptiveBitrateManager.VideoQuality.AUTO && bandwidthKbps == 0L) {
        return
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = indicatorColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur LED réseau
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = indicatorColor,
                        shape = CircleShape
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Label qualité
            Text(
                text = if (quality == AdaptiveBitrateManager.VideoQuality.AUTO) {
                    "AUTO • $statusText"
                } else {
                    quality.label
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Badge de qualité pour le bouton des contrôles.
 * Affiche une abréviation colorée de la qualité.
 */
@UnstableApi
@Composable
private fun QualityBadge(
    quality: AdaptiveBitrateManager.VideoQuality,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val badgeText = when {
        quality == AdaptiveBitrateManager.VideoQuality.AUTO -> "AUTO"
        quality.height >= 2160 -> "4K"
        quality.height >= 1080 -> "FHD"
        quality.height >= 720 -> "HD"
        quality.height >= 480 -> "SD+"
        else -> "SD"
    }

    val badgeColor = when {
        quality == AdaptiveBitrateManager.VideoQuality.AUTO -> tint
        quality.height >= 2160 -> PremiumGold // Or
        quality.height >= 1080 -> Color(0xFF00E676) // Vert
        quality.height >= 720 -> ElectricSkyBlue  // Bleu
        else -> Color(0xFF9E9E9E) // Gris
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Icône de fond
        Icon(
            imageVector = Icons.Default.HighQuality,
            contentDescription = "Qualité: ${quality.label}",
            tint = tint,
            modifier = Modifier.size(24.dp)
        )

        // Badge texte
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            color = badgeColor,
            modifier = Modifier
                .padding(top = 10.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 2.dp, vertical = 0.dp)
        )
    }
}

/**
 * Quick Quality Selector — Popup rapide pour changer de qualité.
 * S'affiche en bas de l'écran comme une bottom sheet.
 */
@UnstableApi
@Composable
private fun QuickQualitySelector(
    currentQuality: AdaptiveBitrateManager.VideoQuality,
    availableQualities: List<AdaptiveBitrateManager.VideoQuality>,
    onQualitySelected: (AdaptiveBitrateManager.VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val autoMode = currentQuality == AdaptiveBitrateManager.VideoQuality.AUTO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = PureBlack.copy(alpha = 0.95f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = ElectricSkyBlue.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Qualité Rapide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Fermer", color = ElectricSkyBlue)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Auto Toggle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQualitySelected(AdaptiveBitrateManager.VideoQuality.AUTO) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (autoMode)
                            ElectricSkyBlue.copy(alpha = 0.2f)
                        else
                            GlassWhite.copy(alpha = 0.2f)
                    ),
                    border = if (autoMode)
                        androidx.compose.foundation.BorderStroke(2.dp, ElectricSkyBlue)
                    else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (autoMode) ElectricSkyBlue else Color.White,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatique",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (autoMode) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                            Text(
                                text = "Adapté à votre connexion",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        if (autoMode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sélectionné",
                                tint = ElectricSkyBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Qualités disponibles (grille)
                Text(
                    text = "Qualités disponibles",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Grille de qualités
                val manualQualities = availableQualities.filter { it != AdaptiveBitrateManager.VideoQuality.AUTO }

                manualQualities.chunked(3).forEach { rowQualities ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowQualities.forEach { quality ->
                            QualityQuickOption(
                                quality = quality,
                                isSelected = !autoMode && currentQuality == quality,
                                onClick = { onQualitySelected(quality) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Remplir avec des spacers si moins de 3 éléments
                        repeat(3 - rowQualities.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Option de qualité rapide (bouton compact).
 */
@UnstableApi
@Composable
private fun QualityQuickOption(
    quality: AdaptiveBitrateManager.VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val backgroundColor = when {
        isSelected -> when {
            quality.height >= 2160 -> PremiumGold.copy(alpha = 0.2f)
            quality.height >= 1080 -> Color(0xFF00E676).copy(alpha = 0.2f)
            quality.height >= 720 -> ElectricSkyBlue.copy(alpha = 0.2f)
            else -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
        }
        else -> GlassWhite.copy(alpha = 0.1f)
    }

    val borderColor = when {
        isSelected -> when {
            quality.height >= 2160 -> PremiumGold
            quality.height >= 1080 -> Color(0xFF00E676)
            quality.height >= 720 -> ElectricSkyBlue
            else -> Color(0xFF9E9E9E)
        }
        else -> Color.Transparent
    }

    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = quality.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = when {
                    quality.height >= 2160 -> "4K"
                    quality.height >= 1080 -> "1080p"
                    quality.height >= 720 -> "720p"
                    quality.height >= 480 -> "480p"
                    else -> "${quality.height}p"
                },
                style = MaterialTheme.typography.labelSmall,
                color = borderColor,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * IconButton avec support long clic.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LongClickIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PlayerOsd(
    channel: com.skyplayer.pro.data.model.Channel?,
    program: com.skyplayer.pro.data.model.EpgProgram?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(16.dp).width(400.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel?.logoUrl != null) {
                    coil.compose.AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = channel?.name ?: "Inconnu",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (program != null) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricSkyBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { program.getProgress() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = ElectricSkyBlue,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
