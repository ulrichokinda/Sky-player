package com.skyplayer.pro.di

import android.content.Context
import androidx.room.Room
import com.skyplayer.pro.data.local.AppDatabase
import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.local.EpgDao
import com.skyplayer.pro.data.local.PlaylistDao
import com.skyplayer.pro.data.local.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt pour la base de données Room
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // Migration propre 5→6 (index + epgUrl). Le fallback destructif n'est
            // conservé que pour les bases antérieures à v5 (versions de test pré-1.0,
            // dont les schémas ne sont plus exportés) — jamais pour la v5 actuelle.
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao {
        return database.channelDao()
    }
    
    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideEpgDao(database: AppDatabase): EpgDao {
        return database.epgDao()
    }
    
    @Provides
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }
}
