package com.skyplayer.pro.ui.components.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.components.ChannelLogoGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Carte de chaîne optimisée pour TV avec navigation D-Pad fluide
 *
 * Fonctionnalités :
 * - États visuels distincts pour focus/normal/sélectionné
 * - Animation fluide de transition
 * - Clic long pour favoris (haptic feedback)
 * - Bordure lumineuse et glow effect
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvChannelCard(
    channel: Channel,
    isFocused: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var showFavoriteIndicator by remember { mutableStateOf(false) }

    // Animations fluides
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isFocused -> 1.08f
            else -> 1f
        },
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused -> 16.dp
            else -> 4.dp
        },
        label = "elevation"
    )

    val borderWidth by animateDpAsState(
        targetValue = when {
            isFocused -> 3.dp
            else -> 0.dp
        },
        label = "borderWidth"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer
            else -> Color(0xFF2A2A2A)
        },
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> Color.White
        },
        label = "contentColor"
    )

    // Montrer brièvement l'indicateur favori au clic long
    LaunchedEffect(showFavoriteIndicator) {
        if (showFavoriteIndicator) {
            delay(1500)
            showFavoriteIndicator = false
        }
    }

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Black
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    showFavoriteIndicator = true
                    onLongClick()
                    // Reset pressed state after animation
                    scope.launch {
                        delay(150)
                        isPressed = false
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (borderWidth > 0.dp) {
            BorderStroke(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Contenu principal
            ChannelCardContent(
                channel = channel,
                isFocused = isFocused,
                contentColor = contentColor
            )

            // Indicateur de favori (visible au clic long ou si déjà favori)
            if (isFavorite || showFavoriteIndicator) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showFavoriteIndicator)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                Color.Transparent
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(50)
                            )
                            .padding(4.dp)
                    )
                }
            }

            // Overlay d'état "Ajouté aux favoris"
            if (showFavoriteIndicator && isFavorite) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Ajouté aux favoris",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelCardContent(
    channel: Channel,
    isFocused: Boolean,
    contentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        // Logo de la chaîne avec Coil et fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp)
        ) {
            ChannelLogoGrid(
                channel = channel,
                modifier = Modifier.fillMaxSize(),
                isFocused = isFocused
            )
        }

        // Bandeau avec le nom de la chaîne
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isFocused)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                    else
                        Color.Black.copy(alpha = 0.8f)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused) Color.Black else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Indicateur de groupe
        channel.groupTitle?.let { group ->
            if (group.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
