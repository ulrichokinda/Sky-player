package com.skyplayer.pro.data.parser

import org.junit.Assert.*
import org.junit.Test

/**
 * A5: Tests unitaires pour la génération d'IDs M3U stables.
 */
class M3UChannelIdTest {

    @Test
    fun `tvg-id is used as key when available`() {
        val playlistId = "p1"
        val epgId = "france2.fr"
        val url = "http://example.com/stream.m3u8"
        val id = M3UChannelId.forChannel(playlistId, epgId, url)
        assertEquals("p1_france2.fr", id)
    }

    @Test
    fun `fallback to hash when no tvg-id`() {
        val playlistId = "p1"
        val url = "http://example.com/stream.m3u8"
        val id = M3UChannelId.forChannel(playlistId, null, url)
        assertNotNull(id)
        assertTrue(id.startsWith("p1_"))
        // Hash is 16 hex chars (SHA-256 truncated) after the prefix
        val hash = id.removePrefix("p1_")
        assertEquals(16, hash.length)
    }

    @Test
    fun `same inputs produce same ID`() {
        val url = "http://example.com/stream.m3u8"
        val tvgId = "canal1.fr"

        val id1 = M3UChannelId.forChannel("p1", tvgId, url)
        val id2 = M3UChannelId.forChannel("p1", tvgId, url)
        assertEquals(id1, id2)
    }

    @Test
    fun `different URLs produce different IDs`() {
        val url1 = "http://example.com/stream1.m3u8"
        val url2 = "http://example.com/stream2.m3u8"

        val id1 = M3UChannelId.forChannel("p1", null, url1)
        val id2 = M3UChannelId.forChannel("p1", null, url2)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `empty tvg-id falls back to hash`() {
        val id1 = M3UChannelId.forChannel("p1", "", "http://example.com/s.m3u8")
        val id2 = M3UChannelId.forChannel("p1", null, "http://example.com/s.m3u8")
        assertEquals(id1, id2)
    }

    @Test
    fun `different playlists produce different IDs for same content`() {
        val id1 = M3UChannelId.forChannel("p1", "ch1", "http://example.com/s.m3u8")
        val id2 = M3UChannelId.forChannel("p2", "ch1", "http://example.com/s.m3u8")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `whitespace in tvg-id is trimmed`() {
        val id1 = M3UChannelId.forChannel("p1", "  france2.fr  ", "http://example.com/s.m3u8")
        val id2 = M3UChannelId.forChannel("p1", "france2.fr", "http://example.com/s.m3u8")
        assertEquals(id1, id2)
    }
}
