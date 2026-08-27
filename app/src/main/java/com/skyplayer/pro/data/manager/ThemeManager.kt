package com.skyplayer.pro.data.manager

import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holder reactif pour le theme de l'application.
 *
 * Resout le probleme MainActivity <-> SettingsViewModel :
 * - MainActivity lit themeMode au demarrage et s'abonne aux changements
 * - SettingsViewModel appelle setThemeMode() quand l'utilisateur change le theme
 * - Le StateFlow notifie MainActivity qui recompose avec le nouveau theme
 *
 * Stockage persistant via EncryptedPrefs (survit aux redemarrages).
 */
@Singleton
class ThemeManager @Inject constructor(
    private val encryptedPrefs: EncryptedPrefs
) {
    private val _themeMode = MutableStateFlow(
        encryptedPrefs.getString("theme_mode", "dark") ?: "dark"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        encryptedPrefs.saveString("theme_mode", mode)
        _themeMode.value = mode
    }
}
