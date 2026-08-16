package com.skyplayer.pro.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skyplayer.pro.data.local.PlaylistDao
import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ChannelFts
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.model.SourceType
import com.skyplayer.pro.data.model.XtreamStream
import com.skyplayer.pro.data.model.XtreamSeries
import com.skyplayer.pro.data.model.XtreamUserInfo
import com.skyplayer.pro.data.model.toContentMetadata
import com.skyplayer.pro.data.parser.M3UParser
import com.skyplayer.pro.data.remote.XtreamCodesApi
import com.skyplayer.pro.data.remote.getLiveCategories
import com.skyplayer.pro.data.remote.getLiveStreams
import com.skyplayer.pro.data.remote.getSeries
import com.skyplayer.pro.data.remote.getSeriesCategories
import com.skyplayer.pro.data.remote.getVodCategories
import com.skyplayer.pro.data.remote.getVodStreams
import com.skyplayer.pro.data.remote.toChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.skyplayer.pro.util.XtreamUrlNormalizer
import androidx.room.withTransaction
import timber.log.Timber
import java.util.UUID

/**
 * État de progression pour le chargement d'une playlist (M3U ou Xtream)
 */
sealed class PlaylistLoadProgress {
    data class Loading(val message: String, val progress: Float? = null) : PlaylistLoadProgress()
    data class Success(val channelCount: Int) : PlaylistLoadProgress()
    data class Error(val exception: Throwable) : PlaylistLoadProgress()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository pour la gestion des playlists
 * Gère les sources M3U et Xtream Codes
 */
@Singleton
class PlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val epgRepository: EpgRepository,
    private val m3uParser: M3UParser,
    private val xtreamApi: XtreamCodesApi,
    private val okHttpClient: OkHttpClient,
    private val database: com.skyplayer.pro.data.local.AppDatabase
) {

    companion object {
        private val HAS_PLAYLIST = booleanPreferencesKey("has_playlist")
        private const val XTREAM_CREDS_PREFS = "xtream_credentials_vault"
    }

    private val secureXtreamPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            XTREAM_CREDS_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Flux indiquant si des playlists existent
    fun hasPlaylists(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_PLAYLIST] ?: (playlistDao.getPlaylistCount() > 0)
    }

    // Récupérer toutes les playlists
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { playlists ->
        playlists.map(::hydrateXtreamCredentials)
    }

    /**
     * Récupère une playlist par son ID avec les credentials hydratés
     */
    suspend fun getPlaylistById(playlistId: String): Playlist? {
        return playlistDao.getPlaylistById(playlistId)?.let { hydrateXtreamCredentials(it) }
    }

    // Ajouter une playlist M3U
    fun addM3UPlaylist(name: String, url: String): Flow<PlaylistLoadProgress> = flow {
        val cleanUrl = url.trim()
        emit(PlaylistLoadProgress.Loading("Vérification de l'URL..."))
        try {
            Timber.i("📥 Début ajout playlist M3U : $name depuis $cleanUrl")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                throw IllegalArgumentException("L'URL doit commencer par http:// ou https://")
            }

            // Valider et parser la playlist
            var epgUrl: String? = null
            emit(PlaylistLoadProgress.Loading("Téléchargement de la playlist..."))
            val response = try {
                val request = Request.Builder()
                    .url(cleanUrl)
                    // Same headers as M3UParser for consistency
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain, */*")
                    .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()
                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                Timber.tag("PlaylistLoader").e("${e.javaClass.simpleName}: ${e.message}", e)
                Timber.e(e, "❌ Échec téléchargement M3U")
                throw Exception("Erreur réseau : ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Impossible de télécharger la playlist"}. Vérifiez votre connexion et l'URL.", e)
            }

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401 -> "Erreur serveur (401) : Accès non autorisé. Vérifiez vos identifiants."
                    403 -> "Erreur serveur (403) : Accès refusé. Le serveur a bloqué la requête."
                    404 -> "Erreur serveur (404) : Playlist introuvable. Vérifiez l'URL."
                    500 -> "Erreur serveur (500) : Erreur interne du serveur. Réessayez plus tard."
                    502 -> "Erreur serveur (502) : Passerelle invalide. Réessayez plus tard."
                    503 -> "Erreur serveur (503) : Service indisponible. Réessayez plus tard."
                    else -> "Erreur serveur (${response.code}) : La playlist n'est pas accessible. (Vérifiez vos accès)"
                }
                throw Exception(errorMsg)
            }
            val responseBody = response.body
                ?: throw Exception("Le serveur n'a retourné aucun contenu")

            // Téléchargement avec progression
            val contentLength = responseBody.contentLength()
            val inputStream = responseBody.byteStream()
            val bufferedInputStream = java.io.BufferedInputStream(inputStream)
            var totalRead = 0L

            emit(PlaylistLoadProgress.Loading("Parsing de la playlist...", if (contentLength > 0) 0f else null))

            // We need to track progress and also parse, so let's use a custom InputStream to count bytes read!
            class CountingInputStream(private val input: java.io.InputStream) : java.io.FilterInputStream(input) {
                private var totalBytesRead = 0L

                override fun read(): Int {
                    val b = super.read()
                    if (b != -1) totalBytesRead++
                    return b
                }

                override fun read(b: ByteArray): Int {
                    val bytesRead = super.read(b)
                    if (bytesRead != -1) totalBytesRead += bytesRead
                    return bytesRead
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val bytesRead = super.read(b, off, len)
                    if (bytesRead != -1) totalBytesRead += bytesRead
                    return bytesRead
                }

                fun getTotalBytesRead(): Long = totalBytesRead
            }

            val countingStream = CountingInputStream(bufferedInputStream)

            // Now, let's parse in a separate coroutine to track progress?
            // Or, alternatively, parse first read all bytes, then parse, but wait, let's just read first, then parse with progress, but also track progress as we read!
            val content = buildString {
                val reader = BufferedReader(InputStreamReader(countingStream, Charsets.UTF_8))
                var currentLine: String?
                while (reader.readLine().also { currentLine = it } != null) {
                    appendLine(currentLine)
                    totalRead = countingStream.getTotalBytesRead()
                    if (contentLength > 0) {
                        val progress = totalRead.toFloat() / contentLength.toFloat()
                        emit(PlaylistLoadProgress.Loading("Parsing de la playlist...", progress.coerceIn(0f, 1f)))
                    }
                }
            }

            val channels = try {
                m3uParser.parseFromInputStream(
                    inputStream = content.byteInputStream(Charsets.UTF_8),
                    playlistId = "temp",
                    sourceUrl = cleanUrl,
                    onEpgUrlFound = { epgUrl = it }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur parsing M3U")
                throw Exception("Format de fichier invalide : Impossible de lire le contenu de la playlist.", e)
            }

            if (channels.isEmpty()) {
                throw Exception("Aucune chaîne trouvée dans la playlist")
            } else {
                emit(PlaylistLoadProgress.Loading("Sauvegarde des chaînes..."))
                val playlistId = "playlist_${System.currentTimeMillis()}"

                // Sauvegarder les chaînes d'abord (Metadata Cache)
                val preparedChannels = channels.map { it.copy(id = "${playlistId}_${it.id}") }
                channelDao.insertChannels(preparedChannels)

                // Mettre à jour l'index de recherche (FTS)
                channelDao.insertChannelsFts(preparedChannels.map {
                    ChannelFts(it.id, it.name, it.category, it.groupTitle)
                })

                // Sauvegarder la playlist
                val playlist = Playlist(
                    id = playlistId,
                    name = name,
                    sourceType = SourceType.M3U_URL,
                    url = cleanUrl,
                    channelCount = channels.size
                )
                playlistDao.insertPlaylist(playlist)

                // Charger l'EPG si une URL a été trouvée
                epgUrl?.let {
                    Timber.i("🚀 Lancement du téléchargement EPG automatique...")
                    epgRepository.fetchEpg(it)
                }

                // Mettre à jour le flag
                context.dataStore.edit { preferences ->
                    preferences[HAS_PLAYLIST] = true
                }

                Timber.i("✅ Playlist M3U ajoutée et mise en cache : $name (${channels.size} chaînes)")
                emit(PlaylistLoadProgress.Success(channels.size))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Échec addM3UPlaylist pour $name")
            emit(PlaylistLoadProgress.Error(e))
        }
    }

    /**
     * Ajoute une playlist M3U depuis un contenu déjà téléchargé (utilisé par DownloadProgressViewModel).
     * Évite un second téléchargement — le contenu M3U vient du flux progressif.
     * @return Result<Int> — nombre de chaînes importées
     */
    suspend fun addM3UPlaylistFromContent(
        name: String,
        url: String,
        content: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val playlistId = "playlist_${System.currentTimeMillis()}"
            val inputStream = content.byteInputStream(Charsets.UTF_8)
            var epgUrl: String? = null

            val channels = try {
                m3uParser.parseFromInputStream(
                    inputStream  = inputStream,
                    playlistId   = playlistId,
                    sourceUrl    = url,
                    onEpgUrlFound = { epgUrl = it }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur parsing content M3U")
                return@withContext Result.failure(Exception("Erreur lors du traitement de la playlist reçue."))
            }

            if (channels.isEmpty()) {
                return@withContext Result.failure(Exception("Aucune chaîne trouvée"))
            }

            val preparedChannels = channels.map { it.copy(id = "${playlistId}_${it.id}") }
            channelDao.insertChannels(preparedChannels)
            channelDao.insertChannelsFts(preparedChannels.map {
                ChannelFts(it.id, it.name, it.category, it.groupTitle)
            })

            playlistDao.insertPlaylist(
                Playlist(
                    id          = playlistId,
                    name        = name,
                    sourceType  = SourceType.M3U_URL,
                    url         = url,
                    channelCount = channels.size
                )
            )

            epgUrl?.let { epgRepository.fetchEpg(it) }

            context.dataStore.edit { prefs -> prefs[HAS_PLAYLIST] = true }

            Timber.i("✅ Playlist MAC importée: $name (${channels.size} chaînes)")
            Result.success(channels.size)
        } catch (e: Exception) {
            Timber.e(e, "❌ addM3UPlaylistFromContent échoué")
            Result.failure(e)
        }
    }

    /**
     * Ajoute une playlist Xtream Codes et charge automatiquement les chaînes
     * @return Flow<PlaylistLoadProgress> — progression et résultat
     */
    fun addXtreamPlaylist(
        name: String,
        username: String,
        password: String,
        serverUrl: String
    ): Flow<PlaylistLoadProgress> = flow {
        emit(PlaylistLoadProgress.Loading("Vérification des identifiants..."))
        try {
            val normalized = XtreamUrlNormalizer.normalize(
                rawInput = serverUrl,
                fallbackUsername = username,
                fallbackPassword = password
            )
            val resolvedUsername = normalized.username?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Nom d'utilisateur Xtream manquant")
            val resolvedPassword = normalized.password?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Mot de passe Xtream manquant")
            val cleanBaseUrl = normalized.serverUrl

            Timber.i("📥 Début ajout playlist Xtream : $name ($cleanBaseUrl)")
            // Vérifier si une playlist avec ce nom existe déjà
            val existingPlaylist = playlistDao.getPlaylistByName(name)
            if (existingPlaylist != null) {
                Timber.w("Une playlist avec le nom '$name' existe déjà")
                throw IllegalStateException("Une playlist avec ce nom existe déjà")
            }

            // Sauvegarder les credentials dans EncryptedSharedPreferences
            val playlistId = UUID.randomUUID().toString()
            saveXtreamCredentials(playlistId, resolvedUsername, resolvedPassword, cleanBaseUrl)

            // Créer la playlist sans exposer les credentials dans l'URL persistée
            val playlist = Playlist(
                id = playlistId,
                name = name,
                sourceType = SourceType.XTREAM_CODES,
                url = XtreamUrlNormalizer.apiUrl(cleanBaseUrl),
                username = null,
                password = null,
                baseUrl = null
            )

            playlistDao.insertPlaylist(playlist)
            Timber.d("Playlist Xtream ajoutée: $name avec credentials chiffrés")

            var totalChannels = 0

            // VÉRIFIER LES CREDENTIALS D'ABORD
            emit(PlaylistLoadProgress.Loading("Connexion au serveur IPTV..."))
            try {
                val authUrl = XtreamUrlNormalizer.apiUrl(cleanBaseUrl)
                Timber.i("🔐 Test authentification: $authUrl")

                val response = try {
                    xtreamApi.authenticate(
                        fullUrl = authUrl,
                        username = resolvedUsername,
                        password = resolvedPassword
                    )
                } catch (e: Exception) {
                    Timber.e(e, "❌ Erreur réseau lors de l'auth")
                    throw Exception("Impossible de contacter le serveur IPTV : ${e.localizedMessage}. Vérifiez votre connexion et l'URL.", e)
                }

                val authResponse = response.body()

                // Vérification de l'authentification avec gestion des différents formats
                val userInfo = authResponse?.userInfo

                // UserInfo null ou vide
                if (userInfo == null) {
                    Timber.e("❌ Réponse serveur sans user_info")
                    throw Exception("Le serveur n'a pas répondu correctement. Vérifiez que l'URL se termine par /player_api.php (ou le port correct).")
                }

                // Message d'erreur explicite du serveur
                if (!userInfo.message.isNullOrBlank()) {
                    Timber.e("❌ Message serveur: ${userInfo.message}")
                    throw Exception("Serveur IPTV : ${userInfo.message}")
                }

                // Vérification principale avec la propriété isAuthenticated
                if (!userInfo.isAuthenticated) {
                    Timber.e("❌ Authentification échouée pour $resolvedUsername - auth=${userInfo.auth}, status=${userInfo.status}")

                    // Message spécifique selon le status
                    val errorMsg = when {
                        userInfo.status?.equals("Expired", ignoreCase = true) == true ->
                            "Votre abonnement IPTV a expiré."
                        userInfo.status?.equals("Banned", ignoreCase = true) == true ->
                            "Votre compte IPTV a été banni."
                        userInfo.status?.equals("Disabled", ignoreCase = true) == true ->
                            "Votre compte IPTV est désactivé."
                        else -> "Identifiants Xtream incorrects (Nom d'utilisateur ou Mot de passe)."
                    }
                    throw Exception(errorMsg)
                }

                Timber.i("✅ Authentification réussie: ${userInfo.username}")

            } catch (e: Exception) {
                Timber.e(e, "❌ Échec authentification Xtream")
                throw Exception("Erreur de connexion : ${e.message}", e)
            }

            // CHARGER LES CHAINES depuis l'API Xtream
            emit(PlaylistLoadProgress.Loading("Récupération des chaînes live...", 0.25f))
            val liveChannels = try {
                val apiUrl = XtreamUrlNormalizer.apiUrl(cleanBaseUrl)
                Timber.i("🌐 Appel API Xtream: $apiUrl")
                Timber.i("👤 Username: $resolvedUsername")

                // Live Streams
                val streamsResponse = try {
                    xtreamApi.getStreams(
                        fullUrl = apiUrl,
                        username = resolvedUsername,
                        password = resolvedPassword,
                        action = "get_live_streams"
                    )
                } catch (e: Exception) {
                    Timber.e(e, "❌ Échec getLiveStreams")
                    throw Exception("Impossible de récupérer les chaînes. Vérifiez l'URL du serveur.", e)
                }

                if (!streamsResponse.isSuccessful) {
                    throw Exception("Erreur serveur Xtream (${streamsResponse.code()}) lors de la récupération des chaînes live")
                }

                val streams = try {
                    streamsResponse.body()?.let { XtreamCodesApi.parseStreamsStream(it) } ?: emptyList<XtreamStream>()
                } catch (e: Exception) {
                    Timber.e(e, "❌ Erreur parsing JSON streams")
                    throw Exception("Erreur lors du traitement des chaînes. Contactez votre fournisseur IPTV.", e)
                }
                Timber.i("✅ ${streams.size} streams récupérés depuis Xtream API")

                if (streams.isEmpty()) {
                    Timber.w("⚠️ Aucun stream retourné par l'API")
                    throw Exception("Le serveur n'a retourné aucune chaîne. Vérifiez l'URL et vos identifiants Xtream.")
                }

                // Récupérer les catégories avec timeout ou fallback
                val liveCategories: Map<String, String> = runCatching {
                    xtreamApi.getCategories(apiUrl, resolvedUsername, resolvedPassword, "get_live_categories")
                        .body()?.associate { it.id to it.name } ?: emptyMap()
                }.getOrElse {
                    Timber.w("⚠️ Échec récupération catégories live")
                    emptyMap()
                }

                streams.map { stream ->
                    stream.toChannel(
                        baseUrl = cleanBaseUrl,
                        username = resolvedUsername,
                        password = resolvedPassword,
                        playlistId = playlistId,
                        categoryName = liveCategories[stream.categoryId],
                        forcedType = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Échec global import Xtream")
                throw Exception("Erreur lors de l'importation : ${e.message}", e)
            }

            emit(PlaylistLoadProgress.Loading("Sauvegarde des chaînes live...", 0.5f))
            // Batch insert pour éviter de saturer la DB
            channelDao.insertChannels(liveChannels)
            channelDao.insertChannelsFts(liveChannels.map {
                ChannelFts(it.id, it.name, it.category, it.groupTitle)
            })
            totalChannels += liveChannels.size
            Timber.i("✅ ${liveChannels.size} chaînes live sauvegardées pour la playlist $name")

            // Mettre à jour le nombre de chaînes dans la playlist
            playlistDao.updateChannelCount(playlistId, totalChannels)

            // Indiquer qu'une playlist existe
            context.dataStore.edit { preferences ->
                preferences[HAS_PLAYLIST] = true
            }


            // RÉCUPÉRATION DES FILMS (VOD)
            emit(PlaylistLoadProgress.Loading("Récupération des films...", 0.55f))
            try {
                val apiUrl = XtreamUrlNormalizer.apiUrl(cleanBaseUrl)
                val vodStreams = runCatching {
                    xtreamApi.getVodStreams(apiUrl, resolvedUsername, resolvedPassword)
                }.getOrElse {
                    Timber.w("⚠️ Erreur VOD pour $playlistId: ${it.message}")
                    emptyList()
                }
                Timber.i("✅ ${vodStreams.size} films récupérés")

                if (vodStreams.isNotEmpty()) {
                    val vodCategories: Map<String, String> = runCatching {
                        xtreamApi.getVodCategories(apiUrl, resolvedUsername, resolvedPassword)
                            .associate { it.id to it.name }
                    }.getOrElse { emptyMap() }

                    val vodChannels: List<Channel> = vodStreams.map { stream ->
                        stream.toChannel(
                            baseUrl = cleanBaseUrl,
                            username = resolvedUsername,
                            password = resolvedPassword,
                            playlistId = playlistId,
                            categoryName = vodCategories[stream.categoryId],
                            forcedType = ContentType.VOD_MOVIE
                        )
                    }

                    channelDao.insertChannels(vodChannels)
                    channelDao.insertChannelsFts(vodChannels.map {
                        ChannelFts(it.id, it.name, it.category, it.groupTitle)
                    })
                    totalChannels += vodChannels.size
                    playlistDao.updateChannelCount(playlistId, totalChannels)
                    Timber.i("✅ ${vodChannels.size} films sauvegardés")
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Erreur chargement films (non critique)")
            }

            // RÉCUPÉRATION DES SÉRIES (Metadata)
            emit(PlaylistLoadProgress.Loading("Récupération des séries...", 0.75f))
            try {
                Timber.i("📺 Chargement séries...")
                val apiUrl = XtreamUrlNormalizer.apiUrl(cleanBaseUrl)
                val seriesResponse = try {
                    xtreamApi.getSeriesList(
                        fullUrl = apiUrl,
                        username = resolvedUsername,
                        password = resolvedPassword,
                        action = "get_series"
                    )
                } catch (e: Exception) {
                    Timber.e(e, "❌ Échec getSeriesList")
                    null
                }

                val series = seriesResponse?.body()?.let { XtreamCodesApi.parseSeriesStream(it) } ?: emptyList<XtreamSeries>()
                Timber.i("✅ ${series.size} séries récupérées")

                if (series.isNotEmpty()) {
                    val seriesCategories: Map<String, String> = runCatching {
                        xtreamApi.getCategories(apiUrl, resolvedUsername, resolvedPassword, "get_series_categories")
                            .body()?.associate { it.id to it.name } ?: emptyMap()
                    }.getOrElse { emptyMap() }

                    val metadataDao = database.contentMetadataDao()
                    val seriesChannels: List<Channel> = series.map { item ->
                        val resolvedCategory = com.skyplayer.pro.data.organizer.ContentClassifier.inferCategory(
                            name = item.name,
                            groupTitle = seriesCategories[item.categoryId],
                            contentType = ContentType.VOD_SERIES
                        )
                        Channel(
                            id = "${playlistId}_series_${item.seriesId}",
                            name = item.name,
                            url = XtreamCodesApi.buildSeriesUrl(cleanBaseUrl, resolvedUsername, resolvedPassword, item.seriesId.toString()),
                            logoUrl = item.cover,
                            category = resolvedCategory,
                            type = ContentType.VOD_SERIES,
                            groupTitle = resolvedCategory
                        )
                    }

                    channelDao.insertChannels(seriesChannels)
                    channelDao.insertChannelsFts(seriesChannels.map {
                        ChannelFts(it.id, it.name, it.category, it.groupTitle)
                    })

                    // Sauvegarder les métadonnées pour affichage riche
                    val metadataList = series.map { item ->
                        item.toContentMetadata("${playlistId}_series_${item.seriesId}")
                    }
                    metadataDao.insertAllMetadata(metadataList)
                    Timber.i("✅ ${seriesChannels.size} séries sauvegardées")
                    totalChannels += seriesChannels.size
                    playlistDao.updateChannelCount(playlistId, totalChannels)
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Erreur chargement séries (non critique)")
            }

            emit(PlaylistLoadProgress.Success(totalChannels))
        } catch (e: Exception) {
            Timber.e(e, "❌ addXtreamPlaylist échoué")
            emit(PlaylistLoadProgress.Error(e))
        }
    }

    /**
     * Supprime une playlist et ses chaînes associées
     */
    suspend fun deletePlaylist(playlistId: String) {
        withContext(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
            channelDao.deleteChannelsByPlaylistId(playlistId)
            deleteXtreamCredentials(playlistId)

            // Mettre à jour le flag si plus de playlists
            val count = playlistDao.getPlaylistCount()
            if (count == 0) {
                context.dataStore.edit { preferences ->
                    preferences[HAS_PLAYLIST] = false
                }
            }
        }
    }

    /**
     * Rafraîchit les données d'une playlist (Metadata Sync)
     */
    suspend fun refreshPlaylist(playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext Result.failure(Exception("Playlist introuvable"))

            val newChannels: List<Channel> = when (playlist.sourceType) {
                SourceType.M3U_URL -> {
                    val url = playlist.url ?: return@withContext Result.failure(Exception("URL M3U manquante"))
                    m3uParser.parseFromUrl(url, playlistId)
                }
                SourceType.XTREAM_CODES -> {
                    val creds = hydrateXtreamCredentials(playlist)
                    val username = creds.username ?: return@withContext Result.failure(Exception("Nom d'utilisateur manquant"))
                    val password = creds.password ?: return@withContext Result.failure(Exception("Mot de passe manquant"))
                    val serverUrl = creds.baseUrl ?: return@withContext Result.failure(Exception("URL serveur manquante"))

                    val cleanBaseUrl = serverUrl.trim().trimEnd('/')
                    val apiUrl = "$cleanBaseUrl/player_api.php"
                    val liveCategories = runCatching {
                        xtreamApi.getLiveCategories(apiUrl, username, password).associate { it.id to it.name }
                    }.getOrElse { emptyMap() }
                    val vodCategories = runCatching {
                        xtreamApi.getVodCategories(apiUrl, username, password).associate { it.id to it.name }
                    }.getOrElse { emptyMap() }
                    val seriesCategories = runCatching {
                        xtreamApi.getSeriesCategories(apiUrl, username, password).associate { it.id to it.name }
                    }.getOrElse { emptyMap() }

                    val streams = xtreamApi.getLiveStreams(apiUrl, username, password)
                    val vodStreams = runCatching {
                        xtreamApi.getVodStreams(apiUrl, username, password)
                    }.getOrElse {
                        Timber.w("Erreur VOD pour $playlistId: ${it.message}")
                        emptyList()
                    }
                    val series = runCatching {
                        xtreamApi.getSeries(apiUrl, username, password)
                    }.getOrElse {
                        Timber.w("Erreur séries pour $playlistId: ${it.message}")
                        emptyList()
                    }

                    val combined = mutableListOf<Channel>()
                    combined.addAll(streams.map {
                        it.toChannel(serverUrl, username, password, playlistId, liveCategories[it.categoryId], null)
                    })
                    combined.addAll(vodStreams.map {
                        it.toChannel(serverUrl, username, password, playlistId, vodCategories[it.categoryId], ContentType.VOD_MOVIE)
                    })
                    combined.addAll(series.map { item ->
                        val resolvedCategory = com.skyplayer.pro.data.organizer.ContentClassifier.inferCategory(
                            name = item.name,
                            groupTitle = seriesCategories[item.categoryId],
                            contentType = ContentType.VOD_SERIES
                        )
                        Channel(
                            id = "${playlistId}_series_${item.seriesId}",
                            name = item.name,
                            url = XtreamCodesApi.buildSeriesUrl(serverUrl, username, password, item.seriesId.toString()),
                            logoUrl = item.cover,
                            category = resolvedCategory,
                            type = ContentType.VOD_SERIES,
                            groupTitle = resolvedCategory
                        )
                    })
                    combined
                }
                else -> emptyList()
            }

            if (newChannels.isNotEmpty()) {
                // Transaction atomique : suppression + réinsertion + FTS + compteur
                // garantissent qu'un refresh interrompu ne laisse jamais la base
                // dans un état partiel (dédoublonnage assuré par delete + reinsert).
                database.withTransaction {
                    channelDao.deleteChannelsByPlaylistId(playlistId)

                    // Chunking pour éviter les erreurs SQLite sur de très grosses listes
                    newChannels.chunked(500).forEach { chunk ->
                        val preparedChunk = chunk.map { channel ->
                            if (channel.id.startsWith(playlistId)) channel
                            else channel.copy(id = "${playlistId}_${channel.id}")
                        }
                        channelDao.insertChannels(preparedChunk)

                        // Mettre à jour l'index de recherche (FTS)
                        channelDao.insertChannelsFts(preparedChunk.map {
                            ChannelFts(it.id, it.name, it.category, it.groupTitle)
                        })
                    }

                    // Mettre à jour le compte et le timestamp
                    playlistDao.updateChannelCount(playlistId, newChannels.size)
                    playlistDao.updateTimestamp(playlistId)
                }

                Timber.i("✅ Playlist $playlistId synchronisée : ${newChannels.size} éléments")
                Result.success(Unit)
            } else {
                Result.failure(Exception("La source distante est vide ou inaccessible"))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Échec synchronisation playlist $playlistId")
            Result.failure(e)
        }
    }

    // Méthode générique pour ajouter une playlist
    fun addPlaylist(
        name: String,
        url: String,
        type: String,
        username: String? = null,
        password: String? = null,
        serverUrl: String? = null
    ): Flow<PlaylistLoadProgress> {
        return when (type.uppercase()) {
            "M3U" -> {
                addM3UPlaylist(name, url)
            }
            "XTREAM" -> {
                if (username != null && password != null && serverUrl != null) {
                    addXtreamPlaylist(name, username, password, serverUrl)
                } else {
                    flow {
                        emit(PlaylistLoadProgress.Error(IllegalArgumentException("Credentials Xtream manquants")))
                    }
                }
            }
            else -> {
                flow {
                    emit(PlaylistLoadProgress.Error(IllegalArgumentException("Type de playlist non supporté: $type")))
                }
            }
        }
    }

    private fun saveXtreamCredentials(playlistId: String, username: String, password: String, baseUrl: String) {
        secureXtreamPrefs.edit()
            .putString("$playlistId.username", username)
            .putString("$playlistId.password", password)
            .putString("$playlistId.baseUrl", baseUrl)
            .apply()
    }

    private fun deleteXtreamCredentials(playlistId: String) {
        secureXtreamPrefs.edit()
            .remove("$playlistId.username")
            .remove("$playlistId.password")
            .remove("$playlistId.baseUrl")
            .apply()
    }

    private fun hydrateXtreamCredentials(playlist: Playlist): Playlist {
        if (playlist.sourceType != SourceType.XTREAM_CODES) return playlist
        return playlist.copy(
            username = secureXtreamPrefs.getString("${playlist.id}.username", null),
            password = secureXtreamPrefs.getString("${playlist.id}.password", null),
            baseUrl = secureXtreamPrefs.getString("${playlist.id}.baseUrl", null)
        )
    }

    /**
     * Récupère le nombre de playlists
     */
    suspend fun getPlaylistCount(): Int {
        return playlistDao.getPlaylistCount()
    }

    /**
     * Rafraîchit l'EPG pour la playlist active si une source EPG exploitable est disponible.
     * Pour les playlists M3U, l'URL EPG est relue depuis l'en-tête #EXTM3U.
     * Pour Xtream, aucun endpoint global n'est actuellement géré ici.
     */
    suspend fun refreshActivePlaylistEpg(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val playlists = getAllPlaylists().first()
            val activePlaylist = playlists.firstOrNull { it.isActive } ?: playlists.firstOrNull()
                ?: return@withContext Result.failure(Exception("Aucune playlist configurée"))

            when (activePlaylist.sourceType) {
                SourceType.M3U_URL -> {
                    val playlistUrl = activePlaylist.url
                        ?: return@withContext Result.failure(Exception("URL playlist manquante"))
                    val epgUrl = extractEpgUrlFromM3uHeader(playlistUrl)
                        ?: return@withContext Result.failure(Exception("Aucune URL EPG trouvée dans la playlist active"))

                    Timber.i("🔄 Rafraîchissement EPG depuis $epgUrl")
                    epgRepository.fetchEpg(epgUrl)
                }
                SourceType.XTREAM_CODES -> {
                    Result.failure(Exception("Le rafraîchissement EPG global Xtream n'est pas encore pris en charge"))
                }
                SourceType.M3U_FILE -> {
                    Result.failure(Exception("Le rafraîchissement EPG pour fichier local n'est pas encore pris en charge"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Rafraîchissement EPG impossible")
            Result.failure(e)
        }
    }

    private suspend fun extractEpgUrlFromM3uHeader(playlistUrl: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(playlistUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Body vide")
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                val headerLine = reader.readLine()?.trim().orEmpty()
                if (!headerLine.startsWith("#EXTM3U", ignoreCase = true)) {
                    return@withContext null
                }

                val epgRegex = "(?:url-tvg|x-tvg-url)=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
                return@withContext epgRegex.find(headerLine)?.groupValues?.getOrNull(1)
            }
        }
    }
}
