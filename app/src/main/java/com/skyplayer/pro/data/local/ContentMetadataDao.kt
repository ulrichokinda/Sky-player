package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skyplayer.pro.data.model.ContentMetadata
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour gérer les métadonnées des films et séries
 */
@Dao
interface ContentMetadataDao {

    @Query("SELECT * FROM content_metadata WHERE contentId = :contentId LIMIT 1")
    suspend fun getMetadata(contentId: String): ContentMetadata?

    @Query("SELECT * FROM content_metadata WHERE contentId = :contentId LIMIT 1")
    fun getMetadataFlow(contentId: String): Flow<ContentMetadata?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: ContentMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadataList: List<ContentMetadata>)

    @Query("DELETE FROM content_metadata WHERE contentId = :contentId")
    suspend fun deleteMetadata(contentId: String)

    @Query("DELETE FROM content_metadata")
    suspend fun deleteAllMetadata()

    @Query("SELECT * FROM content_metadata WHERE year = :year")
    suspend fun getByYear(year: Int): List<ContentMetadata>

    @Query("SELECT * FROM content_metadata ORDER BY year DESC LIMIT 50")
    suspend fun getRecentReleases(): List<ContentMetadata>

    @Query("SELECT DISTINCT year FROM content_metadata WHERE year IS NOT NULL ORDER BY year DESC")
    suspend fun getAvailableYears(): List<Int>

    @Query("SELECT DISTINCT genre FROM content_metadata WHERE genre IS NOT NULL")
    suspend fun getAllGenres(): List<String>

    @Query("UPDATE content_metadata SET imdbRating = :rating WHERE contentId = :contentId")
    suspend fun updateRating(contentId: String, rating: String)

    @Query("SELECT COUNT(*) FROM content_metadata")
    suspend fun getMetadataCount(): Int
}
