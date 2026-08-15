package com.skyplayer.pro.util

import android.net.Uri
import timber.log.Timber
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Normalise les URLs serveur Xtream saisies par l'utilisateur.
 * Accepte : base URL, /player_api.php, /get.php avec credentials dans la query.
 */
data class XtreamCredentials(
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null
)

object XtreamUrlNormalizer {

    private val API_PATHS = listOf(
        "/player_api.php",
        "/panel_api.php",
        "/get.php",
        "/xmltv.php"
    )

    /**
     * Nettoie une URL serveur et extrait éventuellement username/password embarqués.
     */
    fun normalize(
        rawInput: String,
        fallbackUsername: String? = null,
        fallbackPassword: String? = null
    ): XtreamCredentials {
        var input = rawInput.trim()
        if (input.isBlank()) {
            throw IllegalArgumentException("L'URL du serveur est vide")
        }

        if (!input.startsWith("http://", ignoreCase = true) &&
            !input.startsWith("https://", ignoreCase = true)
        ) {
            input = "http://$input"
        }

        val uri = Uri.parse(input)
        val scheme = uri.scheme ?: "http"
        val host = uri.host ?: throw IllegalArgumentException("Hôte serveur invalide")
        val port = if (uri.port != -1) ":${uri.port}" else ""

        var username = fallbackUsername?.trim()?.takeIf { it.isNotEmpty() }
        var password = fallbackPassword?.trim()?.takeIf { it.isNotEmpty() }

        uri.getQueryParameter("username")?.takeIf { it.isNotBlank() }?.let { username = it }
        uri.getQueryParameter("password")?.takeIf { it.isNotBlank() }?.let { password = it }

        // Credentials parfois encodés dans le path : /live/user/pass/123.ts
        val pathSegments = uri.pathSegments.filter { it.isNotBlank() }
        if (username.isNullOrBlank() && pathSegments.size >= 3) {
            when (pathSegments[0].lowercase()) {
                "live", "movie", "series" -> {
                    username = decode(pathSegments[1])
                    password = decode(pathSegments[2])
                }
            }
        }

        val baseUrl = "$scheme://$host$port"
        Timber.d("Xtream URL normalisée: $baseUrl (user=${username?.take(3)}...)")

        return XtreamCredentials(
            serverUrl = baseUrl,
            username = username,
            password = password
        )
    }

    fun apiUrl(serverUrl: String): String =
        "${serverUrl.trim().trimEnd('/')}/player_api.php"

    fun looksLikeXtreamApiUrl(input: String): Boolean {
        val lower = input.lowercase()
        return API_PATHS.any { lower.contains(it) }
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
            .getOrDefault(value)
}
