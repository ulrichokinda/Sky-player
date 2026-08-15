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
 * Fond #0F0F0F (Noir pur), Accents #00AEEF (Bleu électrique), #FFD700 (Or Premium)
 * Glassmorphism et design moderne élégant
 */

/**
 * Thème clair Premium
 */
private val LightColorScheme = lightColorScheme(
    primary = ElectricSkyBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricSkyBlue.copy(alpha = 0.15f),
    onPrimaryContainer = ElectricSkyBlue,
    secondary = PremiumGold,
    onSecondary = PureBlack,
    secondaryContainer = PremiumGold.copy(alpha = 0.15f),
    onSecondaryContainer = PremiumGold,
    tertiary = PremiumGoldLight,
    onTertiary = PureBlack,
    tertiaryContainer = PremiumGoldLight.copy(alpha = 0.15f),
    onTertiaryContainer = PremiumGold,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = CardBlack,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedBlack,
    onSurfaceVariant = TextSecondary,
    surfaceTint = ElectricSkyBlue,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,
    outlineVariant = GlassWhite,
    scrim = PureBlack.copy(alpha = 0.85f),
    inverseSurface = TextPrimary,
    inverseOnSurface = PureBlack,
    inversePrimary = ElectricSkyBlueLight
)

/**
 * Thème sombre Premium - Mode par défaut
 * Optimisé pour écrans AMOLED avec glassmorphism
 */
private val DarkColorScheme = darkColorScheme(
    primary = ElectricSkyBlue,                    // Bleu électrique #00AEEF
    onPrimary = Color.White,
    primaryContainer = ElectricSkyBlueDark,       // Conteneur bleu foncé
    onPrimaryContainer = Color.White,
    secondary = PremiumGold,                        // Or Premium #FFD700
    onSecondary = PureBlack,                         // Noir sur or
    secondaryContainer = PremiumGold.copy(alpha = 0.12f), // Or transparent
    onSecondaryContainer = PremiumGold,
    tertiary = ElectricSkyBlueLight,               // Bleu clair
    onTertiary = PureBlack,
    tertiaryContainer = ElectricSkyBlueLight.copy(alpha = 0.15f),
    onTertiaryContainer = ElectricSkyBlue,
    background = PureBlack,                        // Fond #0F0F0F
    onBackground = TextPrimary,                    // Blanc sur fond
    surface = CardBlack,                           // Cartes #141414
    onSurface = TextPrimary,
    surfaceVariant = ElevatedBlack,                // Élévation #1A1A1A
    onSurfaceVariant = TextSecondary,              // Blanc 70%
    surfaceTint = ElectricSkyBlue,
    error = ErrorRed,                              // Rouge néon
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,                        // Diviseur subtil
    outlineVariant = GlassWhite,                   // Glassmorphism
    scrim = PureBlack.copy(alpha = 0.85f),         // Overlay foncé
    inverseSurface = TextPrimary,
    inverseOnSurface = PureBlack,
    inversePrimary = ElectricSkyBlueLight
)

/**
 * Thème AMOLED Premium - Noir absolu (#000000)
 * Économiseur de batterie pour écrans OLED
 */
private val AmoledColorScheme = darkColorScheme(
    primary = ElectricSkyBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricSkyBlueDark,
    onPrimaryContainer = Color.White,
    secondary = PremiumGold,
    onSecondary = AbsoluteBlack,
    secondaryContainer = PremiumGold.copy(alpha = 0.12f),
    onSecondaryContainer = PremiumGold,
    tertiary = ElectricSkyBlueLight,
    onTertiary = AbsoluteBlack,
    tertiaryContainer = ElectricSkyBlueLight.copy(alpha = 0.15f),
    onTertiaryContainer = ElectricSkyBlue,
    background = AbsoluteBlack,                   // Noir absolu
    onBackground = TextPrimary,
    surface = AbsoluteBlack,
    onSurface = TextPrimary,
    surfaceVariant = CardBlack,
    onSurfaceVariant = TextSecondary,
    surfaceTint = ElectricSkyBlue,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerColor,
    outlineVariant = GlassWhite,
    scrim = AbsoluteBlack.copy(alpha = 0.85f),
    inverseSurface = TextPrimary,
    inverseOnSurface = AbsoluteBlack,
    inversePrimary = ElectricSkyBlueLight
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
