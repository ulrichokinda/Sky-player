package com.skyplayer.pro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs Premium pour Sky Player Pro
 * Design élégant : Noir profond, Émeraude, Teal, Cyan — identité alignée sur le site Sky-player
 * Optimisé pour écrans AMOLED avec glassmorphism
 */

// === COULEURS PRINCIPALES ===
val PureBlack = Color(0xFF0F0F0F)            // Fond principal #0F0F0F
val DeepBlack = Color(0xFF050505)            // Deep Black #050505 - UI Glassmorphism
val AbsoluteBlack = Color(0xFF000000)        // Noir absolu
val ElevatedBlack = Color(0xFF1A1A1A)        // Surface élevée
val CardBlack = Color(0xFF141414)            // Cartes noir profond

// === ACCENTS PREMIUM (identité émeraude/teal/cyan — comme le site) ===
val PremiumEmerald = Color(0xFF10B981)       // Émeraude - action principale
val PremiumEmeraldDark = Color(0xFF059669)   // Émeraude foncé - conteneur
val PremiumEmeraldLight = Color(0xFF6EE7B7)  // Émeraude clair
val PremiumTeal = Color(0xFF14B8A6)          // Teal - secondaire
val PremiumTealDark = Color(0xFF0F766E)      // Teal foncé
val PremiumCyan = Color(0xFF22D3EE)          // Cyan - accent vif

val PremiumGold = Color(0xFFFFD700)          // Or Premium #FFD700 - badges premium
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
val InfoBlue = PremiumEmerald                   // Info = Accent principal

// === GRADIENTS PREMIUM (émeraude → cyan) ===
val GradientElectricStart = PremiumEmerald      // Émeraude
val GradientElectricEnd = PremiumCyan           // Cyan
val GradientGoldStart = PremiumGold             // Or
val GradientGoldEnd = Color(0xFFFFE870)         // Or clair
val GradientDarkStart = PureBlack               // Noir
val GradientDarkEnd = Color(0xFF1A1A1A)         // Noir élevé

// === CATÉGORIES DE CONTENU ===
val LiveTvColor = PremiumCyan                   // Cyan pour Live
val VodColor = PremiumEmerald                   // Émeraude pour VOD
val SeriesColor = Color(0xFF8B5CF6)             // Violet pour Séries
val FavoritesColor = PremiumGold                // Or pour Favoris

// === UTILITAIRES ===
val DividerColor = Color(0x1FFFFFFF)             // Diviseur blanc 12%
val RippleColor = PremiumEmerald.copy(alpha = 0.3f)  // Ripple émeraude
val SelectionBackground = PremiumEmerald.copy(alpha = 0.15f) // Fond sélection

// Legacy compatibility aliases
val SkyBlue = PremiumEmerald
val SkyBlueLight = PremiumEmeraldLight
val SkyBlueDark = PremiumEmeraldDark
val NightSky = PureBlack
val NightSurface = SurfaceLevel2
val NightCard = SurfaceLevel1
val SunGold = PremiumGold
val CloudWhite = TextPrimary
val TextTertiaryOld = TextTertiary
val GradientStart = GradientElectricStart
val GradientEnd = GradientElectricEnd
