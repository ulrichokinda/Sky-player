package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skyplayer.pro.data.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion des playlists
 */
@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): Playlist?
    
    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): Playlist?
    
    @Query("SELECT * FROM playlists WHERE isActive = 1")
    fun getActivePlaylists(): Flow<List<Playlist>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)
    
    @Query("UPDATE playlists SET isActive = :isActive WHERE id = :playlistId")
    suspend fun setPlaylistActive(playlistId: String, isActive: Boolean)
    
    @Query("UPDATE playlists SET lastUpdated = :timestamp WHERE id = :playlistId")
    suspend fun updateTimestamp(playlistId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
    
    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()
    
    @Query("UPDATE playlists SET channelCount = :count WHERE id = :playlistId")
    suspend fun updateChannelCount(playlistId: String, count: Int)
}
