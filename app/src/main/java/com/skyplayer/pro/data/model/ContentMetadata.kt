package com.skyplayer.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Métadonnées enrichies pour les films et séries
 * Stocké en local avec Room
 */
@Entity(tableName = "content_metadata")
data class ContentMetadata(
    @PrimaryKey
    val contentId: String,  // ID du Channel associé
    
    // Informations de base
    val title: String,
    val plot: String? = null,  // Synopsis/description
    val tagline: String? = null,  // Slogan
    
    // Dates
    val releaseDate: String? = null,  // Date de sortie
    val year: Int? = null,  // Année extraite
    
    // Équipe
    val director: String? = null,  // Réalisateur
    val cast: String? = null,  // Acteurs (séparés par virgule)
    val writer: String? = null,  // Scénariste
    val producer: String? = null,  // Producteur
    
    // Classification
    val genre: String? = null,  // Genres (séparés par virgule)
    val rated: String? = null,  // Classification (PG-13, R, etc.)
    
    // Évaluations
    val imdbRating: String? = null,  // Note IMDB
    val imdbId: String? = null,  // ID IMDB
    val tmdbId: String? = null,  // ID TMDB
    
    // Infos techniques
    val duration: String? = null,  // Durée (ex: "2h 15min")
    val runtime: Int? = null,  // Durée en minutes
    
    // Images
    val posterUrl: String? = null,  // URL de l'affiche
    val backdropUrl: String? = null,  // URL du fond
    val trailerUrl: String? = null,  // URL de la bande-annonce
    
    // Info source
    val sourceType: String = "xtream",  // xtream, tmdb, local
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Retourne la liste des acteurs
     */
    fun getActorsList(): List<String> {
        return cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Retourne la liste des genres
     */
    fun getGenresList(): List<String> {
        return genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Retourne l'année formatée
     */
    fun getYearFormatted(): String {
        return year?.toString() ?: releaseDate?.take(4) ?: "N/A"
    }
    
    /**
     * Retourne le rating formaté
     */
    fun getRatingFormatted(): String {
        return imdbRating?.let { "$it/10" } ?: "Non noté"
    }
    
    /**
     * Retourne la description tronquée
     */
    fun getShortPlot(maxLength: Int = 150): String {
        return plot?.take(maxLength)?.let { 
            if (plot.length > maxLength) "$it..." else it 
        } ?: "Aucune description disponible"
    }
}

/**
 * Classe utilitaire pour créer ContentMetadata depuis XtreamVodDetails
 */
fun XtreamVodDetails.toContentMetadata(channelId: String): ContentMetadata {
    return ContentMetadata(
        contentId = channelId,
        title = name,
        plot = plot,
        releaseDate = getReleaseDate(),
        year = extractYear(getReleaseDate()),
        director = director,
        cast = cast,
        genre = genre,
        imdbRating = ratingImdb ?: rating,
        tmdbId = tmdbId,
        duration = getDurationFormatted(),
        posterUrl = movieImage ?: streamIcon,
        backdropUrl = backdropPath?.firstOrNull(),
        trailerUrl = youtubeTrailer,
        sourceType = "xtream"
    )
}

/**
 * Classe utilitaire pour créer ContentMetadata depuis XtreamSeries
 */
fun XtreamSeries.toContentMetadata(channelId: String): ContentMetadata {
    return ContentMetadata(
        contentId = channelId,
        title = name,
        plot = plot,
        releaseDate = releaseDate,
        year = extractYear(releaseDate),
        director = director,
        cast = cast,
        genre = genre,
        imdbRating = rating,
        duration = episodeRunTime,
        posterUrl = cover,
        backdropUrl = backdropPath?.firstOrNull(),
        trailerUrl = youtubeTrailer,
        sourceType = "xtream"
    )
}

/**
 * Extrait l'année depuis une date
 */
private fun extractYear(dateString: String?): Int? {
    return dateString?.take(4)?.toIntOrNull()
}
