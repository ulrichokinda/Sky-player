package com.skyplayer.pro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
                    selectedLanguage = encryptedPrefs.getString("language", "fr") ?: "fr"
                )
            }
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
    fun clearCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isClearingCache = true) }
                
                withContext(Dispatchers.IO) {
                    // Vider le cache de l'application
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                    
                    // Vider la base de données des chaînes (mais pas les playlists)
                    database.channelDao().deleteAllChannels()
                }
                
                Timber.i("🗑️ Cache vidé")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur vidage cache")
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
                    // Tout supprimer
                    database.clearAllTables()
                    
                    // Vider le cache
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                    
                    // Vider les préférences (sauf licence)
                    // encryptedPrefs.clear() // Optionnel
                }
                
                Timber.w("⚠️ Toutes les données supprimées")
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur suppression données")
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
    val isClearingCache: Boolean = false
)

/**
 * Extensions pour convertir les valeurs
 */
fun String.toBufferLabel(): String {
    val seconds = this.toIntOrNull() ?: 60
    return when (seconds) {
        30 -> "30s (Réseau rapide)"
        60 -> "60s (Recommandé)"
        120 -> "120s (Réseau lent)"
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
