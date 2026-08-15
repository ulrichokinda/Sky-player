package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour gérer les favoris (Live TV, Films, Séries)
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val favoriteDao = database.favoriteDao()

    // ========== Récupération ==========
    fun getAllFavorites(): Flow<List<Channel>> = favoriteDao.getAllFavorites().map { list -> list.map { it.toChannel() } }

    fun getLiveTvFavorites(): Flow<List<Channel>> = favoriteDao.getAllFavorites().map { list ->
        list.map { it.toChannel() }.filter { it.type in ChannelRepository.LIVE_CONTENT_TYPES }
    }

    fun getMovieFavorites(): Flow<List<Channel>> = favoriteDao.getFavoritesByType(ContentType.VOD_MOVIE.name).map { list -> list.map { it.toChannel() } }

    fun getSeriesFavorites(): Flow<List<Channel>> = favoriteDao.getFavoritesByType(ContentType.VOD_SERIES.name).map { list -> list.map { it.toChannel() } }

    suspend fun isFavorite(channelId: String): Boolean {
        return favoriteDao.isFavorite(channelId)
    }

    // ========== Modification ==========
    suspend fun addToFavorites(channel: Channel) {
        val favorite = channel.toFavoriteEntity()
        favoriteDao.insertFavorite(favorite)
        Timber.i("⭐ Ajouté aux favoris: ${channel.name} [${channel.type}]")
    }

    suspend fun removeFromFavorites(channelId: String) {
        favoriteDao.deleteFavorite(channelId)
        Timber.i("❌ Retiré des favoris: $channelId")
    }

    suspend fun toggleFavorite(channel: Channel): Boolean {
        return if (isFavorite(channel.id)) {
            removeFromFavorites(channel.id)
            false
        } else {
            addToFavorites(channel)
            true
        }
    }

    // ========== Stats ==========
    suspend fun getFavoriteCount(): Int = favoriteDao.getFavoriteCount()

    suspend fun getFavoriteCountByType(type: ContentType): Int {
        return favoriteDao.getFavoriteCountByType(type.name)
    }
}

/**
 * Extension pour convertir Channel en FavoriteEntity
 */
private fun Channel.toFavoriteEntity(): com.skyplayer.pro.data.local.FavoriteEntity {
    return com.skyplayer.pro.data.local.FavoriteEntity(
        id = this.id,
        name = this.name,
        url = this.url,
        logoUrl = this.logoUrl,
        category = this.category,
        type = this.type.name,
        addedAt = System.currentTimeMillis()
    )
}

/**
 * Extension pour convertir FavoriteEntity en Channel
 */
private fun com.skyplayer.pro.data.local.FavoriteEntity.toChannel(): Channel {
    return Channel(
        id = this.id,
        name = this.name,
        url = this.url,
        logoUrl = this.logoUrl,
        category = this.category,
        type = ContentType.valueOf(this.type),
        epgId = null,
        groupTitle = this.category
    )
}
