package com.skyplayer.pro.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A3: Picture-in-Picture (PiP) manager.
 *
 * Permet à l'utilisateur de continuer à regarder une chaîne
 * en naviguant dans l'app ou en passant à une autre app.
 *
 * Fonctionne sur :
 * - Android TV / Box (mode natif)
 * - Smartphones Android 8.0+ (API 26+)
 * - Tablettes Android 7.0+ (API 24+)
 */
@Singleton
class PipManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Vérifie si le PiP est supporté sur cet appareil.
     */
    fun isPipSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * Vérifie si le PiP est activé dans les paramètres système.
     */
    fun isPipEnabled(): Boolean {
        return context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
        )
    }

    /**
     * Crée les paramètres PiP pour le player.
     *
     * @param widthRatio Largeur de la fenêtre PiP (défaut: 16:9)
     * @param heightRatio Hauteur de la fenêtre PiP (défaut: 16:9)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createPipParams(
        widthRatio: Int = 16,
        heightRatio: Int = 9
    ): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(widthRatio, heightRatio))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Auto-enter PiP quand l'utilisateur quitte l'app
                    setAutoEnterEnabled(false) // Contrôlé manuellement
                    // Actions dans la notification PiP
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }

    /**
     * Calcule la meilleure taille pour la fenêtre PiP selon l'écran.
     */
    fun calculatePipSize(screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        val pipWidth = (screenWidth * 0.4).toInt().coerceIn(200, 400)
        val pipHeight = (pipWidth * 9.0 / 16.0).toInt()
        return pipWidth to pipHeight
    }

    /**
     * Gère le changement de configuration quand on entre/sort du PiP.
     */
    fun onPictureInPictureModeChanged(
        isInPipMode: Boolean,
        newConfig: Configuration?
    ): PipModeInfo {
        return PipModeInfo(
            isInPipMode = isInPipMode,
            isSmallScreen = newConfig?.screenLayout?.and(
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
            ) == android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL,
            screenWidth = newConfig?.screenWidthDp ?: 0,
            screenHeight = newConfig?.screenHeightDp ?: 0
        )
    }

    /**
     * Détermine si les contrôles UI doivent être masqués en mode PiP.
     */
    fun shouldHideControls(isInPipMode: Boolean): Boolean {
        return isInPipMode
    }

    /**
     * Détermine si l'audio doit continuer en arrière-plan.
     */
    fun shouldContinueAudio(isInPipMode: Boolean): Boolean {
        return true // Toujours continuer l'audio en PiP
    }
}

/**
 * Info sur l'état PiP.
 */
data class PipModeInfo(
    val isInPipMode: Boolean,
    val isSmallScreen: Boolean,
    val screenWidth: Int,
    val screenHeight: Int
)
