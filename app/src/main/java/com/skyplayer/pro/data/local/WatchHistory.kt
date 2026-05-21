package com.skyplayer.pro.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyplayer.pro.data.model.Channel

/**
 * Entité pour l'historique de visionnage
 */
@Entity(
    tableName = "watch_history",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("channelId")]
)
data class WatchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val duration: Long? = null, // Durée regardée en ms
    val position: Long? = null, // Position de reprise pour VOD
    val completed: Boolean = false
)
