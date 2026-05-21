package com.skyplayer.pro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs Premium pour Sky Player Pro
 * Design élégant: Noir pur, Bleu électrique, Or Premium
 * Optimisé pour écrans AMOLED avec glassmorphism
 */

// === COULEURS PRINCIPALES ===
val PureBlack = Color(0xFF0F0F0F)            // Fond principal #0F0F0F
val DeepBlack = Color(0xFF050505)            // Deep Black #050505 - UI Glassmorphism
val AbsoluteBlack = Color(0xFF000000)        // Noir absolu
val ElevatedBlack = Color(0xFF1A1A1A)        // Surface élevée
val CardBlack = Color(0xFF141414)            // Cartes noir profond

// === ACCENTS PREMIUM ===
val ElectricSkyBlue = Color(0xFF00AEEF)      // Bleu ciel électrique - Sélections
val ElectricSkyBlueDark = Color(0xFF0088CC)  // Bleu électrique foncé
val ElectricSkyBlueLight = Color(0xFF4DC9FF) // Bleu électrique clair

val PremiumGold = Color(0xFFFFD700)          // Or Premium #FFD700
val PremiumGoldDark = Color(0xFFB8860B)      // Or foncé
val PremiumGoldLight = Color(0xFFFFE55C)     // Or clair

// === GLASSMORPHISM ===
val GlassWhite = Color(0x1AFFFFFF)           // Blanc transparent (10%)
val GlassLight = Color(0x0DFFFFFF)           // Blanc très transparent (5%)
val GlassDark = Color(0x26000000)            // Noir transparent (15%)
val GlassOverlay = Color(0x33FFFFFF)         // Overlay blanc 20%
val BlurBackground = Color(0x800F0F0F)        // Fond flouté semi-transparent

// === SURFACES ET ÉLÉVATIONS ===
val SurfaceLevel0 = PureBlack              // Fond
val SurfaceLevel1 = CardBlack              // Cartes
val SurfaceLevel2 = ElevatedBlack          // Éléments surélevés
val SurfaceLevel3 = Color(0xFF222222)      // Haute élévation

// === TEXTES ===
val TextPrimary = Color(0xFFFFFFFF)            // Blanc pur
val TextSecondary = Color(0xB3FFFFFF)          // Blanc 70% opacité
val TextTertiary = Color(0x80FFFFFF)           // Blanc 50% opacité
val TextDisabled = Color(0x4DFFFFFF)           // Blanc 30% opacité
val TextOnGold = Color(0xFF0F0F0F)              // Noir sur or

// === ÉTATS ET FEEDBACKS ===
val SuccessGreen = Color(0xFF00E676)           // Vert néon
val ErrorRed = Color(0xFFFF3D71)                 // Rouge néon
val WarningOrange = Color(0xFFFFA726)          // Orange
val InfoBlue = ElectricSkyBlue                   // Info = Accent principal

// === GRADIENTS PREMIUM ===
val GradientElectricStart = Color(0xFF00AEEF)    // Bleu électrique
val GradientElectricEnd = Color(0xFF00D4FF)      // Cyan électrique
val GradientGoldStart = PremiumGold              // Or
val GradientGoldEnd = Color(0xFFFFE870)          // Or clair
val GradientDarkStart = PureBlack                // Noir
val GradientDarkEnd = Color(0xFF1A1A1A)            // Noir élevé

// === CATÉGORIES DE CONTENU ===
val LiveTvColor = Color(0xFFFF3D71)              // Rose néon pour Live
val VodColor = Color(0xFF00E676)                 // Vert néon pour VOD
val SeriesColor = Color(0xFF7C4DFF)              // Violet néon pour Séries
val FavoritesColor = PremiumGold                 // Or pour Favoris

// === UTILITAIRES ===
val DividerColor = Color(0x1FFFFFFF)             // Diviseur blanc 12%
val RippleColor = ElectricSkyBlue.copy(alpha = 0.3f)  // Ripple bleu
val SelectionBackground = ElectricSkyBlue.copy(alpha = 0.15f) // Fond sélection

// Legacy compatibility aliases
val SkyBlue = ElectricSkyBlue
val SkyBlueLight = ElectricSkyBlueLight
val SkyBlueDark = ElectricSkyBlueDark
val NightSky = PureBlack
val NightSurface = SurfaceLevel2
val NightCard = SurfaceLevel1
val SunGold = PremiumGold
val CloudWhite = TextPrimary
val TextTertiaryOld = TextTertiary
val GradientStart = GradientElectricStart
val GradientEnd = GradientElectricEnd
