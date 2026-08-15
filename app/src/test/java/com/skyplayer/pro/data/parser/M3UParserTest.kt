package com.skyplayer.pro.data.parser

import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class M3UParserTest {

    private lateinit var parser: M3UParser

    @Before
    fun setUp() {
        parser = M3UParser(OkHttpClient())
    }

    @Test
    fun parseFromContent_extractsLiveChannel() = runTest {
        val content = """
            #EXTM3U x-tvg-url="http://epg.example.com/xmltv.xml"
            #EXTINF:-1 tvg-id="bbc1" tvg-logo="http://logo.example.com/bbc.png" group-title="News",BBC One HD
            http://stream.example.com/live/bbc1.m3u8
        """.trimIndent()

        val channels = parser.parseFromContent(content, "playlist-1")

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertTrue(channel.name.contains("BBC One"))
        assertEquals("http://stream.example.com/live/bbc1.m3u8", channel.url)
        assertEquals("Actualités", channel.groupTitle)
        assertEquals("bbc1", channel.epgId)
        assertEquals(ContentType.LIVE_TV, channel.type)
    }

    @Test
    fun parseFromContent_parsesMultipleChannels() = runTest {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Sport 1
            http://stream.example.com/sport1.m3u8
            #EXTINF:-1 group-title="Sports",Sport 2
            https://stream.example.com/sport2.m3u8
        """.trimIndent()

        val channels = parser.parseFromContent(content, "playlist-2")

        assertEquals(2, channels.size)
        assertEquals("Sport 1", channels[0].name)
        assertEquals("Sport 2", channels[1].name)
    }

    @Test
    fun parseFromContent_ignoresInvalidUrls() = runTest {
        val content = """
            #EXTM3U
            #EXTINF:-1,Valid Channel
            http://stream.example.com/valid.m3u8
            #EXTINF:-1,Orphan Line
        """.trimIndent()

        val channels = parser.parseFromContent(content, "playlist-3")

        assertEquals(1, channels.size)
        assertEquals("Valid Channel", channels[0].name)
    }

    @Test
    fun parseFromContent_generatesStableIds() = runTest {
        val content = """
            #EXTM3U
            #EXTINF:-1,Channel A
            http://stream.example.com/a.m3u8
        """.trimIndent()

        val first = parser.parseFromContent(content, "pl")
        val second = parser.parseFromContent(content, "pl")

        assertEquals(first.first().id, second.first().id)
        assertTrue(first.first().id.startsWith("pl_"))
    }
}
