package com.skyplayer.pro.data.organizer

import com.skyplayer.pro.data.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentClassifierTest {

    // ── inferContentType : Xtream Codes (type explicite) ─────────────────────
    @Test
    fun inferContentType_xtreamExplicitLive() {
        assertEquals(
            ContentType.LIVE_TV,
            ContentClassifier.inferContentType("x", null, "", explicitType = "live", isXtream = true)
        )
    }

    @Test
    fun inferContentType_xtreamExplicitMovie() {
        assertEquals(
            ContentType.VOD_MOVIE,
            ContentClassifier.inferContentType("x", null, "", explicitType = "movie", isXtream = true)
        )
    }

    @Test
    fun inferContentType_xtreamExplicitSeries() {
        assertEquals(
            ContentType.VOD_SERIES,
            ContentClassifier.inferContentType("x", null, "", explicitType = "series", isXtream = true)
        )
    }

    // ── inferContentType : M3U (heuristiques) ────────────────────────────────
    @Test
    fun inferContentType_m3uGroupVodKeywords() {
        assertEquals(
            ContentType.VOD_MOVIE,
            ContentClassifier.inferContentType("Film 1", "VOD", "http://x/a.mp4")
        )
    }

    @Test
    fun inferContentType_m3uGroupSeriesKeywords() {
        // Le mot-clé de groupe prime sur l'extension (.m3u8 = live normalement)
        assertEquals(
            ContentType.VOD_SERIES,
            ContentClassifier.inferContentType("Série 1", "Séries", "http://x/b.m3u8")
        )
    }

    @Test
    fun inferContentType_m3uHlsExtensionIsLive() {
        assertEquals(
            ContentType.LIVE_TV,
            ContentClassifier.inferContentType("Chaîne 1", null, "http://x/live.m3u8")
        )
    }

    @Test
    fun inferContentType_m3uMp4ExtensionIsMovie() {
        assertEquals(
            ContentType.VOD_MOVIE,
            ContentClassifier.inferContentType("Movie", null, "http://x/movie.mp4")
        )
    }

    @Test
    fun inferContentType_seriesEpisodeInName() {
        assertEquals(
            ContentType.VOD_SERIES,
            ContentClassifier.inferContentType("Game of Thrones S01E02", null, "http://x/stream")
        )
    }

    @Test
    fun inferContentType_newsNameIsNewsChannel() {
        assertEquals(
            ContentType.LIVE_NEWS,
            ContentClassifier.inferContentType("News 24", null, "http://x/stream")
        )
    }

    @Test
    fun inferContentType_sportsNameIsSportsChannel() {
        assertEquals(
            ContentType.LIVE_SPORTS,
            ContentClassifier.inferContentType("ESPN", null, "http://x/stream")
        )
    }

    @Test
    fun inferContentType_unknownNameDefaultsToLive() {
        assertEquals(
            ContentType.LIVE_TV,
            ContentClassifier.inferContentType("Random", null, "http://x/stream")
        )
    }

    // ── inferCategory : catégorie déduite du group-title ─────────────────────
    @Test
    fun inferCategory_groupNewsMapsToActualites() {
        assertEquals("Actualités", ContentClassifier.inferCategory("CNN", "News", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupCountryMapsToFrance() {
        assertEquals("France", ContentClassifier.inferCategory("TF1", "FR", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupSportsMapsToSports() {
        assertEquals("Sports", ContentClassifier.inferCategory("ESPN", "Sport", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupKidsMapsToEnfants() {
        assertEquals("Enfants", ContentClassifier.inferCategory("Canal J", "Enfants", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupMusicMapsToMusique() {
        assertEquals("Musique", ContentClassifier.inferCategory("MTV Hits", "Musique", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupDocumentaryMapsToDocumentaires() {
        assertEquals("Documentaires", ContentClassifier.inferCategory("Discovery", "Documentaires", ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_groupAdultMapsToAdultes() {
        assertEquals("Adultes", ContentClassifier.inferCategory("X", "Mature", ContentType.LIVE_TV))
    }

    // ── inferCategory : fallback sur le nom de la chaîne ─────────────────────
    @Test
    fun inferCategory_nameFallbackSports() {
        assertEquals("Sports", ContentClassifier.inferCategory("ESPN Deportes", null, ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_nameFallbackNews() {
        assertEquals("Actualités", ContentClassifier.inferCategory("News 24", null, ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_nameFallbackCountry() {
        assertEquals("France", ContentClassifier.inferCategory("France 24", null, ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_nameFallbackKids() {
        assertEquals("Enfants", ContentClassifier.inferCategory("Cartoon Network", null, ContentType.LIVE_TV))
    }

    @Test
    fun inferCategory_unknownNameDefaultsToGeneralistes() {
        assertEquals("Généralistes", ContentClassifier.inferCategory("Zzz Channel", null, ContentType.LIVE_TV))
    }

    // ── getVodGenre ──────────────────────────────────────────────────────────
    @Test
    fun getVodGenre_actionKeywords() {
        assertEquals("Action", ContentClassifier.getVodGenre("Spy Game 2", null))
    }

    // ── Numéros de saison / épisode ──────────────────────────────────────────
    @Test
    fun extractSeasonNumber_parsesSxx() {
        assertEquals(5, ContentClassifier.extractSeasonNumber("Breaking Bad S05E08"))
    }

    @Test
    fun extractEpisodeNumber_parsesExx() {
        assertEquals(8, ContentClassifier.extractEpisodeNumber("Breaking Bad S05E08"))
    }

    @Test
    fun extractSeasonNumber_defaultsToOneWhenMissing() {
        assertEquals(1, ContentClassifier.extractSeasonNumber("Série sans numéro"))
    }

    @Test
    fun extractEpisodeNumber_defaultsToOneWhenMissing() {
        assertEquals(1, ContentClassifier.extractEpisodeNumber("Série sans numéro"))
    }
}
