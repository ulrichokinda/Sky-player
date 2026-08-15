package com.skyplayer.pro.ui.screens.welcome

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.local.PlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlaylistState {
    LOADING,
    LOADED,
    EMPTY
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val encryptedPrefs: EncryptedPrefs
) : ViewModel() {

    private val _playlistStatus = mutableStateOf(PlaylistState.LOADING)
    val playlistStatus: State<PlaylistState> = _playlistStatus

    init {
        checkPlaylistStatus()
    }

    private fun checkPlaylistStatus() {
        viewModelScope.launch {
            try {
                val count = playlistDao.getPlaylistCount()
                _playlistStatus.value = if (count > 0) PlaylistState.LOADED else PlaylistState.EMPTY
            } catch (_: Exception) {
                _playlistStatus.value = PlaylistState.EMPTY
            }
        }
    }

    fun completeOnboarding() {
        encryptedPrefs.setOnboardingCompleted()
    }
}
