package com.skyplayer.pro.data.parser

import java.security.MessageDigest

/**
 * Génération d'IDs de chaînes M3U **déterministes**.
 *
 * L'ancienne implémentation utilisait `System.nanoTime()` / le numéro de ligne dans
 * l'ID : à chaque refresh, des IDs entièrement nouveaux étaient générés, ce qui
 * orphelinait les favoris et l'historique (clés primaires), et empêchait le
 * dédoublonnage `REPLACE` → doublons accumulés à chaque synchronisation.
 *
 * Règle : on utilise le `tvg-id` du flux s'il est présent (identité stable voulue
 * par le fournisseur), sinon un hash SHA-256 tronqué de l'URL — stable tant que
 * l'URL ne change pas.
 */
object M3UChannelId {

    /**
     * @param playlistId Identifiant de la playlist (préfixe)
     * @param epgId      Attribut `tvg-id` de la chaîne, si présent
     * @param url        URL du flux (base du hash quand pas de tvg-id)
     */
    fun forChannel(playlistId: String, epgId: String?, url: String): String {
        val key = epgId?.trim()?.takeIf { it.isNotBlank() }
            ?: sha256(url.trim()).take(16)
        return "${playlistId}_$key"
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
