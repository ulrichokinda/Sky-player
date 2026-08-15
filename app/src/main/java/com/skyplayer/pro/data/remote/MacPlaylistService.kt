package com.skyplayer.pro.data.remote

import com.google.gson.annotations.SerializedName
import com.skyplayer.pro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service de liaison MAC → Playlist via le backend Sky-player
 *
 * Flux complet :
 *  1. GET /api/v1/playlist/{mac}  → vérifie si une playlist est associée à la MAC
 *  2. Si active → télécharge le fichier M3U par flux avec rapport de progression
 *
 * Endpoint : {BACKEND_BASE_URL}/api/v1/playlist/{mac}
 */
@Singleton
class MacPlaylistService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        // Utilisation de l'URL injectée via BuildConfig pour cohérence
        private val BASE_URL = BuildConfig.BACKEND_BASE_URL.trimEnd('/') + "/"
        private const val APP_KEY   = "skyplayer_pro"
        private const val TIMEOUT_S = 15L
        private const val BUFFER_SIZE = 8192   // 8 Ko par lecture
    }

    // Client HTTP dédié avec timeouts adaptés
    private val httpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)     // Long pour les gros M3U
            .writeTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Vérifie si une playlist est assignée à cette adresse MAC.
     * @return [MacPlaylistResult] — Active ou NoPlaylist ou Error
     */
    suspend fun checkMacPlaylist(macAddress: String): MacPlaylistResult =
        withContext(Dispatchers.IO) {
            try {
                // Endpoint du backend Sky-player : GET /api/v1/playlist/{mac}
                val request = Request.Builder()
                    .url("${BASE_URL}api/v1/playlist/$macAddress")
                    .get()
                    .addHeader("X-App-Key", APP_KEY)
                    .addHeader("X-Activation-API-Key", BuildConfig.LICENSE_API_KEY)
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.w("⚠️ /api/v1/playlist HTTP ${response.code}")
                    return@withContext MacPlaylistResult.NetworkError("HTTP ${response.code}")
                }

                val body = response.body?.string()
                    ?: return@withContext MacPlaylistResult.NetworkError("Corps de réponse vide")

                parsePlaylistResponse(body)

            } catch (e: Exception) {
                Timber.w("⚠️ checkMacPlaylist: ${e.message}")
                MacPlaylistResult.NetworkError(e.message ?: "Erreur réseau")
            }
        }

    /**
     * Télécharge le fichier M3U depuis l'URL fournie avec rapport de progression.
     *
     * Émet des [DownloadProgress] à chaque lecture de bloc (BUFFER_SIZE octets).
     * Le dernier état émis est [DownloadProgress.Done] avec le contenu M3U complet.
     *
     * Usage :
     * ```kotlin
     * macPlaylistService.downloadPlaylistWithProgress(url).collect { progress ->
     *     when (progress) {
     *         is DownloadProgress.Downloading -> updateUI(progress.readMb, progress.totalMb)
     *         is DownloadProgress.Done        -> parseM3U(progress.content)
     *         is DownloadProgress.Failed      -> showError(progress.error)
     *     }
     * }
     * ```
     */
    fun downloadPlaylistWithProgress(url: String): Flow<DownloadProgress> = flow {

        emit(DownloadProgress.Downloading(readBytes = 0, totalBytes = -1))

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain, */*")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadProgress.Failed("HTTP ${response.code}"))
                return@flow
            }

            val responseBody = response.body ?: run {
                emit(DownloadProgress.Failed("Corps de réponse vide"))
                return@flow
            }

            // Content-Length peut être -1 si inconnu (chunked transfer)
            val totalBytes = responseBody.contentLength()

            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalRead = 0L

            responseBody.byteStream().use { inputStream ->
                while (true) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    emit(
                        DownloadProgress.Downloading(
                            readBytes  = totalRead,
                            totalBytes = totalBytes
                        )
                    )
                }
            }

            val content = outputStream.toString("UTF-8")
            Timber.i("✅ Playlist téléchargée: ${(totalRead / 1024.0 / 1024.0).format1f} Mo")
            emit(DownloadProgress.Done(content = content, totalBytes = totalRead))

        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur téléchargement playlist")
            emit(DownloadProgress.Failed(e.message ?: "Erreur de téléchargement"))
        }

    }.flowOn(Dispatchers.IO)

    // ── Parsing JSON réponse /api/v1/playlist (backend Sky-player) ────────────
    private fun parsePlaylistResponse(json: String): MacPlaylistResult {
        return try {
            val obj = JSONObject(json)

            if (!obj.optBoolean("active", false)) {
                Timber.i("🌐 /api/v1/playlist: aucune playlist active (${obj.optString("message")})")
                return MacPlaylistResult.NoPlaylist
            }

            // Compat snake_case (playlist_url) et camelCase (playlistUrl)
            val playlistUrl = obj.optString("playlist_url", "").ifBlank { obj.optString("playlistUrl", "") }
            val xtreamHost = listOf("xtream_host", "xtreamServer")
                .map { obj.optString(it, "") }
                .firstOrNull { it.isNotBlank() }
            val xtreamUser = obj.optString("xtream_username", "").ifBlank { obj.optString("xtreamUser", "") }
            val xtreamPass = obj.optString("xtream_password", "").ifBlank { obj.optString("xtreamPassword", "") }

            // Le backend ne renvoie pas de champ "type" : on le déduit des credentials Xtream
            val type = if (xtreamHost != null && xtreamUser.isNotBlank()) "xtream" else "m3u"

            MacPlaylistResult.Active(
                info = MacPlaylistInfo(
                    name       = obj.optString("name", "Ma Playlist"),
                    url        = playlistUrl,
                    type       = type,
                    expireDate = obj.optString("expire", ""),
                    xtreamUsername   = xtreamUser,
                    xtreamPassword   = xtreamPass,
                    xtreamServerUrl  = xtreamHost ?: ""
                )
            ).also { Timber.i("🌐 /api/v1/playlist: playlist active ($type)") }
        } catch (e: Exception) {
            Timber.e(e, "❌ Parsing /api/v1/playlist échoué: $json")
            MacPlaylistResult.NetworkError("Parsing JSON échoué")
        }
    }
}

// ── Extension formatage ───────────────────────────────────────────────────────
private val Double.format1f: String
    get() = "%.1f".format(this)

// ── Modèles de données ────────────────────────────────────────────────────────

/** Résultat de la vérification MAC sur le serveur */
sealed class MacPlaylistResult {
    /** Le serveur a trouvé une playlist active pour cette MAC */
    data class Active(val info: MacPlaylistInfo) : MacPlaylistResult()

    /** Aucune playlist associée à cette MAC (ou expirée) */
    object NoPlaylist : MacPlaylistResult()

    /** Erreur réseau ou serveur injoignable */
    data class NetworkError(val message: String) : MacPlaylistResult()
}

/** Informations de la playlist renvoyées par le serveur */
@androidx.annotation.Keep
data class MacPlaylistInfo(
    val name: String,
    val url: String,
    val type: String,           // "m3u" ou "xtream"
    @SerializedName("expire") val expireDate: String,
    @SerializedName("xtream_username") val xtreamUsername: String,
    @SerializedName("xtream_password") val xtreamPassword: String,
    @SerializedName("xtream_server_url") val xtreamServerUrl: String
)

/** États du téléchargement progressif */
sealed class DownloadProgress {
    /**
     * Téléchargement en cours
     * @param readBytes  Octets déjà lus
     * @param totalBytes Taille totale (-1 si inconnue / chunked)
     */
    data class Downloading(
        val readBytes: Long,
        val totalBytes: Long
    ) : DownloadProgress() {
        /** Progression en Mo lus */
        val readMb: Float  get() = readBytes  / 1_048_576f
        /** Taille totale en Mo (-1 si inconnue) */
        val totalMb: Float get() = totalBytes / 1_048_576f
        /** Pourcentage 0..100 (-1 si taille inconnue) */
        val percent: Int   get() = if (totalBytes > 0)
            ((readBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
            else -1
        /** Texte formaté "X.X Mo / Y.Y Mo" ou "X.X Mo" si taille inconnue */
        val label: String  get() = if (totalBytes > 0)
            "${"%.1f".format(readMb)} Mo / ${"%.1f".format(totalMb)} Mo"
            else "${"%.1f".format(readMb)} Mo"
    }

    /** Téléchargement terminé avec le contenu complet */
    data class Done(
        val content: String,
        val totalBytes: Long
    ) : DownloadProgress() {
        val sizeMb: Float get() = totalBytes / 1_048_576f
    }

    /** Erreur pendant le téléchargement */
    data class Failed(val error: String) : DownloadProgress()
}
