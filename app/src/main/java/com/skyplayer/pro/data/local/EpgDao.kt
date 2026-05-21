package com.skyplayer.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skyplayer.pro.data.model.EpgProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgram>)

    @Query("SELECT * FROM epg_programs WHERE epgId = :epgId AND stop > :currentTime ORDER BY start ASC LIMIT 1")
    suspend fun getCurrentProgram(epgId: String, currentTime: Long = System.currentTimeMillis()): EpgProgram?

    @Query("SELECT * FROM epg_programs WHERE epgId = :epgId AND stop > :currentTime ORDER BY start ASC")
    fun getUpcomingPrograms(epgId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<EpgProgram>>

    @Query("DELETE FROM epg_programs WHERE stop < :currentTime")
    suspend fun deleteOldPrograms(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()
}
