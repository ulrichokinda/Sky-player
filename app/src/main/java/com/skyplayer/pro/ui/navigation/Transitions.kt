package com.skyplayer.pro.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/**
 * Animations de transition fluides pour Sky Player Pro
 * Transitions premium entre la liste des chaînes et le lecteur
 */

// Durée des animations (ms)
private const val TRANSITION_DURATION = 400
private const val FADE_DURATION = 300

/**
 * Transition slide horizontal avec fade
 * Utilisée pour la navigation standard entre écrans
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(FADE_DURATION)
    )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it / 2 },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = LinearOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(FADE_DURATION)
    )
}

/**
 * Transition vers le lecteur - zoom et fade
 * Crée un effet immersif lors de l'ouverture du player
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.enterToPlayer(): EnterTransition {
    return scaleIn(
        initialScale = 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(FADE_DURATION)
    )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.exitFromPlayer(): ExitTransition {
    return scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(TRANSITION_DURATION)
    ) + fadeOut(
        animationSpec = tween(FADE_DURATION)
    )
}

/**
 * Transition slide vertical - pour bottom sheets et dialogs
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromBottom(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(FADE_DURATION)
    )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToBottom(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = LinearOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(FADE_DURATION)
    )
}

/**
 * Transition fade simple - pour changements subtils
 */
fun fadeEnter(): EnterTransition {
    return fadeIn(
        animationSpec = tween(FADE_DURATION)
    )
}

fun fadeExit(): ExitTransition {
    return fadeOut(
        animationSpec = tween(FADE_DURATION)
    )
}

/**
 * Transition shared element - pour les éléments partagés
 * Effet de zoom élégant pour les cartes de chaînes vers le player
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedElementEnter(): EnterTransition {
    return scaleIn(
        initialScale = 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(
        animationSpec = tween(FADE_DURATION)
    )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedElementExit(): ExitTransition {
    return scaleOut(
        targetScale = 1.1f,
        animationSpec = tween(TRANSITION_DURATION)
    ) + fadeOut(
        animationSpec = tween(FADE_DURATION / 2)
    )
}
