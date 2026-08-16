package com.skyplayer.pro.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composants Glassmorphism pour l'UI Premium
 *
 * Effet de verre dépoli avec :
 * - Blur background (API 31+)
 * - Gradient translucide
 * - Bordures subtiles
 * - Ombres douces
 */

/**
 * Conteneur Glassmorphism principal
 */
@Composable
fun GlassmorphismContainer(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.15f,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = GlassWhite,
    backgroundGradient: Brush? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                // Optimisation GPU
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = backgroundGradient ?: Brush.verticalGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = 0.2f),
                        GlassWhite.copy(alpha = 0.05f)
                    )
                )
            )
            .glassmorphismBorder(borderWidth, borderColor, cornerRadius)
            .blurIfSupported(blurRadius),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Carte Glassmorphism pour items de liste
 */
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = if (isFocused) {
        PremiumEmerald.copy(alpha = 0.8f)
    } else {
        GlassWhite.copy(alpha = 0.3f)
    }
    
    val backgroundAlpha = if (isFocused) 0.25f else 0.15f
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = backgroundAlpha),
                        GlassWhite.copy(alpha = backgroundAlpha * 0.5f)
                    )
                )
            )
            .glassmorphismBorder(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                cornerRadius = cornerRadius
            )
            .blurIfSupported(10.dp),
        content = content
    )
}

/**
 * Overlay Glassmorphism pour barres d'info
 */
@Composable
fun GlassmorphismOverlay(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 30.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PureBlack.copy(alpha = 0.7f),
                        PureBlack.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
            .blurIfSupported(blurRadius),
        content = content
    )
}

/**
 * Bouton Glassmorphism avec effet premium
 */
@Composable
fun GlassmorphismButton(
    modifier: Modifier = Modifier,
    isPressed: Boolean = false,
    accentColor: Color = PremiumEmerald,
    content: @Composable BoxScope.() -> Unit
) {
    val scale = if (isPressed) 0.95f else 1f
    val alpha = if (isPressed) 0.3f else 0.2f
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = alpha),
                        accentColor.copy(alpha = alpha * 0.5f)
                    )
                )
            )
            .glassmorphismBorder(1.dp, accentColor.copy(alpha = 0.5f), 12.dp)
            .blurIfSupported(8.dp),
        content = content
    )
}

/**
 * Extension pour ajouter une bordure glassmorphism
 */
private fun Modifier.glassmorphismBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
): Modifier = this.then(
    Modifier.drawBehind {
        val strokeWidthPx = width.toPx()
        val halfStroke = strokeWidthPx / 2
        
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(
                width = size.width - strokeWidthPx,
                height = size.height - strokeWidthPx
            ),
            topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                x = cornerRadius.toPx(),
                y = cornerRadius.toPx()
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
        )
    }
)

/**
 * Extension pour appliquer le blur si supporté (API 31+)
 */
private fun Modifier.blurIfSupported(radius: Dp): Modifier {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        this.blur(radius)
    } else {
        // Fallback pour anciennes versions : simuler avec transparence
        this.background(Color.Black.copy(alpha = 0.3f))
    }
}

/**
 * Variante Or Premium pour éléments VIP
 */
@Composable
fun PremiumGoldGlassmorphism(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GlassmorphismContainer(
        modifier = modifier,
        borderColor = PremiumGold.copy(alpha = 0.5f),
        backgroundGradient = Brush.linearGradient(
            colors = listOf(
                PremiumGold.copy(alpha = 0.15f),
                PremiumGold.copy(alpha = 0.05f)
            )
        ),
        content = content
    )
}
