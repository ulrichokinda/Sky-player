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
import com.skyplayer.pro.ui.navigation.SkyPlayerNavHost
import com.skyplayer.pro.ui.theme.SkyPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité principale de l'application
 * Mode paysage immersif: barre d'état + navigation masquées, encoche supportée
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fix écran blanc TV: forcer fond noir AVANT toute inflation
        // Le système Android dessine windowBackground avant que Compose charge
        window.decorView.setBackgroundColor(Color.parseColor("#0F0F0F"))
        window.statusBarColor = Color.parseColor("#0F0F0F")
        window.navigationBarColor = Color.parseColor("#0F0F0F")

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Compose content prêt: libère le splashScreen dès que le premier frame est rendu
        var isContentReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        setContent {
            // Marquer le content comme prêt au premier frame — élimine l'écran blanc TV
            isContentReady = true

            SkyPlayerProTheme {
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
