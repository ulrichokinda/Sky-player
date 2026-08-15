package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.XtreamEpisode
import com.skyplayer.pro.data.model.XtreamSeriesDetails
import com.skyplayer.pro.data.organizer.ContentClassifier
import com.skyplayer.pro.data.remote.XtreamCodesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository spécialisé pour la gestion des séries et des épisodes
 */
@Singleton
class SeriesRepository @Inject constructor(
    private val xtreamApi: XtreamCodesApi,
    private val playlistRepository: PlaylistRepository,
    private val contentClassifier: ContentClassifier
) {

    /**
     * Regroupe les épisodes d'une source M3U par saison
     */
    suspend fun groupM3uEpisodesBySeason(episodes: List<Channel>): Map<Int, List<Channel>> = withContext(Dispatchers.IO) {
        episodes.groupBy { contentClassifier.extractSeasonNumber(it.name) }
            .toSortedMap()
    }

    /**
     * Récupère les détails d'une série et regroupe les épisodes par saison
     * 
     * @param playlistId ID de la playlist pour récupérer les credentials
     * @param seriesId ID technique de la série chez le fournisseur
     * @return Map regroupant les épisodes par numéro de saison
     */
    suspend fun getSeriesDetailsGroupedBySeason(
        playlistId: String,
        seriesId: Int
    ): Map<Int, List<XtreamEpisode>> = withContext(Dispatchers.IO) {
        try {
            // 1. Récupérer les credentials de la playlist
            val playlist = playlistRepository.getPlaylistById(playlistId) 
                ?: throw Exception("Playlist introuvable")
            
            val baseUrl = playlist.baseUrl ?: throw Exception("URL serveur manquante")
            val username = playlist.username ?: throw Exception("Username manquant")
            val password = playlist.password ?: throw Exception("Password manquant")
            
            val apiUrl = "${baseUrl.trim().trimEnd('/')}/player_api.php"

            // 2. Appel API Xtream
            val response = xtreamApi.getSeriesDetails(
                fullUrl = apiUrl,
                username = username,
                password = password,
                seriesId = seriesId
            )

            val details = response.body()?.let { XtreamCodesApi.parseSeriesDetailsStream(it) } 
                ?: return@withContext emptyMap()

            // 3. Regroupement par saison (Algorithme demandé)
            // L'API Xtream peut renvoyer les épisodes soit dans une Map (clé=saison), 
            // soit on doit les regrouper nous-mêmes si le format varie.
            
            val episodesMap: Map<String, List<XtreamEpisode>> = details.episodes ?: emptyMap()
            
            if (episodesMap.isNotEmpty()) {
                // Si l'API renvoie déjà une Map, on s'assure que les clés sont des Int
                return@withContext episodesMap.mapKeys { (key, _) -> key.toIntOrNull() ?: 0 }
            } else {
                // Fallback: Si on avait une liste plate (rare chez Xtream mais possible selon les versions)
                // on utiliserait .groupBy { it.seasonNumber }
                Timber.w("Aucun épisode trouvé ou format inattendu pour la série $seriesId")
                return@withContext emptyMap()
            }

        } catch (e: Exception) {
            Timber.e(e, "Erreur lors de la récupération des détails de la série $seriesId")
            emptyMap()
        }
    }
}
