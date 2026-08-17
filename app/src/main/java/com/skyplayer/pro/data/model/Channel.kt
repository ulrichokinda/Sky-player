package com.skyplayer.pro.data.model

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Modèle de données pour une chaîne TV ou contenu VOD
 * Compatible avec Room pour persistance locale
 */
@Entity(
    tableName = "channels",
    indices = [
        // Index pour les requêtes fréquentes Live/VOD/Séries (full scan évité sur 50k+ lignes)
        Index("type"),
        Index("category"),
        Index("isFavorite")
    ]
)
data class Channel(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String,
    val type: ContentType,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false, // Pour contrôle parental
    val epgId: String? = null, // ID pour guide électronique des programmes
    val groupTitle: String? = null,
    val lastWatched: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Table FTS4 pour recherche ultra-rapide (Suggestion 2)
 * On stocke le channelId explicitement car le lien contentEntity nécessite un ID de type Long
 */
@Entity(tableName = "channels_fts")
@Fts4
data class ChannelFts(
    val channelId: String,
    val name: String,
    val category: String,
    val groupTitle: String?
)

/**
 * Types de contenu disponibles
 */
enum class ContentType {
    LIVE_TV,
    LIVE_SPORTS,
    LIVE_NEWS,
    VOD_MOVIE,
    VOD_SERIES,
    SERIES_EPISODE,
    RADIO
}

/**
 * Groupe de catégories pour l'affichage
 */
enum class CategoryGroup(val displayName: String) {
    ALL("Tout"),
    ENTERTAINMENT("Divertissement"),
    SPORTS("Sports"),
    NEWS("Actualités"),
    MOVIES("Films"),
    DOCUMENTARY("Documentaires"),
    KIDS("Enfants"),
    MUSIC("Musique"),
    RELIGIOUS("Religieux"),
    LOCAL("Locales")
}

/**
 * Représentation d'une playlist M3U/Xtream
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey
    val id: String,
    val name: String,
    val sourceType: SourceType,
    val url: String? = null, // Pour M3U direct
    val username: String? = null, // Pour Xtream
    val password: String? = null, // Pour Xtream
    val baseUrl: String? = null, // Pour Xtream
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val channelCount: Int = 0,
    val epgUrl: String? = null // URL EPG extraite à l'import (évite une requête HTTP au refresh)
)

enum class SourceType {
    M3U_URL,
    M3U_FILE,
    XTREAM_CODES
}
