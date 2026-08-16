package com.skyplayer.pro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Thème Premium Sky Player Pro
 * Fond #0F0F0F (Noir pur), Accents #10B981 (Émeraude), #14B8A6 (Teal), #22D3EE (Cyan)
 * Identité alignée sur le site Sky-player — Glassmorphism et design moderne élégant
 */

/**
 * Thème clair Premium
 */
private val LightColorScheme = lightColorScheme(
    primary = PremiumEmerald,
    onPrimary = Color.White,
    primaryContainer = PremiumEmerald.copy(alpha = 0.15f),
    onPrimaryContainer = PremiumEmerald,
    secondary = PremiumTeal,
    onSecondary = PureBlack,
    secondaryContainer = PremiumTeal.copy(alpha = 0.15f),
    onSecondaryContainer = PremiumTeal,
    tertiary = PremiumCyan,
    onTertiary = PureBlack,
    tertiaryContainer = PremiumCyan.copy(alpha = 0.15f),
    onTertiaryContainer = PremiumCyan,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = CardBlack,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedBlack,
    onSurfaceVariant = TextSecondary,
    surfaceTint = PremiumEmerald,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,
    outlineVariant = GlassWhite,
    scrim = PureBlack.copy(alpha = 0.85f),
    inverseSurface = TextPrimary,
    inverseOnSurface = PureBlack,
    inversePrimary = PremiumEmeraldLight
)

/**
 * Thème sombre Premium - Mode par défaut
 * Optimisé pour écrans AMOLED avec glassmorphism
 */
private val DarkColorScheme = darkColorScheme(
    primary = PremiumEmerald,                    // Émeraude #10B981
    onPrimary = Color.White,
    primaryContainer = PremiumEmeraldDark,       // Conteneur émeraude foncé
    onPrimaryContainer = Color.White,
    secondary = PremiumTeal,                       // Teal #14B8A6
    onSecondary = PureBlack,                       // Noir sur teal
    secondaryContainer = PremiumTeal.copy(alpha = 0.12f), // Teal transparent
    onSecondaryContainer = PremiumTeal,
    tertiary = PremiumCyan,                       // Cyan #22D3EE
    onTertiary = PureBlack,
    tertiaryContainer = PremiumCyan.copy(alpha = 0.15f),
    onTertiaryContainer = PremiumCyan,
    background = PureBlack,                        // Fond #0F0F0F
    onBackground = TextPrimary,                    // Blanc sur fond
    surface = CardBlack,                           // Cartes #141414
    onSurface = TextPrimary,
    surfaceVariant = ElevatedBlack,                // Élévation #1A1A1A
    onSurfaceVariant = TextSecondary,              // Blanc 70%
    surfaceTint = PremiumEmerald,
    error = ErrorRed,                              // Rouge néon
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,                        // Diviseur subtil
    outlineVariant = GlassWhite,                   // Glassmorphism
    scrim = PureBlack.copy(alpha = 0.85f),         // Overlay foncé
    inverseSurface = TextPrimary,
    inverseOnSurface = PureBlack,
    inversePrimary = PremiumEmeraldLight
)

/**
 * Thème AMOLED Premium - Noir absolu (#000000)
 * Économiseur de batterie pour écrans OLED
 */
private val AmoledColorScheme = darkColorScheme(
    primary = PremiumEmerald,
    onPrimary = Color.White,
    primaryContainer = PremiumEmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = PremiumTeal,
    onSecondary = AbsoluteBlack,
    secondaryContainer = PremiumTeal.copy(alpha = 0.12f),
    onSecondaryContainer = PremiumTeal,
    tertiary = PremiumCyan,
    onTertiary = AbsoluteBlack,
    tertiaryContainer = PremiumCyan.copy(alpha = 0.15f),
    onTertiaryContainer = PremiumCyan,
    background = AbsoluteBlack,                   // Noir absolu
    onBackground = TextPrimary,
    surface = AbsoluteBlack,
    onSurface = TextPrimary,
    surfaceVariant = CardBlack,
    onSurfaceVariant = TextSecondary,
    surfaceTint = PremiumEmerald,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,
    outlineVariant = GlassWhite,
    scrim = AbsoluteBlack.copy(alpha = 0.85f),
    inverseSurface = TextPrimary,
    inverseOnSurface = AbsoluteBlack,
    inversePrimary = PremiumEmeraldLight
)

/**
 * Theme principal Premium de Sky Player Pro
 * Options: System, Light, Dark, AMOLED
 * Status bar et navigation en pure black (#0F0F0F)
 */
@Composable
fun SkyPlayerProTheme(
    themeMode: String = "dark",                    // "system", "light", "dark", "amoled"
    dynamicColor: Boolean = false,                  // Couleurs de marque fixes
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val effectiveDarkTheme = when (themeMode) {
        "system" -> isSystemDark
        "light" -> false
        "dark" -> true
        "amoled" -> true
        else -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == "amoled" -> AmoledColorScheme
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false      // Icônes blanches
                isAppearanceLightNavigationBars = false // Navigation blanche
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SkyPlayerTypography,
        shapes = SkyPlayerShapes,
        content = content
    )
}
