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
            .fallbackToDestructiveMigration() // Recrée la DB si migration impossible
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
