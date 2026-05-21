package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.skyplayer.pro.data.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    val repository: EpgRepository
) : ViewModel()
