package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.model.StreamingPreferences
import com.skyplayer.pro.data.model.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel pour gérer les préférences de streaming
 * Optimise la qualité vidéo selon la connexion
 */
@HiltViewModel
class StreamingPreferencesViewModel @Inject constructor(
    private val encryptedPrefs: EncryptedPrefs
) : ViewModel() {

    private val _preferences = MutableStateFlow(StreamingPreferences())
    val preferences: StateFlow<StreamingPreferences> = _preferences.asStateFlow()

    private val _currentNetworkSpeed = MutableStateFlow<Long>(0)
    val currentNetworkSpeed: StateFlow<Long> = _currentNetworkSpeed.asStateFlow()

    private val _estimatedQuality = MutableStateFlow<VideoQuality>(VideoQuality.AUTO)
    val estimatedQuality: StateFlow<VideoQuality> = _estimatedQuality.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val quality = VideoQuality.fromString(
            encryptedPrefs.getString("preferred_quality", VideoQuality.AUTO.name) ?: VideoQuality.AUTO.name
        )
        val autoAdjust = encryptedPrefs.getBoolean("auto_adjust_quality", true)
        val bufferDuration = encryptedPrefs.getInt("buffer_duration", 30)
        val lowLatency = encryptedPrefs.getBoolean("low_latency_mode", false)

        _preferences.value = StreamingPreferences(
            preferredQuality = quality,
            autoAdjustQuality = autoAdjust,
            bufferDurationSeconds = bufferDuration,
            lowLatencyMode = lowLatency
        )

        Timber.i("📊 Préférences streaming chargées: $quality (auto=$autoAdjust)")
    }

    /**
     * Définit la qualité vidéo préférée
     */
    fun setPreferredQuality(quality: VideoQuality) {
        viewModelScope.launch {
            encryptedPrefs.saveString("preferred_quality", quality.name)
            _preferences.value = _preferences.value.copy(preferredQuality = quality)
            Timber.i("🎬 Qualité définie: ${quality.label}")
        }
    }

    /**
     * Active/désactive l'ajustement automatique
     */
    fun setAutoAdjustQuality(enabled: Boolean) {
        viewModelScope.launch {
            encryptedPrefs.saveBoolean("auto_adjust_quality", enabled)
            _preferences.value = _preferences.value.copy(autoAdjustQuality = enabled)
            Timber.i("🔄 Ajustement auto: $enabled")
        }
    }

    /**
     * Définit la durée du tampon (buffer)
     */
    fun setBufferDuration(seconds: Int) {
        viewModelScope.launch {
            encryptedPrefs.saveInt("buffer_duration", seconds)
            _preferences.value = _preferences.value.copy(bufferDurationSeconds = seconds)
            Timber.i("⏱️ Tampon: ${seconds}s")
        }
    }

    /**
     * Active/désactive le mode faible latence
     */
    fun setLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch {
            encryptedPrefs.saveBoolean("low_latency_mode", enabled)
            _preferences.value = _preferences.value.copy(lowLatencyMode = enabled)
            Timber.i("⚡ Mode faible latence: $enabled")
        }
    }

    /**
     * Met à jour la vitesse de connexion estimée
     */
    fun updateNetworkSpeed(bandwidthKbps: Long) {
        _currentNetworkSpeed.value = bandwidthKbps
        _estimatedQuality.value = _preferences.value.getEffectiveQuality(bandwidthKbps)
    }

    /**
     * Récupère la qualité recommandée selon le réseau actuel
     */
    fun getRecommendedQuality(): VideoQuality {
        val speed = _currentNetworkSpeed.value
        return VideoQuality.fromBandwidth(speed)
    }

    /**
     * Mode économique de données (pour connexions limitées)
     */
    fun enableDataSaverMode() {
        viewModelScope.launch {
            setPreferredQuality(VideoQuality.MEDIUM)
            setAutoAdjustQuality(true)
            setBufferDuration(60) // Buffer plus grand pour éviter interruptions
            setLowLatencyMode(false)
            Timber.i("💾 Mode économie de données activé")
        }
    }

    /**
     * Mode performance maximale (pour connexions rapides)
     */
    fun enablePerformanceMode() {
        viewModelScope.launch {
            setPreferredQuality(VideoQuality.UHD)
            setAutoAdjustQuality(true)
            setBufferDuration(20)
            setLowLatencyMode(true)
            Timber.i("🚀 Mode performance maximale activé")
        }
    }
}
