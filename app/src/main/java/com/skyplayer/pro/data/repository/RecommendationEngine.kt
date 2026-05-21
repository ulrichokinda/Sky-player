package com.skyplayer.pro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skyplayer.pro.data.local.WatchHistoryDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.WatchHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Moteur de recommandation intelligent basé sur les habitudes utilisateur
 *
 * Analyse :
 * - Heures de visionnage préférées
 * - Catégories favorites
 * - Jours de la semaine (ex: sport le samedi soir)
 * - Durée de visionnage par chaîne
 *
 * Propose :
 * - Recommandations contextuelles (temps réel)
 * - Alertes proactives (30 min avant matchs)
 * - Mise en avant intelligente sur l'accueil
 */
@Singleton
class RecommendationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchHistoryDao: WatchHistoryDao,
    private val channelDao: com.skyplayer.pro.data.local.ChannelDao
) {
    companion object {
        private const val DATASTORE_NAME = "recommendations"
        private const val MIN_WATCH_TIME_MS = 5 * 60 * 1000 // 5 minutes minimum
        private const val PREDICTION_WINDOW_MINUTES = 30 // 30 min avant
        
        // Scores minimums
        private const val HIGH_CONFIDENCE = 0.75
        private const val MEDIUM_CONFIDENCE = 0.50
        private const val LOW_CONFIDENCE = 0.25
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

    // État des recommandations
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _userHabits = MutableStateFlow<UserHabits?>(null)
    val userHabits: StateFlow<UserHabits?> = _userHabits.asStateFlow()

    private var allChannels: List<Channel> = emptyList()

    /**
     * Analyse les habitudes utilisateur et génère des recommandations
     */
    suspend fun analyzeAndRecommend(channels: List<Channel>) {
        allChannels = channels
        
        // 1. Récupérer l'historique de visionnage
        val watchHistory = watchHistoryDao.getWatchHistory(limit = 100)
            .first()
            .filter { (it.duration ?: 0L) >= MIN_WATCH_TIME_MS }

        if (watchHistory.isEmpty()) {
            Timber.d("📊 Pas assez d'historique pour les recommandations")
            _userHabits.value = null
            return
        }

        // 2. Analyser les habitudes
        val habits = analyzeHabits(watchHistory)
        _userHabits.value = habits

        Timber.i("📊 Habitudes analysées: ${habits.favoriteCategories.size} catégories, " +
                "${habits.preferredTimeSlots.size} créneaux favoris")

        // 3. Générer recommandations contextuelles
        val recommendations = generateContextualRecommendations(habits, channels)
        _recommendations.value = recommendations

        // 4. Sauvegarder pour usage futur
        saveHabits(habits)
    }

    /**
     * Analyse les habitudes à partir de l'historique
     */
    private suspend fun analyzeHabits(history: List<WatchHistory>): UserHabits {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        
        // Récupérer les détails des chaînes pour avoir les catégories
        val channelsMap = mutableMapOf<String, Channel>()
        history.map { it.channelId }.distinct().forEach { channelId ->
            channelDao.getChannelById(channelId)?.let { channel ->
                channelsMap[channelId] = channel
            }
        }
        
        // Analyse par catégorie (via les chaînes)
        val categoryWatchTime = history.groupBy { entry ->
            channelsMap[entry.channelId]?.category ?: "Autre"
        }.mapValues { entry -> entry.value.sumOf { it.duration ?: 0L } }
        
        val totalWatchTime = categoryWatchTime.values.sum()
        val favoriteCategories = if (totalWatchTime > 0) {
            categoryWatchTime
                .map { (category, time) -> category to (time.toDouble() / totalWatchTime) }
                .sortedByDescending { it.second }
                .take(3)
                .toMap()
        } else emptyMap()

        // Analyse par jour de la semaine
        val dayOfWeekPattern = history.groupBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.watchedAt }
            cal.get(Calendar.DAY_OF_WEEK)
        }.mapValues { entry -> entry.value.sumOf { it.duration ?: 0L } }

        // Analyse par créneau horaire
        val timeSlots = history.groupBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.watchedAt }
            cal.get(Calendar.HOUR_OF_DAY) / 4 // Créneaux de 4h
        }.mapValues { entry -> entry.value.sumOf { it.duration ?: 0L } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        // Chaînes favorites
        val channelWatchTime = history.groupBy { it.channelId }
            .mapValues { entry -> entry.value.sumOf { it.duration ?: 0L } }
        
        val favoriteChannels = channelWatchTime
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        return UserHabits(
            favoriteCategories = favoriteCategories,
            dayOfWeekPattern = dayOfWeekPattern,
            preferredTimeSlots = timeSlots,
            favoriteChannelIds = favoriteChannels,
            lastAnalyzedAt = now
        )
    }

    /**
     * Génère des recommandations contextuelles basées sur le moment actuel
     */
    private fun generateContextualRecommendations(
        habits: UserHabits,
        channels: List<Channel>
    ): List<Recommendation> {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val currentDayOfWeek = now.dayOfWeek.value
        val currentHour = now.hour
        val currentTimeSlot = currentHour / 4

        val recommendations = mutableListOf<Recommendation>()

        // 1. Recommandation basée sur le jour + créneau (ex: sport samedi soir)
        val dayHabit = habits.dayOfWeekPattern[currentDayOfWeek] ?: 0
        val isPreferredTime = habits.preferredTimeSlots.contains(currentTimeSlot)
        
        if (dayHabit > 0 && isPreferredTime) {
            // Trouver les catégories regardées ce jour à cette heure
            val topCategories = habits.favoriteCategories.keys.take(2)
            
            topCategories.forEach { category ->
                val matchingChannels = channels.filter { 
                    it.category == category && it.groupTitle?.contains(category, ignoreCase = true) == true
                }
                
                if (matchingChannels.isNotEmpty()) {
                    recommendations.add(
                        Recommendation(
                            type = RecommendationType.CONTEXTUAL,
                            title = "${getDayName(currentDayOfWeek)} soir - $category",
                            description = "Basé sur vos habitudes de visionnage",
                            channels = matchingChannels.take(5),
                            confidence = HIGH_CONFIDENCE,
                            triggerTime = now,
                            icon = "📺"
                        )
                    )
                }
            }
        }

        // 2. Recommandation proactive (30 min avant créneau habituel)
        val nextTimeSlot = (currentTimeSlot + 1) % 6
        if (habits.preferredTimeSlots.contains(nextTimeSlot)) {
            val minutesUntilSlot = ((nextTimeSlot * 4) - currentHour) * 60 - now.minute
            
            if (minutesUntilSlot in 15..45) { // Entre 15 et 45 min avant
                habits.favoriteCategories.keys.firstOrNull()?.let { category ->
                    val previewChannels = channels.filter { it.category == category }
                    
                    recommendations.add(
                        Recommendation(
                            type = RecommendationType.PROACTIVE,
                            title = "Dans ${minutesUntilSlot} minutes - $category",
                            description = "Votre créneau habituel commence bientôt",
                            channels = previewChannels.take(3),
                            confidence = MEDIUM_CONFIDENCE,
                            triggerTime = now.plusMinutes(minutesUntilSlot.toLong()),
                            icon = "⏰"
                        )
                    )
                }
            }
        }

        // 3. Recommandation basée sur les chaînes favorites
        val favoriteChannels = channels.filter { 
            habits.favoriteChannelIds.contains(it.id) 
        }
        
        if (favoriteChannels.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.FAVORITES,
                    title = "Vos chaînes préférées",
                    description = "Retrouvez vos contenus favoris",
                    channels = favoriteChannels.take(5),
                    confidence = HIGH_CONFIDENCE,
                    triggerTime = now,
                    icon = "⭐"
                )
            )
        }

        // 4. Découverte - catégories similaires
        if (recommendations.size < 3) {
            val allCategories = channels.map { it.category }.distinct()
            val unusedCategories = allCategories.filter { !habits.favoriteCategories.containsKey(it) }
            
            if (unusedCategories.isNotEmpty()) {
                val discoveryCategory = unusedCategories.random()
                val discoveryChannels = channels.filter { it.category == discoveryCategory }
                
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.DISCOVERY,
                        title = "Découvrir - $discoveryCategory",
                        description = "Nouveautés basées sur vos goûts",
                        channels = discoveryChannels.take(4),
                        confidence = LOW_CONFIDENCE,
                        triggerTime = now,
                        icon = "✨"
                    )
                )
            }
        }

        return recommendations.sortedByDescending { it.confidence }
    }

    /**
     * Vérifie si une recommandation proactive doit être affichée maintenant
     */
    fun shouldShowProactiveNotification(): Boolean {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val habits = _userHabits.value ?: return false
        
        val currentHour = now.hour
        val currentTimeSlot = currentHour / 4
        val nextTimeSlot = (currentTimeSlot + 1) % 6
        
        return if (habits.preferredTimeSlots.contains(nextTimeSlot)) {
            val minutesUntilSlot = ((nextTimeSlot * 4) - currentHour) * 60 - now.minute
            minutesUntilSlot in 25..35 // Fenêtre de 30 min
        } else {
            false
        }
    }

    /**
     * Récupère les recommandations actives pour l'écran d'accueil
     */
    fun getHomeScreenRecommendations(): Flow<List<Recommendation>> {
        return _recommendations.map { recs ->
            recs.filter { it.type != RecommendationType.DISCOVERY }
        }
    }

    /**
     * Enregistre une session de visionnage pour améliorer les futures recommandations
     */
    suspend fun recordWatchSession(channel: Channel, durationMs: Long) {
        val watchHistory = WatchHistory(
            id = 0,
            channelId = channel.id,
            watchedAt = System.currentTimeMillis(),
            duration = durationMs,
            position = null,
            completed = durationMs > 300000 // 5 min = considéré comme complété
        )
        
        watchHistoryDao.insertWatchHistory(watchHistory)
        Timber.d("📊 Session enregistrée: ${channel.name} (${durationMs / 1000}s)")
    }

    private suspend fun saveHabits(habits: UserHabits) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey("top_category_count")] = habits.favoriteCategories.size
            prefs[stringPreferencesKey("top_category")] = habits.favoriteCategories.keys.firstOrNull() ?: ""
            prefs[longPreferencesKey("last_analyzed")] = habits.lastAnalyzedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Dimanche"
            2 -> "Lundi"
            3 -> "Mardi"
            4 -> "Mercredi"
            5 -> "Jeudi"
            6 -> "Vendredi"
            7 -> "Samedi"
            else -> "Jour"
        }
    }

    private fun LocalDateTime.plusMinutes(minutes: Long): LocalDateTime {
        return this.plusMinutes(minutes)
    }
}

/**
 * Données des habitudes utilisateur
 */
data class UserHabits(
    val favoriteCategories: Map<String, Double>, // Catégorie -> % du temps total
    val dayOfWeekPattern: Map<Int, Long>, // Jour (1-7) -> temps total en ms
    val preferredTimeSlots: List<Int>, // Créneaux favoris (0-5: 0-4h, 4-8h, etc.)
    val favoriteChannelIds: List<String>,
    val lastAnalyzedAt: LocalDateTime?
)

/**
 * Recommandation générée
 */
data class Recommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val channels: List<Channel>,
    val confidence: Double, // 0.0 - 1.0
    val triggerTime: LocalDateTime,
    val icon: String,
    val id: String = java.util.UUID.randomUUID().toString()
)

enum class RecommendationType {
    CONTEXTUAL,    // Basé sur jour + heure actuels
    PROACTIVE,     // 30 min avant créneau habituel
    FAVORITES,     // Chaînes les plus regardées
    DISCOVERY      // Nouvelles catégories
}
