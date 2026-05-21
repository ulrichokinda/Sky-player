package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.local.EpgDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.model.SourceType
import com.skyplayer.pro.data.parser.EpgParser
import com.skyplayer.pro.data.remote.XtreamCodesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val epgDao: EpgDao,
    private val xtreamApi: XtreamCodesApi,
    private val okHttpClient: OkHttpClient
) {
    private val parser = EpgParser()

    /**
     * Télécharge et met à jour l'EPG depuis une URL XMLTV
     */
    suspend fun fetchEpg(url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                
                val body = response.body ?: throw Exception("Body vide")
                val inputStream = body.byteStream()
                
                // Gérer GZIP si nécessaire
                val finalStream = if (url.endsWith(".gz", ignoreCase = true)) {
                    GZIPInputStream(inputStream)
                } else {
                    inputStream
                }

                val programs = parser.parse(finalStream)
                if (programs.isNotEmpty()) {
                    epgDao.deleteOldPrograms()
                    // Chunking pour Room
                    programs.chunked(1000).forEach { chunk ->
                        epgDao.insertPrograms(chunk)
                    }
                    Timber.i("✅ EPG mis à jour : ${programs.size} programmes")
                    Result.success(programs.size)
                } else {
                    Result.failure(Exception("Aucun programme trouvé dans le XMLTV"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Échec mise à jour EPG")
            Result.failure(e)
        }
    }

    /**
     * Télécharge l'EPG depuis Xtream Codes
     */
    suspend fun fetchXtreamEpg(baseUrl: String, user: String, pass: String, streamId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "$baseUrl/player_api.php"
            val response = xtreamApi.getShortEpg(apiUrl, user, pass, streamId)
            
            val programs = response.epgList?.map { item: com.skyplayer.pro.data.model.XtreamEpgListing ->
                EpgProgram(
                    epgId = streamId.toString(),
                    start = (item.startTimestamp ?: 0L) * 1000,
                    stop = (item.stopTimestamp ?: 0L) * 1000,
                    title = item.title ?: "Sans titre",
                    description = item.description
                )
            } ?: emptyList<EpgProgram>()

            if (programs.isNotEmpty()) {
                epgDao.insertPrograms(programs)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Aucun EPG trouvé pour ce stream"))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Échec EPG Xtream")
            Result.failure(e)
        }
    }

    suspend fun getCurrentProgram(epgId: String): EpgProgram? {
        return epgDao.getCurrentProgram(epgId)
    }

    fun getUpcomingPrograms(epgId: String): Flow<List<EpgProgram>> {
        return epgDao.getUpcomingPrograms(epgId)
    }

    suspend fun clearAll() {
        epgDao.deleteAllPrograms()
    }

    /**
     * Met à jour l'EPG pour une chaîne spécifique selon sa source
     */
    suspend fun updateEpgForChannel(channel: Channel, playlist: Playlist?) {
        if (channel.epgId == null || playlist == null) return
        
        // Vérifier si on a déjà un programme récent pour éviter les appels inutiles
        val current = getCurrentProgram(channel.epgId)
        if (current != null && current.stop > System.currentTimeMillis() + 3600000) return // Déjà ok pour 1h

        when (playlist.sourceType) {
            SourceType.XTREAM_CODES -> {
                val baseUrl = playlist.baseUrl ?: return
                val user = playlist.username ?: return
                val pass = playlist.password ?: return
                // Extraire l'ID numérique du stream Xtream
                val streamId = channel.id.substringAfterLast("_").toIntOrNull() ?: return
                fetchXtreamEpg(baseUrl, user, pass, streamId)
            }
            else -> {}
        }
    }
}
