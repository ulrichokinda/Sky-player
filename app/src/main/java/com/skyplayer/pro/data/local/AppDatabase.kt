package com.skyplayer.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
    exportSchema = true
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

        /**
         * Migration 5 → 6 : ajout des index de recherche sur `channels` (type,
         * catégorie, favori) et de la colonne `epgUrl` sur `playlists`.
         *
         * Uniquement des changements additifs : aucune donnée n'est perdue.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_type` ON `channels` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_category` ON `channels` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_isFavorite` ON `channels` (`isFavorite`)")
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `epgUrl` TEXT")
            }
        }
    }
}
