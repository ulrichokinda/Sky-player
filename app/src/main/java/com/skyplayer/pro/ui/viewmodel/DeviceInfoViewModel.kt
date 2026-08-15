package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.license.LicenseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel simple pour exposer les informations de l'appareil (DeviceId/MAC)
 */
@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    private val licenseManager: LicenseManager
) : ViewModel() {

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    init {
        viewModelScope.launch {
            _deviceId.value = licenseManager.getDeviceId()
        }
    }
}
