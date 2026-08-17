package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des chaînes et contenus
 * Fournit une abstraction entre la source de données et l'UI
 */
@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao
) {

    companion object {
        val LIVE_CONTENT_TYPES = listOf(
            ContentType.LIVE_TV,
            ContentType.LIVE_SPORTS,
            ContentType.LIVE_NEWS,
            ContentType.RADIO
        )

        /**
         * Échappe une saisie utilisateur avant injection dans `MATCH` (FTS4).
         *
         * Sans échappement, un caractère d'opérateur FTS (`"`, `*`, `(`, `-`, …)
         * lève une `SQLiteException` → crash de l'app. On retire ces caractères,
         * puis chaque terme devient un préfixe (`terme*`) pour la recherche partielle.
         */
        fun sanitizeFtsQuery(raw: String): String {
            val tokens = raw.trim().split(Regex("\\s+")).mapNotNull { token ->
                token.replace(Regex("""["*()\-^<>=~!\\]"""), "")
                    .takeIf { it.isNotBlank() }
            }
            return tokens.joinToString(" ") { "$it*" }.ifEmpty { "\"\"" }
        }
    }

    // Toutes les chaînes
    fun getAllChannels(): Flow<List<Channel>> =
        channelDao.getAllChannels()

    // Récupérer les chaînes par type
    fun getLiveChannels(): Flow<List<Channel>> =
        channelDao.getChannelsByTypes(LIVE_CONTENT_TYPES)

    fun getVodContent(): Flow<List<Channel>> =
        channelDao.getChannelsByType(ContentType.VOD_MOVIE)

    fun getSeries(): Flow<List<Channel>> =
        channelDao.getChannelsByType(ContentType.VOD_SERIES)

    // Favoris
    fun getFavorites(): Flow<List<Channel>> =
        channelDao.getFavorites()

    suspend fun toggleFavorite(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channelId, !isFavorite)
    }

    // Recherche ultra-rapide (FTS4)
    fun searchChannels(query: String): Flow<List<Channel>> =
        channelDao.searchChannels(sanitizeFtsQuery(query))

    // Historique
    suspend fun updateLastWatched(channelId: String) {
        channelDao.updateLastWatched(channelId)
    }

    fun getRecentlyWatched(limit: Int = 20): Flow<List<Channel>> =
        channelDao.getRecentlyWatched(ContentType.LIVE_TV, limit)

    fun getAllRecentlyWatched(limit: Int = 10): Flow<List<Channel>> =
        channelDao.getAllRecentlyWatched(limit)

    // Détails
    suspend fun getChannelById(channelId: String): Channel? =
        channelDao.getChannelById(channelId)

    // Catégories
    fun getCategories(type: ContentType): Flow<List<String>> =
        channelDao.getCategories(type)

    fun getLiveCategories(): Flow<List<String>> =
        channelDao.getCategoriesByTypes(LIVE_CONTENT_TYPES)

    // Sauvegarde
    suspend fun saveChannels(channels: List<Channel>) {
        channelDao.insertChannels(channels)
    }

    suspend fun deleteAllChannels() {
        channelDao.deleteAllChannels()
    }

    suspend fun getChannelCount(): Int =
        channelDao.getChannelCount()
}
