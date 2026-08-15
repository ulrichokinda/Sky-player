package com.skyplayer.pro.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Modèle pour les détails des films VOD depuis Xtream Codes
 */
@Keep
data class XtreamVodDetails(
    @SerializedName("num")
    val num: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("stream_id")
    val streamId: Int = 0,
    @SerializedName("stream_icon")
    val streamIcon: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("container_extension")
    val containerExtension: String? = null,
    @SerializedName("custom_sid")
    val customSid: String? = null,
    @SerializedName("direct_source")
    val directSource: String? = null,
    
    // Métadonnées du film
    @SerializedName("plot")
    val plot: String? = null,
    @SerializedName("cast")
    val cast: String? = null,
    @SerializedName("director")
    val director: String? = null,
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("releasedate")
    val releaseDate: String? = null,
    @SerializedName("release_date")
    val releaseDateAlt: String? = null,
    @SerializedName("rating")
    val rating: String? = null,
    @SerializedName("rating_imdb")
    val ratingImdb: String? = null,
    @SerializedName("duration")
    val duration: String? = null,
    @SerializedName("bitrate")
    val bitrate: Int = 0,
    @SerializedName("movie_image")
    val movieImage: String? = null,
    @SerializedName("backdrop_path")
    val backdropPath: List<String>? = null,
    @SerializedName("youtube_trailer")
    val youtubeTrailer: String? = null,
    @SerializedName("tmdb_id")
    val tmdbId: String? = null,
    
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("category_ids")
    val categoryIds: List<Int>? = null
)

/**
 * Extension pour extraire la date de sortie depuis différents formats
 */
fun XtreamVodDetails.getReleaseDate(): String {
    return releaseDate ?: releaseDateAlt ?: createdAt ?: "Date inconnue"
}

/**
 * Extension pour extraire le rating/10
 */
fun XtreamVodDetails.getRating(): String {
    return when {
        !ratingImdb.isNullOrEmpty() -> "${ratingImdb}/10"
        !rating.isNullOrEmpty() -> "${rating}/10"
        else -> "Non noté"
    }
}

/**
 * Extension pour formater la durée
 */
fun XtreamVodDetails.getDurationFormatted(): String {
    return duration ?: "Durée inconnue"
}

/**
 * Extension pour extraire la liste des acteurs
 */
fun XtreamVodDetails.getActorsList(): List<String> {
    return cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}

/**
 * Extension pour extraire la liste des genres
 */
fun XtreamVodDetails.getGenresList(): List<String> {
    return genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}
