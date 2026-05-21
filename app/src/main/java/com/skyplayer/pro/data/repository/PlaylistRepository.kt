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
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.model.SourceType
import com.skyplayer.pro.data.parser.M3UParser
import com.skyplayer.pro.data.remote.XtreamCodesApi
import com.skyplayer.pro.data.remote.toChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID

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
    private val okHttpClient: OkHttpClient
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
    
    // Ajouter une playlist M3U
    suspend fun addM3UPlaylist(name: String, url: String): Result<Unit> {
        return try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return Result.failure(IllegalArgumentException("L'URL doit commencer par http:// ou https://"))
            }

            // Valider et parser la playlist
            var epgUrl: String? = null
            val channels = m3uParser.parseFromInputStream(
                inputStream = okHttpClient.newCall(Request.Builder().url(url).build()).execute().body!!.byteStream(),
                playlistId = "temp",
                sourceUrl = url,
                onEpgUrlFound = { epgUrl = it }
            )
            
            if (channels.isEmpty()) {
                Result.failure(Exception("Aucune chaîne trouvée dans la playlist"))
            } else {
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
                    url = url,
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
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
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

            val channels = m3uParser.parseFromInputStream(
                inputStream  = inputStream,
                playlistId   = playlistId,
                sourceUrl    = url,
                onEpgUrlFound = { epgUrl = it }
            )

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
     */
    suspend fun addXtreamPlaylist(
        name: String,
        username: String,
        password: String,
        serverUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier si une playlist avec ce nom existe déjà
            val existingPlaylist = playlistDao.getPlaylistByName(name)
            if (existingPlaylist != null) {
                Timber.w("Une playlist avec le nom '$name' existe déjà")
                return@withContext Result.failure(IllegalStateException("Une playlist avec ce nom existe déjà"))
            }

            // Sauvegarder les credentials dans EncryptedSharedPreferences
            val playlistId = UUID.randomUUID().toString()
            saveXtreamCredentials(playlistId, username, password, serverUrl)

            // Créer la playlist avec les credentials chiffrés
            val playlist = Playlist(
                id = playlistId,
                name = name,
                sourceType = SourceType.XTREAM_CODES,
                url = "$serverUrl/player_api.php?username=$username&password=$password",
                username = null,
                password = null,
                baseUrl = null
            )

            playlistDao.insertPlaylist(playlist)
            Timber.d("Playlist Xtream ajoutée: $name avec credentials chiffrés")

            // VÉRIFIER LES CREDENTIALS D'ABORD
            try {
                val authUrl = "$serverUrl/player_api.php"
                Timber.i("🔐 Test authentification: $authUrl")

                val authResponse = xtreamApi.authenticate(
                    fullUrl = authUrl,
                    username = username,
                    password = password
                )

                // Vérification de l'authentification avec gestion des différents formats
                val userInfo = authResponse.userInfo
                
                // Message d'erreur explicite du serveur
                if (!userInfo?.message.isNullOrBlank()) {
                    Timber.e("❌ Message serveur: ${userInfo?.message}")
                    return@withContext Result.failure(Exception("Erreur serveur: ${userInfo?.message}"))
                }
                
                // UserInfo null ou vide
                if (userInfo == null) {
                    Timber.e("❌ Réponse serveur sans user_info")
                    return@withContext Result.failure(Exception("Réponse invalide du serveur. Vérifiez l'URL."))
                }
                
                // Vérification principale avec la propriété isAuthenticated
                if (!userInfo.isAuthenticated) {
                    Timber.e("❌ Authentification échouée pour $username - auth=${userInfo.auth}, status=${userInfo.status}")
                    
                    // Message spécifique selon le status
                    val errorMsg = when {
                        userInfo.status?.equals("Expired", ignoreCase = true) == true -> 
                            "Votre abonnement a expiré. Contactez votre fournisseur."
                        userInfo.status?.equals("Banned", ignoreCase = true) == true -> 
                            "Compte suspendu. Contactez votre fournisseur."
                        userInfo.status?.equals("Disabled", ignoreCase = true) == true -> 
                            "Compte désactivé. Contactez votre fournisseur."
                        else -> "Credentials invalides. Vérifiez votre nom d'utilisateur et mot de passe."
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                Timber.i("✅ Authentification réussie: ${authResponse.userInfo?.username}")
                Timber.i("📊 Status compte: ${authResponse.userInfo?.status}")
                Timber.i("⏰ Expire le: ${authResponse.userInfo?.expDate ?: "Jamais"}")

            } catch (e: Exception) {
                Timber.e(e, "❌ Échec authentification Xtream")
                return@withContext Result.failure(Exception("Impossible de se connecter au serveur. Vérifiez l'URL et vos credentials. Erreur: ${e.message}"))
            }

            // CHARGER LES CHAINES depuis l'API Xtream
            try {
                // Construire l'URL complète pour l'API
                val apiUrl = "$serverUrl/player_api.php"
                Timber.i("🌐 Appel API Xtream: $apiUrl")
                Timber.i("👤 Username: $username")

                val streams = xtreamApi.getLiveStreams(
                    fullUrl = apiUrl,
                    username = username,
                    password = password
                )
                Timber.i("✅ ${streams.size} streams récupérés depuis Xtream API")

                if (streams.isEmpty()) {
                    Timber.w("⚠️ Aucun stream retourné par l'API")
                    return@withContext Result.failure(Exception("Aucune chaîne trouvée sur ce serveur."))
                }

                // Convertir en Channel en utilisant l'extension toChannel()
                val channels = streams.map { stream ->
                    stream.toChannel(serverUrl, username, password, playlistId)
                }

                // Sauvegarder les chaînes
                channelDao.insertChannels(channels)
                
                // Mettre à jour l'index de recherche (FTS)
                channelDao.insertChannelsFts(channels.map { 
                    ChannelFts(it.id, it.name, it.category, it.groupTitle)
                })
                
                Timber.i("✅ ${channels.size} chaînes sauvegardées pour la playlist $name")

                // CHARGER AUSSI LES VOD
                try {
                    Timber.i("🎬 Chargement VOD...")
                    val vodStreams = xtreamApi.getVodStreams(
                        fullUrl = apiUrl,
                        username = username,
                        password = password
                    )
                    Timber.i("✅ ${vodStreams.size} films VOD récupérés")

                    if (vodStreams.isNotEmpty()) {
                        val vodChannels = vodStreams.map { stream ->
                            stream.toChannel(serverUrl, username, password, playlistId, isVod = true)
                        }
                        channelDao.insertChannels(vodChannels)
                        
                        // Mettre à jour l'index de recherche (FTS)
                        channelDao.insertChannelsFts(vodChannels.map { 
                            ChannelFts(it.id, it.name, it.category, it.groupTitle)
                        })

                        Timber.i("✅ ${vodChannels.size} films VOD sauvegardés")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ Erreur chargement VOD (non critique)")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur lors du chargement des chaînes Xtream pour $name")
                return@withContext Result.failure(Exception("Erreur chargement chaînes: ${e.message}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Erreur lors de l'ajout de la playlist Xtream: $name")
            Result.failure(e)
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
                    
                    val apiUrl = "$serverUrl/player_api.php"
                    val streams = xtreamApi.getLiveStreams(apiUrl, username, password)
                    val vodStreams = try {
                        xtreamApi.getVodStreams(apiUrl, username, password)
                    } catch (e: Exception) { 
                        Timber.w("Erreur VOD pour $playlistId: ${e.message}")
                        emptyList() 
                    }
                    
                    val combined = mutableListOf<Channel>()
                    combined.addAll(streams.map { it.toChannel(serverUrl, username, password, playlistId, isVod = false) })
                    combined.addAll(vodStreams.map { it.toChannel(serverUrl, username, password, playlistId, isVod = true) })
                    combined
                }
                else -> emptyList()
            }

            if (newChannels.isNotEmpty()) {
                // Utiliser une transaction pour la rapidité
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
    suspend fun addPlaylist(
        name: String,
        url: String,
        type: String,
        username: String? = null,
        password: String? = null,
        serverUrl: String? = null
    ): Result<Unit> {
        return when (type.uppercase()) {
            "M3U" -> {
                addM3UPlaylist(name, url)
            }
            "XTREAM" -> {
                if (username != null && password != null && serverUrl != null) {
                    addXtreamPlaylist(name, username, password, serverUrl)
                } else {
                    Result.failure(IllegalArgumentException("Credentials Xtream manquants"))
                }
            }
            else -> {
                Result.failure(IllegalArgumentException("Type de playlist non supporté: $type"))
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
}
