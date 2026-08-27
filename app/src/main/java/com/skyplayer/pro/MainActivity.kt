package com.skyplayer.pro

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer
import com.skyplayer.pro.ui.navigation.SkyPlayerNavHost
import com.skyplayer.pro.ui.theme.SkyPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import android.app.PictureInPictureParams
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.manager.ThemeManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Activité principale de l'application
 * Mode paysage immersif : barre d'état + navigation masquées, encoche supportée
 * Amélioration PiP avec actions et transitions fluides
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var player: ExoPlayer
    
    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var encryptedPrefs: EncryptedPrefs

    @Inject
    lateinit var themeManager: ThemeManager

    // État de la PiP pour les composables
    var isPiPActive by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fix écran blanc TV: forcer fond noir AVANT toute inflation
        window.decorView.setBackgroundColor(Color.parseColor("#0F0F0F"))
        
        // Utiliser enableEdgeToEdge pour gérer les barres système de façon moderne
        enableEdgeToEdge()

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Compose content prêt
        var isContentReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        setContent {
            // Marquer le content comme prêt au premier frame — élimine l'écran blanc TV
            isContentReady = true

            // Écouter les changements de thème via le ThemeManager réactif
            val themeMode by themeManager.themeMode.collectAsStateWithLifecycle()

            SkyPlayerProTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SkyPlayerNavHost(navController = navController)
                }
            }
        }

        // Mode immersif après setContent (ordre correct sur TV)
        enableFullscreenImmersive()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableFullscreenImmersive()
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        this.isPiPActive = isInPictureInPictureMode
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePictureInPictureParams() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(16, 9))
            .setActions(createPiPActions())
            .build()
        setPictureInPictureParams(params)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPiPActions(): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        // Action Play/Pause
        val playPauseIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_PLAY_PAUSE"
        }
        val playPausePendingIntent = PendingIntent.getActivity(
            this,
            0,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIcon = Icon.createWithResource(this, android.R.drawable.ic_media_play)
        actions.add(
            RemoteAction(
                playPauseIcon,
                "Lecture/Pause",
                "Basculer lecture/pause",
                playPausePendingIntent
            )
        )

        return actions
    }

    private fun enableFullscreenImmersive() {
        // Support encoche (notch) — contenu s'étend derrière
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
