package com.skyplayer.pro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formes arrondies premium pour Sky Player Pro
 * Coins arrondis 16dp pour un look moderne et élégant
 */
val SkyPlayerShapes = Shapes(
    // Petits éléments: boutons, chips, badges
    small = RoundedCornerShape(8.dp),
    
    // Éléments moyens: cartes, dialogs, menus
    medium = RoundedCornerShape(16.dp),
    
    // Grands éléments: sheets, full-screen dialogs
    large = RoundedCornerShape(24.dp),
    
    // Extra large: navigation drawers, side sheets
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Formes spécifiques pour glassmorphism
 */
val GlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Rayons de coins spécifiques
 */
object CornerRadius {
    val xs = 4.dp    // Extra small: icônes, indicateurs
    val sm = 8.dp    // Small: boutons, chips
    val md = 12.dp   // Medium: input fields, small cards
    val lg = 16.dp   // Large: cards principales (requis design)
    val xl = 20.dp   // Extra large: modals, dialogs
    val xxl = 24.dp  // Extra extra large: bottom sheets
    val round = 50.dp // Full rounded: avatars, boutons circulaires
}
