package com.skyplayer.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ChannelFts
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.model.WatchHistory

/**
 * Base de données Room principale
 * Gère les chaînes, playlists, favoris, historique et métadonnées
 */
@Database(
    entities = [
        Channel::class,
        ChannelFts::class,
        Playlist::class,
        WatchHistory::class,
        FavoriteEntity::class,
        ContentMetadata::class,
        EpgProgram::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun epgDao(): EpgDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun contentMetadataDao(): ContentMetadataDao

    companion object {
        const val DATABASE_NAME = "skyplayer_database"
    }
}
