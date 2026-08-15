package com.skyplayer.pro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.local.PlaylistDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ChannelFts
import com.skyplayer.pro.data.model.Playlist
import com.skyplayer.pro.data.parser.M3UParserFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playlist_refresh")

/**
 * Repository avec rafraîchissement intelligent des playlists
 *
 * Stratégie :
 * 1. Charge immédiatement depuis la base locale (démarrage instantané)
 * 2. Vérifie en arrière-plan si une mise à jour est nécessaire
 * 3. Rafraîchit silencieusement si l'URL a changé ou si le délai est écoulé
 */
@Singleton
class SmartPlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val REFRESH_INTERVAL_HOURS = 24 // Rafraîchir toutes les 24h

        // Keys DataStore
        private val LAST_REFRESH_KEY = longPreferencesKey("last_refresh_timestamp")
        private val LAST_URL_HASH_KEY = longPreferencesKey("last_url_hash")
    }

    private val parser = M3UParserFlow()

    // === ÉTAT DU RAFRAÎCHISSEMENT ===

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Charge les chaînes de manière intelligente :
     * 1. Émet immédiatement les chaînes depuis la base locale
     * 2. Vérifie en parallèle si une mise à jour est nécessaire
     * 3. Rafraîchit et ré-émet si nouvelles données disponibles
     *
     * @param playlist La playlist à charger
     * @return Flow<List<Channel>> Flux de chaînes (local puis éventuellement mis à jour)
     */
    fun loadChannelsSmart(playlist: Playlist): Flow<List<Channel>> = flow {
        // 1. CHARGEMENT INSTANTANÉ depuis la base locale
        val localChannels = loadChannelsFromLocal(playlist.id)
        emit(localChannels)
        Timber.i("⚡ Démarrage instantané: ${localChannels.size} chaînes depuis la base locale")

        // 2. VÉRIFICATION en arrière-plan si rafraîchissement nécessaire
        if (shouldRefresh(playlist)) {
            _isRefreshing.value = true
            _refreshState.value = RefreshState.CheckingForUpdates

            try {
                // 3. RAFRAÎCHISSEMENT silencieux
                refreshPlaylist(playlist)
                    .collect { batch ->
                        // Accumuler et émettre les nouvelles chaînes
                        val updatedChannels = localChannels + batch
                        emit(updatedChannels)
                    }

                _refreshState.value = RefreshState.Success
                Timber.i("✅ Playlist rafraîchie avec succès")
            } catch (e: Exception) {
                Timber.e(e, "❌ Erreur rafraîchissement playlist")
                _refreshState.value = RefreshState.Error(e.message ?: "Erreur inconnue")
                // On garde les données locales en cas d'erreur
            } finally {
                _isRefreshing.value = false
            }
        } else {
            Timber.d("📦 Données locales à jour, pas de rafraîchissement nécessaire")
            _refreshState.value = RefreshState.UpToDate
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Charge uniquement depuis la base locale (démarrage rapide)
     */
    private suspend fun loadChannelsFromLocal(playlistId: String): List<Channel> {
        val channels = channelDao.getChannelsByTypePlaylistId(playlistId).first()
        Timber.d("📦 ${channels.size} chaînes chargées depuis la base locale")
        return channels
    }

    /**
     * Détermine si un rafraîchissement est nécessaire
     */
    private suspend fun shouldRefresh(playlist: Playlist): Boolean {
        val lastRefresh = context.dataStore.data.map { it[LAST_REFRESH_KEY] ?: 0L }.first()
        val lastUrlHash = context.dataStore.data.map { it[LAST_URL_HASH_KEY] ?: 0L }.first()

        val currentUrlHash = playlist.url?.hashCode()?.toLong() ?: 0L
        val now = System.currentTimeMillis()
        val hoursSinceRefresh = TimeUnit.MILLISECONDS.toHours(now - lastRefresh)

        // Conditions de rafraîchissement :
        // 1. Jamais rafraîchi (lastRefresh = 0)
        // 2. URL a changé (hash différent)
        // 3. Plus de 24h depuis le dernier rafraîchissement
        return when {
            lastRefresh == 0L -> {
                Timber.d("🔄 Premier rafraîchissement nécessaire")
                true
            }
            lastUrlHash != currentUrlHash -> {
                Timber.d("🔄 URL modifiée, rafraîchissement nécessaire")
                true
            }
            hoursSinceRefresh >= REFRESH_INTERVAL_HOURS -> {
                Timber.d("🔄 Délai de $hoursSinceRefresh heures écoulé, rafraîchissement nécessaire")
                true
            }
            else -> false
        }
    }

    /**
     * Rafraîchit la playlist depuis l'URL M3U
     * Émet les chaînes par paquets pour mise à jour progressive de l'UI
     */
    private fun refreshPlaylist(playlist: Playlist): Flow<List<Channel>> = flow {
        if (playlist.url == null) {
            emit(emptyList())
            return@flow
        }

        val playlistId = playlist.id
        val startTime = System.currentTimeMillis()
        var totalChannels = 0
        val allChannels = mutableListOf<Channel>()

        _refreshState.value = RefreshState.Downloading

        // Parser et émettre par paquets
        parser.parseFromUrlAsFlow(playlist.url, playlistId, okHttpClient)
            .collect { batch ->
                totalChannels += batch.size
                allChannels.addAll(batch)

                // Sauvegarder chaque batch dans la base locale
                channelDao.insertChannels(batch)

                // Mettre à jour l'index de recherche (FTS)
                channelDao.insertChannelsFts(batch.map {
                    ChannelFts(it.id, it.name, it.category, it.groupTitle)
                })

                // Émettre pour mise à jour UI
                emit(batch)

                Timber.d("📦 Batch reçu: ${batch.size} chaînes (Total: $totalChannels)")
            }

        // Mettre à jour les métadonnées
        updateRefreshMetadata(playlist)

        // Mettre à jour le compteur de chaînes dans la playlist
        playlistDao.updateChannelCount(playlistId, totalChannels)

        val duration = System.currentTimeMillis() - startTime
        Timber.i("✅ Rafraîchissement terminé: $totalChannels chaînes en ${duration}ms")
    }.flowOn(Dispatchers.IO)

    /**
     * Force un rafraîchissement manuel (swipe-to-refresh)
     */
    suspend fun forceRefresh(playlist: Playlist) {
        _isRefreshing.value = true
        _refreshState.value = RefreshState.Downloading

        try {
            // Supprimer les anciennes chaînes
            channelDao.deleteChannelsByPlaylistId(playlist.id)

            // Re-parser complètement
            refreshPlaylist(playlist).collect { _ -> }

            _refreshState.value = RefreshState.Success
        } catch (e: Exception) {
            _refreshState.value = RefreshState.Error(e.message ?: "Erreur")
            throw e
        } finally {
            _isRefreshing.value = false
        }
    }

    /**
     * Met à jour les métadonnées de rafraîchissement
     */
    private suspend fun updateRefreshMetadata(playlist: Playlist) {
        context.dataStore.edit { prefs ->
            prefs[LAST_REFRESH_KEY] = System.currentTimeMillis()
            prefs[LAST_URL_HASH_KEY] = playlist.url?.hashCode()?.toLong() ?: 0L
        }
        playlistDao.updateTimestamp(playlist.id)
    }

    /**
     * Précharge une playlist (utile pour le premier lancement)
     * Sans émettre de résultats intermédiaires
     */
    suspend fun preloadPlaylist(playlist: Playlist): Result<Int> {
        return try {
            if (playlist.url == null) {
                return Result.failure(Exception("URL manquante"))
            }

            var count = 0
            parser.parseFromUrlAsFlow(playlist.url, playlist.id, okHttpClient)
                .collect { batch ->
                    channelDao.insertChannels(batch)
                    count += batch.size
                }

            updateRefreshMetadata(playlist)
            playlistDao.updateChannelCount(playlist.id, count)

            Timber.i("✅ Préchargement terminé: $count chaînes")
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur préchargement")
            Result.failure(e)
        }
    }
}

/**
 * États du rafraîchissement
 */
sealed class RefreshState {
    object Idle : RefreshState()
    object UpToDate : RefreshState() // Données locales à jour
    object CheckingForUpdates : RefreshState() // Vérification en cours
    object Downloading : RefreshState() // Téléchargement en cours
    object Success : RefreshState() // Rafraîchissement réussi
    data class Error(val message: String) : RefreshState()
}
