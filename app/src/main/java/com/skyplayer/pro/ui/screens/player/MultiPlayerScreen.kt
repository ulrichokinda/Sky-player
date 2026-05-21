package com.skyplayer.pro.ui.screens.player

import androidx.media3.common.util.UnstableApi
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skyplayer.pro.data.model.PlayerConnectionState
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Écran Multi-Player permettant de regarder 2 à 4 chaînes simultanément
 * Mode Picture-in-Picture avancé avec grille adaptable
 */
@UnstableApi
@Composable
fun MultiPlayerScreen(
    initialChannelId: String,
    onBackClick: () -> Unit,
    onAddChannel: () -> Unit, // Navigation vers sélection chaîne
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    
    // Configuration des joueurs multiples
    val playerSlots = remember { mutableStateListOf<PlayerSlot>() }
    var selectedSlot by remember { mutableIntStateOf(0) }
    var layoutMode by remember { mutableStateOf(LayoutMode.TWO_SPLIT) }
    var showLayoutSelector by remember { mutableStateOf(false) }
    
    // Init avec le premier canal
    LaunchedEffect(initialChannelId) {
        if (playerSlots.isEmpty()) {
            playerSlots.add(PlayerSlot(channelId = initialChannelId))
        }
    }
    
    // Plein écran forcé
    DisposableEffect(Unit) {
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            it.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        onDispose {
            activity?.let {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                it.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            // Libérer tous les players
            playerSlots.forEach { it.release() }
        }
    }
    
    BackHandler {
        onBackClick()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Grille de lecteurs
        when (layoutMode) {
            LayoutMode.TWO_SPLIT -> TwoPlayerLayout(
                slots = playerSlots,
                selectedSlot = selectedSlot,
                onSlotSelected = { selectedSlot = it },
                onRemoveSlot = { index ->
                    if (playerSlots.size > 1) {
                        playerSlots[index].release()
                        playerSlots.removeAt(index)
                        if (selectedSlot >= playerSlots.size) {
                            selectedSlot = playerSlots.size - 1
                        }
                    }
                }
            )
            LayoutMode.FOUR_GRID -> FourPlayerLayout(
                slots = playerSlots,
                selectedSlot = selectedSlot,
                onSlotSelected = { selectedSlot = it },
                onRemoveSlot = { index ->
                    if (playerSlots.size > 1) {
                        playerSlots[index].release()
                        playerSlots.removeAt(index)
                        if (selectedSlot >= playerSlots.size) {
                            selectedSlot = playerSlots.size - 1
                        }
                    }
                }
            )
            LayoutMode.PIP_MAIN -> PipMainLayout(
                slots = playerSlots,
                selectedSlot = selectedSlot,
                onSlotSelected = { selectedSlot = it },
                onRemoveSlot = { index ->
                    if (playerSlots.size > 1) {
                        playerSlots[index].release()
                        playerSlots.removeAt(index)
                    }
                }
            )
        }
        
        // Barre de contrôle supérieure
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
            
            // Sélecteur de layout
            Row {
                IconButton(
                    onClick = { showLayoutSelector = !showLayoutSelector },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Layout",
                        tint = Color.White
                    )
                }
                
                // Ajouter une chaîne
                if (playerSlots.size < 4) {
                    IconButton(
                        onClick = onAddChannel,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        // Sélecteur de layout
        if (showLayoutSelector) {
            LayoutSelector(
                currentMode = layoutMode,
                onLayoutSelected = { mode ->
                    layoutMode = mode
                    showLayoutSelector = false
                },
                onDismiss = { showLayoutSelector = false },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Info nombre de chaînes
        Text(
            text = "${playerSlots.size}/4 chaînes",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * Layout 2 écrans séparés (horizontal)
 */
@Composable
private fun TwoPlayerLayout(
    slots: List<PlayerSlot>,
    selectedSlot: Int,
    onSlotSelected: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Slot 0 (principal)
        PlayerSlotView(
            slot = slots.getOrNull(0),
            isSelected = selectedSlot == 0,
            isEmpty = slots.isEmpty(),
            onClick = { onSlotSelected(0) },
            onRemove = { slots.getOrNull(0)?.let { onRemoveSlot(0) } },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
        )
        
        // Slot 1
        PlayerSlotView(
            slot = slots.getOrNull(1),
            isSelected = selectedSlot == 1,
            isEmpty = slots.size < 2,
            onClick = { onSlotSelected(1) },
            onRemove = { slots.getOrNull(1)?.let { onRemoveSlot(1) } },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
        )
    }
}

/**
 * Layout 4 écrans en grille
 */
@Composable
private fun FourPlayerLayout(
    slots: List<PlayerSlot>,
    selectedSlot: Int,
    onSlotSelected: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Rangée 1
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerSlotView(
                slot = slots.getOrNull(0),
                isSelected = selectedSlot == 0,
                isEmpty = slots.isEmpty(),
                onClick = { onSlotSelected(0) },
                onRemove = { slots.getOrNull(0)?.let { onRemoveSlot(0) } },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            )
            
            PlayerSlotView(
                slot = slots.getOrNull(1),
                isSelected = selectedSlot == 1,
                isEmpty = slots.size < 2,
                onClick = { onSlotSelected(1) },
                onRemove = { slots.getOrNull(1)?.let { onRemoveSlot(1) } },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            )
        }
        
        // Rangée 2
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerSlotView(
                slot = slots.getOrNull(2),
                isSelected = selectedSlot == 2,
                isEmpty = slots.size < 3,
                onClick = { onSlotSelected(2) },
                onRemove = { slots.getOrNull(2)?.let { onRemoveSlot(2) } },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            )
            
            PlayerSlotView(
                slot = slots.getOrNull(3),
                isSelected = selectedSlot == 3,
                isEmpty = slots.size < 4,
                onClick = { onSlotSelected(3) },
                onRemove = { slots.getOrNull(3)?.let { onRemoveSlot(3) } },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Layout PiP - 1 principal + petits thumbnails
 */
@Composable
private fun PipMainLayout(
    slots: List<PlayerSlot>,
    selectedSlot: Int,
    onSlotSelected: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Player principal (slot sélectionné)
        val mainSlot = slots.getOrNull(selectedSlot)
        PlayerSlotView(
            slot = mainSlot,
            isSelected = true,
            isEmpty = slots.isEmpty(),
            onClick = { },
            onRemove = null,
            modifier = Modifier.fillMaxSize()
        )
        
        // Miniatures des autres chaînes (overlay)
        if (slots.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                slots.forEachIndexed { index, slot ->
                    if (index != selectedSlot) {
                        MiniPlayerView(
                            slot = slot,
                            onClick = { onSlotSelected(index) },
                            onRemove = { onRemoveSlot(index) },
                            modifier = Modifier
                                .size(160.dp, 90.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vue d'un slot de lecteur
 */
@Composable
private fun PlayerSlotView(
    slot: PlayerSlot?,
    isSelected: Boolean,
    isEmpty: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) ElectricSkyBlue else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = PureBlack
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isEmpty) {
                // Slot vide
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Touchez pour ajouter",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Lecteur vidéo
                slot?.exoPlayer?.let { player ->
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
                
                // Overlay état connexion
                slot?.let {
                    ConnectionOverlay(
                        state = it.connectionState,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            // Bouton fermer
            if (onRemove != null && !isEmpty) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Label chaîne
            val channelName = slot?.channelName
            if (!isEmpty && channelName != null) {
                Text(
                    text = channelName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Miniature pour mode PiP
 */
@Composable
private fun MiniPlayerView(
    slot: PlayerSlot,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            slot.exoPlayer?.let { player ->
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
            
            // Bouton fermer
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            
            // Nom chaîne
            Text(
                text = slot.channelName ?: "Chaîne",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * Overlay état connexion simplifié
 */
@Composable
private fun ConnectionOverlay(
    state: PlayerConnectionState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is PlayerConnectionState.Connecting,
        is PlayerConnectionState.Buffering -> {
            Box(
                modifier = modifier.background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ElectricSkyBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        is PlayerConnectionState.Reconnecting -> {
            Box(
                modifier = modifier.background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        is PlayerConnectionState.Error -> {
            Box(
                modifier = modifier.background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        else -> {}
    }
}

/**
 * Sélecteur de layout
 */
@Composable
private fun LayoutSelector(
    currentMode: LayoutMode,
    onLayoutSelected: (LayoutMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(top = 60.dp)
            .background(Color.Black.copy(alpha = 0.9f)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2 Split
            LayoutOption(
                icon = "2",
                label = "2 Écrans",
                isSelected = currentMode == LayoutMode.TWO_SPLIT,
                onClick = { onLayoutSelected(LayoutMode.TWO_SPLIT) }
            )
            
            // 4 Grid
            LayoutOption(
                icon = "4",
                label = "4 Grille",
                isSelected = currentMode == LayoutMode.FOUR_GRID,
                onClick = { onLayoutSelected(LayoutMode.FOUR_GRID) }
            )
            
            // PiP
            LayoutOption(
                icon = "PiP",
                label = "Principal",
                isSelected = currentMode == LayoutMode.PIP_MAIN,
                onClick = { onLayoutSelected(LayoutMode.PIP_MAIN) }
            )
        }
    }
}

@Composable
private fun LayoutOption(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (isSelected) ElectricSkyBlue else Color.Gray.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = if (isSelected) Color.Black else Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = label,
            color = if (isSelected) ElectricSkyBlue else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Modes de layout disponibles
 */
enum class LayoutMode {
    TWO_SPLIT,    // 2 écrans côte à côte
    FOUR_GRID,    // 4 écrans en grille
    PIP_MAIN      // 1 principal + miniatures
}

/**
 * Data class représentant un slot de lecteur
 */
class PlayerSlot(
    val channelId: String,
    var channelName: String? = null,
    var exoPlayer: ExoPlayer? = null,
    var connectionState: PlayerConnectionState = PlayerConnectionState.Idle
) {
    fun release() {
        exoPlayer?.let {
            it.stop()
            it.clearMediaItems()
            it.release()
        }
        exoPlayer = null
    }
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
