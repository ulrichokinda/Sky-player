package com.skyplayer.pro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité pour stocker les favoris
 * Fonctionne pour Live TV, Films et Séries
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String,
    val type: String, // LIVE_TV, VOD_MOVIE, VOD_SERIES
    val addedAt: Long = System.currentTimeMillis()
)
