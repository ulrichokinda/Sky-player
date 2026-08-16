package com.skyplayer.pro.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Composant Shimmer premium avec animation fluide
 * Utilise un gradient animé pour créer un effet de chargement professionnel
 */
@Composable
fun PremiumShimmerCard(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color(0xFF10B981).copy(alpha = 0.06f),
        Color(0xFF10B981).copy(alpha = 0.16f),
        Color(0xFF10B981).copy(alpha = 0.06f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
                delayMillis = 200
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to shimmerColors[0],
                        0.5f to shimmerColors[1],
                        1.0f to shimmerColors[2]
                    ),
                    startX = translateAnimation - 1000f,
                    endX = translateAnimation
                )
            )
    )
}

/**
 * Version avec ratio d'aspect spécifique pour les cartes de contenu
 */
@Composable
fun PremiumShimmerCard(
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color(0xFF10B981).copy(alpha = 0.06f),
        Color(0xFF10B981).copy(alpha = 0.16f),
        Color(0xFF10B981).copy(alpha = 0.06f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
                delayMillis = 200
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to shimmerColors[0],
                        0.5f to shimmerColors[1],
                        1.0f to shimmerColors[2]
                    ),
                    startX = translateAnimation - 1000f,
                    endX = translateAnimation
                )
            )
    )
}

/**
 * Version pour les tuiles carrées (dashboard, etc.)
 */
@Composable
fun PremiumShimmerTile(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.08f),
        Color.Gray.copy(alpha = 0.15f),
        Color.Gray.copy(alpha = 0.08f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
                delayMillis = 200
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to shimmerColors[0],
                        0.5f to shimmerColors[1],
                        1.0f to shimmerColors[2]
                    ),
                    startX = translateAnimation - 1000f,
                    endX = translateAnimation
                )
            )
    )
}

/**
 * Version pour les lignes de texte shimmer
 */
@Composable
fun PremiumShimmerLine(
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.08f),
        Color.Gray.copy(alpha = 0.15f),
        Color.Gray.copy(alpha = 0.08f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
                delayMillis = 200
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to shimmerColors[0],
                        0.5f to shimmerColors[1],
                        1.0f to shimmerColors[2]
                    ),
                    startX = translateAnimation - 1000f,
                    endX = translateAnimation
                )
            )
    )
}
