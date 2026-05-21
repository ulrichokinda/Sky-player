package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour les informations appareil
 * Fournit l'ID/MAC persistant pour affichage
 */
@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    private val licenseManager: LicenseManager
) : ViewModel() {

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    init {
        loadDeviceId()
    }

    private fun loadDeviceId() {
        viewModelScope.launch {
            try {
                val id = licenseManager.getDeviceId()
                _deviceId.value = id
                Timber.d("📱 Device ID chargé: $id")
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur chargement Device ID")
                _deviceId.value = ""
            }
        }
    }

    /**
     * Force le rafraîchissement de l'ID
     */
    fun refreshDeviceId() {
        loadDeviceId()
    }
}
