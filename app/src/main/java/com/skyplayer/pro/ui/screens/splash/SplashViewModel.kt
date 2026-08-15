package com.skyplayer.pro.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.local.PlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'écran de splash.
 * Affiche le logo puis route vers Welcome (première utilisation) ou Dashboard.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val encryptedPrefs: EncryptedPrefs
) : ViewModel() {

    sealed class SplashNavigation {
        data object ToDashboard : SplashNavigation()
        data object ToWelcome : SplashNavigation()
    }

    private val _navigationEvent = MutableStateFlow<SplashNavigation?>(null)
    val navigationEvent: StateFlow<SplashNavigation?> = _navigationEvent.asStateFlow()

    private val splashDelayMs = 2500L

    init {
        viewModelScope.launch {
            Timber.i("SplashScreen - Démarrage temporisation")
            delay(splashDelayMs)

            val destination = resolveDestination()
            _navigationEvent.value = destination
            Timber.i("Navigation splash → %s", destination::class.simpleName)
        }
    }

    private suspend fun resolveDestination(): SplashNavigation {
        if (encryptedPrefs.isOnboardingCompleted()) {
            return SplashNavigation.ToDashboard
        }
        val playlistCount = try {
            playlistDao.getPlaylistCount()
        } catch (_: Exception) {
            0
        }
        return if (playlistCount > 0) {
            encryptedPrefs.setOnboardingCompleted()
            SplashNavigation.ToDashboard
        } else {
            SplashNavigation.ToWelcome
        }
    }

    fun onNavigationConsumed() {
        _navigationEvent.value = null
    }
}
