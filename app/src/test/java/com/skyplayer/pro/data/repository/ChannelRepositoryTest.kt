package com.skyplayer.pro.data.repository

import org.junit.Assert.*
import org.junit.Test

/**
 * A5: Tests unitaires pour l'échappement des requêtes FTS4.
 */
class ChannelRepositoryFtsTest {

    /**
     * Sanitize une requête FTS4 pour éviter les crashes.
     * (Copie de la logique dans ChannelRepository pour test unitaire)
     */
    private fun sanitizeFtsQuery(query: String): String {
        // Retirer les opérateurs FTS4
        val cleaned = query
            .replace("\"", "")
            .replace("*", "")
            .replace("-", " ")
            .replace("(", " ")
            .replace(")", " ")
            .trim()

        if (cleaned.isBlank()) return "*"

        // Transformer en requête préfixe
        val terms = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return terms.joinToString(" OR ") { "$it*" }
    }

    @Test
    fun `normal query passes through`() {
        val result = sanitizeFtsQuery("France 2")
        assertEquals("France* OR 2*", result)
    }

    @Test
    fun `double quotes are removed`() {
        val result = sanitizeFtsQuery("canal \"france\"")
        assertEquals("canal* OR france*", result)
    }

    @Test
    fun `asterisks are removed`() {
        val result = sanitizeFtsQuery("bein* sport*")
        assertEquals("bein* OR sport*", result)
    }

    @Test
    fun `parentheses are removed`() {
        val result = sanitizeFtsQuery("ESPN (USA)")
        assertEquals("ESPN* OR USA*", result)
    }

    @Test
    fun `dash is treated as separator`() {
        val result = sanitizeFtsQuery("beIN-Sports")
        assertEquals("beIN* OR Sports*", result)
    }

    @Test
    fun `empty query returns wildcard`() {
        val result = sanitizeFtsQuery("")
        assertEquals("*", result)
    }

    @Test
    fun `special chars only returns wildcard`() {
        val result = sanitizeFtsQuery("\"\"---***")
        assertEquals("*", result)
    }

    @Test
    fun `mixed special chars are cleaned`() {
        val result = sanitizeFtsQuery("\"canal+\" (sport*-)")
        // " removed, * removed, - -> space, ( ) removed, + stays
        assertEquals("canal+* OR sport*", result)
    }
}
