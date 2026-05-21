package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.skyplayer.pro.data.security.ParentalControlManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel simple pour exposer le ParentalControlManager
 */
@HiltViewModel
class ParentalViewModel @Inject constructor(
    val manager: ParentalControlManager
) : ViewModel()
