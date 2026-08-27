package com.skyplayer.pro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.manager.ThemeManager
import com.skyplayer.pro.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour gérer les paramètres de l'application
 * Gère toutes les préférences utilisateur via DataStore/EncryptedPrefs
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: EncryptedPrefs,
    private val playlistRepository: PlaylistRepository,
    private val themeManager: ThemeManager,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    autoUpdate = encryptedPrefs.getBoolean("auto_update", true),
                    parentalCode = encryptedPrefs.getString("parental_code", "") ?: "",
                    bufferSize = encryptedPrefs.getInt("buffer_size", 60),
                    autoReconnect = encryptedPrefs.getBoolean("auto_reconnect", true),
                    selectedPlayer = encryptedPrefs.getString("selected_player", "exo") ?: "exo",
                    aspectRatio = encryptedPrefs.getString("aspect_ratio", "16:9") ?: "16:9",
                    selectedLanguage = encryptedPrefs.getString("language", "fr") ?: "fr",
                    themeMode = encryptedPrefs.getString("theme_mode", "dark") ?: "dark"
                )
            }
        }
    }

    // ========== Thème ==========
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            encryptedPrefs.saveString("theme_mode", mode)
            _uiState.update { it.copy(themeMode = mode) }
            Timber.i("🎨 Thème: $mode")
        }
    }

    // ========== Sécurité ==========
    fun setParentalCode(code: String) {
        viewModelScope.launch {
            encryptedPrefs.saveString("parental_code", code)
            _uiState.update { it.copy(parentalCode = code) }
            Timber.i("🔒 Code parental mis à jour")
        }
    }

    fun setPin(pin: String) {
        setParentalCode(pin)
    }

    fun verifyParentalCode(code: String): Boolean {
        return _uiState.value.parentalCode == code
    }

    // ========== Performance ==========
    fun setBufferSize(seconds: Int) {
        viewModelScope.launch {
            encryptedPrefs.saveInt("buffer_size", seconds)
            _uiState.update { it.copy(bufferSize = seconds) }
            Timber.i("⏱️ Tampon mémoire: ${seconds}s")
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            encryptedPrefs.saveBoolean("auto_reconnect", enabled)
            _uiState.update { it.copy(autoReconnect = enabled) }
            Timber.i("🔄 Reconnexion auto: $enabled")
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            encryptedPrefs.saveBoolean("auto_update", enabled)
            _uiState.update { it.copy(autoUpdate = enabled) }
            Timber.i("📡 Mise à jour auto: $enabled")
        }
    }

    // ========== Lecteur ==========
    fun setPlayer(player: String) {
        viewModelScope.launch {
            encryptedPrefs.saveString("selected_player", player)
            _uiState.update { it.copy(selectedPlayer = player) }
            Timber.i("🎬 Lecteur: $player")
        }
    }

    fun setAspectRatio(ratio: String) {
        viewModelScope.launch {
            encryptedPrefs.saveString("aspect_ratio", ratio)
            _uiState.update { it.copy(aspectRatio = ratio) }
            Timber.i("📐 Format: $ratio")
        }
    }

    // ========== Langue ==========
    fun setLanguage(language: String) {
        viewModelScope.launch {
            encryptedPrefs.saveString("language", language)
            _uiState.update { it.copy(selectedLanguage = language) }
            Timber.i("🌍 Langue: $language")
        }
    }

    // ========== Données ==========
    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isClearingCache = true) }

                withContext(Dispatchers.IO) {
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()

                    val mediaCache = java.io.File(context.cacheDir, "media_cache")
                    if (mediaCache.exists()) {
                        mediaCache.deleteRecursively()
                    }

                    context.getDatabasePath("exoplayer_download.db")?.let { dbPath ->
                        if (dbPath.exists()) {
                            dbPath.delete()
                        }
                    }

                    context.filesDir.listFiles()
                        ?.filter { it.name.startsWith("cache") || it.name.endsWith(".tmp") }
                        ?.forEach { it.deleteRecursively() }

                    database.channelDao().deleteAllChannels()
                }

                Timber.i("🗑️ Cache vidé")
                _events.emit("Cache vidé avec succès")
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur vidage cache")
                _events.emit("Erreur lors du vidage du cache")
            } finally {
                _uiState.update { it.copy(isClearingCache = false) }
            }
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isClearingCache = true) }

                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                }

                Timber.w("⚠️ Toutes les données supprimées")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur suppression données")
                _events.emit("Erreur lors de la suppression des données")
            } finally {
                _uiState.update { it.copy(isClearingCache = false) }
            }
        }
    }

    // ========== Playlists ==========
    fun getPlaylistCount(callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = playlistRepository.getPlaylistCount()
            callback(count)
        }
    }

    fun refreshActivePlaylistEpg() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshingEpg = true) }
                val result = playlistRepository.refreshActivePlaylistEpg()
                result
                    .onSuccess { programCount ->
                        _events.emit("EPG mis à jour : ${programCount} programmes")
                    }
                    .onFailure { error ->
                        _events.emit(error.message ?: "Échec de la mise à jour EPG")
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ Rafraîchissement EPG échoué")
                _events.emit("Échec de la mise à jour EPG")
            } finally {
                _uiState.update { it.copy(isRefreshingEpg = false) }
            }
        }
    }
}

/**
 * État UI des paramètres
 */
data class SettingsUiState(
    val autoUpdate: Boolean = true,
    val parentalCode: String = "",
    val bufferSize: Int = 60,
    val autoReconnect: Boolean = true,
    val selectedPlayer: String = "exo",
    val aspectRatio: String = "16:9",
    val selectedLanguage: String = "fr",
    val themeMode: String = "dark",
    val isClearingCache: Boolean = false,
    val isRefreshingEpg: Boolean = false
)

/**
 * Extensions pour convertir les valeurs
 */
fun String.toBufferLabel(): String {
    val seconds = this.toIntOrNull() ?: 60
    return when {
        seconds <= 15 -> "${seconds}s (Très faible latence)"
        seconds <= 30 -> "${seconds}s (Réseau rapide)"
        seconds <= 60 -> "${seconds}s (Recommandé)"
        seconds <= 90 -> "${seconds}s (Réseau lent)"
        else -> "${seconds}s"
    }
}

fun String.toAspectRatioLabel(): String {
    return when (this) {
        "4:3" -> "4:3 (Standard)"
        "16:9" -> "16:9 (HD)"
        "21:9" -> "21:9 (Cinéma)"
        "auto" -> "Automatique"
        else -> this
    }
}

fun String.toPlayerLabel(): String {
    return when (this) {
        "exo" -> "ExoPlayer (Recommandé)"
        "vlc" -> "VLC Player"
        "ijk" -> "IJK Player"
        else -> this
    }
}
