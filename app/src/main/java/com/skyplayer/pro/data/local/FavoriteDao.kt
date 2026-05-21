package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour gérer les favoris
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY name ASC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :channelId")
    suspend fun deleteFavorite(channelId: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(*) FROM favorites WHERE type = :type")
    suspend fun getFavoriteCountByType(type: String): Int
}
