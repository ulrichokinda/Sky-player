package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skyplayer.pro.data.model.WatchHistory
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion de l'historique de visionnage
 */
@Dao
interface WatchHistoryDao {
    
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getWatchHistory(limit: Int = 50): Flow<List<WatchHistory>>
    
    @Query("SELECT * FROM watch_history WHERE channelId = :channelId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getLastWatchForChannel(channelId: String): WatchHistory?
    
    @Query("SELECT * FROM watch_history WHERE channelId = :channelId")
    fun getHistoryForChannel(channelId: String): Flow<List<WatchHistory>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(watchHistory: WatchHistory)
    
    @Delete
    suspend fun deleteWatchHistory(watchHistory: WatchHistory)
    
    @Query("DELETE FROM watch_history WHERE channelId = :channelId")
    suspend fun deleteHistoryForChannel(channelId: String)
    
    @Query("DELETE FROM watch_history")
    suspend fun deleteAllHistory()
    
    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getHistoryCount(): Int
    
    @Query("DELETE FROM watch_history WHERE watchedAt < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)
}
