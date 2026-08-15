package com.skyplayer.pro

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Génère le baseline profile de l'app pour accélérer le démarrage.
 *
 * Exécution (nécessite un appareil ou émulateur connecté, Android 7+) :
 *   ./gradlew :app:generateBaselineProfile
 *
 * Le profil généré est écrit dans `src/main/baselineProfiles/baseline-prof.txt`
 * et embarqué dans l'APK de release via `androidx.profileinstaller`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.skyplayer.pro",
            profileBlock = {
                // Démarrage + écran d'accueil : capture les classes chargées au lancement
                startActivityAndWait()
                device.waitForIdle()
            }
        )
    }
}
