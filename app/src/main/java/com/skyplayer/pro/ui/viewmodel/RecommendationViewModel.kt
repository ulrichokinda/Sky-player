package com.skyplayer.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.Recommendation
import com.skyplayer.pro.data.repository.RecommendationEngine
import com.skyplayer.pro.data.repository.RecommendationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel pour les recommandations intelligentes
 *
 * Gère :
 * - Affichage des recommandations sur l'écran d'accueil
 * - Notifications proactives (30 min avant créneaux)
 * - Mise à jour en temps réel
 */
@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {

    val recommendations: StateFlow<List<Recommendation>> = recommendationEngine.recommendations
    val userHabits = recommendationEngine.userHabits

    // Recommandations filtrées pour l'écran d'accueil
    private val _homeRecommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val homeRecommendations: StateFlow<List<Recommendation>> = _homeRecommendations.asStateFlow()

    // Notification proactive active
    private val _proactiveNotification = MutableStateFlow<Recommendation?>(null)
    val proactiveNotification: StateFlow<Recommendation?> = _proactiveNotification.asStateFlow()

    // Canal actuellement recommandé (pour highlight)
    private val _highlightedChannel = MutableStateFlow<String?>(null)
    val highlightedChannel: StateFlow<String?> = _highlightedChannel.asStateFlow()

    private var monitoringJob: kotlinx.coroutines.Job? = null

    init {
        // Observer les changements de recommandations
        viewModelScope.launch {
            recommendations.collect { recs ->
                _homeRecommendations.value = recs.filter { 
                    it.type != RecommendationType.DISCOVERY 
                }.take(3) // Max 3 sur l'accueil
            }
        }

        // Démarrer surveillance pour notifications proactives
        startProactiveMonitoring()
    }

    /**
     * Analyse les habitudes et génère les recommandations
     */
    fun analyzeHabits(channels: List<Channel>) {
        viewModelScope.launch {
            try {
                // Analyse sur la liste complète des chaînes → hors du thread principal
                withContext(Dispatchers.Default) {
                    recommendationEngine.analyzeAndRecommend(channels)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur analyse habitudes")
            }
        }
    }

    /**
     * Surveillance pour notifications proactives
     */
    private fun startProactiveMonitoring() {
        monitoringJob?.cancel()
        
        monitoringJob = viewModelScope.launch {
            while (isActive) {
                checkProactiveNotifications()
                delay(60000) // Vérifier toutes les minutes
            }
        }
    }

    /**
     * Vérifie si une notification proactive doit être affichée
     */
    private fun checkProactiveNotifications() {
        val currentRecs = recommendations.value
        
        // Chercher une recommandation proactive valide
        val proactiveRec = currentRecs.find { rec ->
            rec.type == RecommendationType.PROACTIVE &&
            rec.triggerTime.isAfter(LocalDateTime.now().minusMinutes(5)) &&
            rec.triggerTime.isBefore(LocalDateTime.now().plusMinutes(5))
        }

        if (proactiveRec != null && _proactiveNotification.value?.id != proactiveRec.id) {
            _proactiveNotification.value = proactiveRec
            Timber.i("🔔 Notification proactive affichée: ${proactiveRec.title}")
            
            // Auto-dismiss après 2 minutes
            viewModelScope.launch {
                delay(120000)
                dismissProactiveNotification()
            }
        }
    }

    /**
     * Dismiss la notification proactive
     */
    fun dismissProactiveNotification() {
        _proactiveNotification.value = null
    }

    /**
     * Highlight une chaîne recommandée
     */
    fun highlightChannel(channelId: String) {
        _highlightedChannel.value = channelId
        
        // Retirer le highlight après 5 secondes
        viewModelScope.launch {
            delay(5000)
            if (_highlightedChannel.value == channelId) {
                _highlightedChannel.value = null
            }
        }
    }

    /**
     * Enregistre une session de visionnage
     */
    fun recordWatchSession(channel: Channel, durationMs: Long) {
        viewModelScope.launch {
            recommendationEngine.recordWatchSession(channel, durationMs)
        }
    }

    /**
     * Force une mise à jour des recommandations
     */
    fun refreshRecommendations(channels: List<Channel>) {
        viewModelScope.launch {
            analyzeHabits(channels)
        }
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
    }
}
