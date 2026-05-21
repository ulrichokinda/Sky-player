package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ChannelFts
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion des chaînes et contenus
 */
@Dao
interface ChannelDao {
    
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE type = :type ORDER BY name ASC")
    fun getChannelsByType(type: ContentType): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE category = :category AND type = :type")
    fun getChannelsByCategory(category: String, type: ContentType): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): Channel?
    
    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%'")
    fun searchChannelsOld(query: String): Flow<List<Channel>>

    /**
     * Recherche ultra-rapide via FTS4 (Suggestion 2)
     */
    @Query("""
        SELECT * FROM channels 
        WHERE id IN (SELECT channelId FROM channels_fts WHERE channels_fts MATCH :query)
    """)
    fun searchChannels(query: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelsFts(fts: List<ChannelFts>)
    
    @Query("SELECT DISTINCT category FROM channels WHERE type = :type")
    fun getCategories(type: ContentType): Flow<List<String>>
    
    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: String, isFavorite: Boolean)
    
    @Query("UPDATE channels SET lastWatched = :timestamp WHERE id = :channelId")
    suspend fun updateLastWatched(channelId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM channels WHERE type = :type AND lastWatched IS NOT NULL ORDER BY lastWatched DESC LIMIT :limit")
    fun getRecentlyWatched(type: ContentType, limit: Int = 20): Flow<List<Channel>>
    
    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)
    
    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)
    
    @Update
    suspend fun updateChannel(channel: Channel)
    
    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int
    
    @Query("SELECT * FROM channels WHERE id LIKE :playlistId || '_%' ORDER BY name ASC")
    fun getChannelsByTypePlaylistId(playlistId: String): Flow<List<Channel>>
    
    @Query("DELETE FROM channels WHERE id LIKE :playlistId || '_%'")
    suspend fun deleteChannelsByPlaylistId(playlistId: String)
}
