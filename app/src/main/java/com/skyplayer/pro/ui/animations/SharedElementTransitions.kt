package com.skyplayer.pro.ui.animations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Animations Shared Element Transitions Premium
 *
 * Transitions fluides entre :
 * - Liste des chaînes ↔ Lecteur plein écran
 * - Navigation D-Pad
 * - Changement de catégories
 *
 * Durée : 300-400ms pour premium feel
 * Easing : FastOutSlowIn pour natural motion
 */

/**
 * Animation de transition Liste → Player (zoom + fade)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> ChannelToPlayerTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            // Zoom in depuis la carte vers plein écran
            scaleIn(
                initialScale = 0.85f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(300)
            ) togetherWith
            scaleOut(
                targetScale = 1.15f,
                animationSpec = tween(300)
            ) + fadeOut(
                animationSpec = tween(250)
            )
        },
        content = content
    )
}

/**
 * Transition horizontale pour navigation D-Pad
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> HorizontalZappingTransition(
    targetState: T,
    direction: ZappingDirection,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val slideDirection = when (direction) {
                ZappingDirection.NEXT -> 1  // Vers la droite
                ZappingDirection.PREVIOUS -> -1  // Vers la gauche
            }
            
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth * slideDirection },
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(animationSpec = tween(200)) togetherWith
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth * slideDirection },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(200))
        },
        label = "zapping_transition",
        content = content
    )
}

/**
 * Transition verticale pour le zapping (Up/Down)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> VerticalZappingTransition(
    targetState: T,
    direction: ZappingDirection,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val slideDirection = when (direction) {
                ZappingDirection.NEXT -> 1  // Vers le bas
                ZappingDirection.PREVIOUS -> -1  // Vers le haut
            }
            
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight * slideDirection },
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(animationSpec = tween(200)) togetherWith
            slideOutVertically(
                targetOffsetY = { fullHeight -> -fullHeight * slideDirection },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(200))
        },
        label = "vertical_zapping_transition",
        content = content
    )
}

/**
 * Transition de fade subtil pour éléments UI
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> FadeScaleTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200)) togetherWith
            scaleOut(
                targetScale = 1.05f,
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(150))
        },
        content = content
    )
}

/**
 * Animation de morphing pour changement de taille
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> MorphingTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        contentAlignment = contentAlignment,
        transitionSpec = {
            // Crossfade avec resize smooth
            fadeIn(animationSpec = tween(300)) togetherWith
            fadeOut(animationSpec = tween(200)) using
            SizeTransform { initialSize, targetSize ->
                tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            }
        },
        content = content
    )
}

/**
 * Directions de zapping
 */
enum class ZappingDirection {
    NEXT,      // Chaîne suivante (droite)
    PREVIOUS // Chaîne précédente (gauche)
}

enum class VerticalDirection {
    UP,    // Haut
    DOWN   // Bas
}

/**
 * Extension pour animation de focus TV
 */
fun Modifier.focusAnimation(
    isFocused: Boolean,
    scale: Float = 1.05f
): Modifier {
    return if (isFocused) {
        this.then(
            graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 8f
            }
        )
    } else {
        this.then(
            graphicsLayer {
                scaleX = 1f
                scaleY = 1f
                shadowElevation = 0f
            }
        )
    }
}
