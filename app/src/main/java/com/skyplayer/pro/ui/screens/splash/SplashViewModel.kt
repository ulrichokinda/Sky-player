package com.skyplayer.pro.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour l'écran de splash minimaliste
 * Affiche le logo pendant 3 secondes puis navigue vers le Dashboard
 */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    /**
     * État de navigation - uniquement vers Dashboard
     */
    sealed class SplashNavigation {
        object ToDashboard : SplashNavigation()
    }

    private val _navigationEvent = MutableStateFlow<SplashNavigation?>(null)
    val navigationEvent: StateFlow<SplashNavigation?> = _navigationEvent.asStateFlow()

    // Temporisation de 3 secondes avant navigation automatique
    private val SPLASH_DELAY_MS = 3000L // 3 secondes

    init {
        viewModelScope.launch {
            Timber.i("🚀 SplashScreen - Démarrage temporisation 3 secondes")
            
            // Attendre 3 secondes
            delay(SPLASH_DELAY_MS)
            
            // Naviguer vers Dashboard
            _navigationEvent.value = SplashNavigation.ToDashboard
            Timber.i("➡️ Navigation vers Dashboard")
        }
    }

    /**
     * Consomme l'événement de navigation (évite la double navigation)
     */
    fun onNavigationConsumed() {
        _navigationEvent.value = null
    }
}
