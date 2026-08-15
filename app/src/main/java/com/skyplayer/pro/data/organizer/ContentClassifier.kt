package com.skyplayer.pro.data.organizer

import com.skyplayer.pro.data.model.ContentType
import java.text.Normalizer

object ContentClassifier {

    // Cache LRU pour accélérer la classification sur les grosses playlists
    private const val MAX_CACHE_SIZE = 2000
    private val contentTypeCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, ContentType>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ContentType>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )

    // ─────────────────────────────────────────────────────────────────────────
    // SPORTS
    // ─────────────────────────────────────────────────────────────────────────
    private val liveSportsKeywords = listOf(
        // Generic
        "sport", "sports", "football", "soccer", "basket", "nba", "tennis", "rugby",
        "f1", "formula", "motogp", "espn", "eurosport",
        // International/pan-African broadcasters
        "canal sport", "super sport", "supersport", "dstv sport", "multichoice",
        "beinsport", "bein sport", "bein sports", "bein",
        // Arabic transliterated
        "riyada", "riadha", "riada", "koora", "kora",
        // Arabic script
        "\u0643\u0631\u0629", "\u0631\u064a\u0627\u0636\u0629", "\u0631\u064a\u0627\u0636\u064a",
        // Swahili (Kenya, Tanzania, Uganda)
        "michezo", "mpira wa miguu", "mpira",
        // Hausa (Nigeria, Niger, Chad)
        "wasanni", "kwallon kafa", "kwallon",
        // Portuguese (Angola, Mozambique, Brazil)
        "desporto", "futebol", "basquete", "atletismo",
        // Canal+ Africa sport brands
        "canal+ sport", "canal sport afrique", "rtv sport"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // NEWS
    // ─────────────────────────────────────────────────────────────────────────
    private val liveNewsKeywords = listOf(
        // Generic
        "news", "actualite", "actualites", "info", "infos", "journal",
        "bfm", "france 24", "cnn", "bbc", "al jazeera", "cnews", "dw", "sky news",
        // Arabic transliterated
        "akhbar", "arabiya", "al arabiya", "alhurra", "nessma news",
        "watan", "ennahar", "echourouk",
        // Arabic script
        "\u0623\u062e\u0628\u0627\u0631", "\u0639\u0631\u0628\u064a", "\u0627\u0644\u062d\u0631\u0629",
        // Swahili
        "habari", "taarifa",
        // Hausa
        "labari", "labarai", "bbc hausa",
        // Portuguese
        "noticias", "jornal", "telejornal",
        // Pan-African / international in Africa
        "rfi", "france 24 afrique", "africa 24", "tv5monde",
        // Francophone African national broadcasters
        "crtv", "ortb", "ortm", "ortl", "ort", "rti", "rts", "rtnb", "rtnc",
        // Anglophone African broadcasters
        "nta", "sabc news", "ktn news", "ntv kenya", "citizen tv",
        "tbc", "tvt", "nbsafrica",
        // Others
        "tv camer", "equinoxe"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // MOVIES
    // ─────────────────────────────────────────────────────────────────────────
    private val movieKeywords = listOf(
        "movie", "movies", "film", "films", "cinema", "cinema", "vod", "boxoffice"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // SERIES
    // ─────────────────────────────────────────────────────────────────────────
    private val seriesKeywords = listOf(
        "series", "serie", "serie", "tv show", "tvshow", "show", "season", "saison",
        "episode", "episode", "episodes", "episodes",
        "novela", "telenovela", "dorama"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // KIDS
    // ─────────────────────────────────────────────────────────────────────────
    private val kidsKeywords = listOf(
        // Generic
        "kids", "kid", "children", "child", "cartoon", "anime", "junior",
        "jeunesse", "enfant", "enfants",
        // Arabic transliterated + script
        "atfal", "\u0623\u0637\u0641\u0627\u0644", "\u0643\u0631\u062a\u0648\u0646", "kartoon",
        // Swahili
        "watoto", "katuni",
        // Hausa
        "yara", "karatu",
        // Portuguese
        "infantil", "criancas", "crianca", "infantis",
        // Kids channels present in Africa
        "cartoon network", "nickelodeon", "disney", "boomerang",
        "gulli africa", "canal+ family", "tiji", "tfou"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // DOCUMENTARY
    // ─────────────────────────────────────────────────────────────────────────
    private val documentaryKeywords = listOf(
        // Generic
        "documentary", "documentaire", "discovery", "nat geo", "history",
        "science", "animal",
        // Arabic transliterated + script
        "wathaiqi", "\u0648\u062b\u0627\u0626\u0642\u064a",
        // Swahili
        "uhalisia",
        // French extra
        "reportage", "enquete", "investigation", "magazine",
        // Portuguese
        "documentario",
        // International documentary channels
        "national geographic", "natgeo", "nat geo wild",
        "discovery channel", "animal planet", "history channel",
        "arte", "planete+", "planete plus",
        // African documentary brands
        "afrimagazine", "afrimag"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // MUSIC
    // ─────────────────────────────────────────────────────────────────────────
    private val musicKeywords = listOf(
        // Generic
        "music", "musique", "mtv", "hits", "radio",
        // Arabic transliterated + script
        "tarab", "\u0637\u0631\u0628", "\u0645\u0648\u0633\u064a\u0642\u0649", "musiqa",
        // African music channels
        "trace", "trace africa", "trace urban", "trace mziki",
        "canal+ musique", "afro hits", "afrobeat",
        // Portuguese / Lusophone music genres
        "musica", "kizomba", "kuduro", "zouk",
        // Swahili
        "muziki", "bongo flava",
        // General
        "gospel", "clip", "clips", "videoclip", "video clip", "soundcity"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // ADULT
    // ─────────────────────────────────────────────────────────────────────────
    private val adultKeywords = listOf(
        "adult", "adulte", "xxx", "porn", "erotic",
        "+18", "18+", "+21", "21+", "adults only", "adult only",
        "x rated", "xrated", "explicit", "mature",
        "playboy", "penthouse", "brazzers", "bangbros",
        "x-"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // RELIGION
    // ─────────────────────────────────────────────────────────────────────────
    private val religionKeywords = listOf(
        // Islam — transliterated
        "islam", "islamique", "islamiya", "islami",
        "coran", "quran", "al quran", "muslim", "muslima", "oumma",
        // Islam — Arabic script
        "\u062f\u064a\u0646", "\u062f\u064a\u0646\u064a", "\u062f\u064a\u064a\u0646\u0629",
        "\u0642\u0631\u0622\u0646",
        // Christian Africa
        "gospel", "eglise", "evangile", "evangelho", "evangelical",
        "christian", "christ", "jesus", "praise", "worship",
        "eglise", "adventiste", "catholique", "protestant",
        // Notable African church channels
        "emmanueltv", "lwf", "dclm", "rccg", "winners chapel",
        "mountain of fire", "shiloh",
        // Generic
        "relig", "church"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // GENERAL / ENTERTAINMENT (African broadcasters)
    // ─────────────────────────────────────────────────────────────────────────
    private val generalKeywords = listOf(
        "general", "general", "tv", "entertainment", "divertissement",
        "national", "generaliste", "generaliste"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // BOUQUETS ET PAYS
    // ─────────────────────────────────────────────────────────────────────────
    private val bouquetCanalPlus = listOf("canal+", "canal plus", "c+", "canal afrique", "canal+ afrique")
    private val bouquetBein = listOf("bein", "bein sport", "bein sports")
    
    private val countryIvoryCoast = listOf("rti", "la 3", "ivory coast", "cote d'ivoire", "ci", "abidjan")
    private val countrySenegal = listOf("rts", "tfm", "2stv", "senegal", "sn", "dakar")
    private val countryFrance = listOf("france 2", "france 3", "france 4", "france 5", "tf1", "m6", "fr", "france")
    private val countryMali = listOf("ortm", "mali", "ml", "bamako")
    private val countryCameroon = listOf("crtv", "cameroon", "cameroun", "cm", "yaounde")
    private val countryBenin = listOf("ortb", "benin", "bj", "cotonou")
    private val countryTogo = listOf("tvt", "togo", "tg", "lome")
    private val countryGabon = listOf("gabon", "ga", "libreville")
    private val countryCongo = listOf("tele congo", "rtnc", "congo", "cg", "kinshasa")
    private val countryBurkina = listOf("rtb", "burkina", "bf", "ouagadougou")

    // ─────────────────────────────────────────────────────────────────────────
    // VOD GENRE KEYWORDS
    // ─────────────────────────────────────────────────────────────────────────
    private val vodActionKeywords = listOf(
        "action", "thriller", "combat", "guerre", "war", "aventure", "adventure",
        "espionnage", "spy"
    )

    private val vodComedyKeywords = listOf(
        "comedy", "comedie", "comedie", "humour", "humor", "comique"
    )

    private val vodHorrorKeywords = listOf(
        "horror", "horreur", "epouvante", "terreur", "slasher", "gore", "scary"
    )

    private val vodAnimationKeywords = listOf(
        "animation", "anime", "anime", "cartoon", "kids", "enfants", "pixar", "studio ghibli"
    )

    private val vodRomanceKeywords = listOf(
        "romance", "amour", "love", "romantique", "romantic", "sentimentale"
    )

    private val vodDocumentaryKeywords = listOf(
        "documentary", "documentaire", "documentario", "docu"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // SERIES EPISODE DETECTION REGEX
    // ─────────────────────────────────────────────────────────────────────────
    private val seriesEpisodeRegex = Regex(
        """(?i)(""" +
            // Classic SxxExx and NxNN patterns
            """S\d{1,2}E\d{1,2}""" + "|" +
            """\d{1,2}x\d{1,2}\b""" + "|" +
            // Standalone episode markers with surrounding separators
            """[.\s_]E\d{1,3}[.\s_]""" + "|" +
            """[.\s_]EP\d{1,3}[.\s_]""" + "|" +
            // Textual season/episode markers
            """Season\s*\d""" + "|" +
            """Saison\s*\d""" + "|" +
            """Episode\s*\d""" + "|" +
            """\u00c9pisode\s*\d""" + "|" +
            // Extra patterns — Parts / Volumes
            """Part\s*\d+""" + "|" +
            """Partie\s*\d+""" + "|" +
            """Vol\s*\.?\s*\d+""" + "|" +
            """Ep\s*\d+""" + "|" +
            // Regex pour extraction Saison/Épisode
            """S(\d{1,2})[.\s_]?E(\d{1,3})""" + "|" +
            """(\d{1,2})x(\d{1,3})""" + "|" +
            """Saison\s*(\d{1,2})""" + "|" +
            """Episode\s*(\d{1,3})""" + "|" +
            // Arabic — series word
            """\u0645\u0633\u0644\u0633\u0644""" + "|" +
            // Lusophone — telenovela / novela
            """novela""" + "|" +
            """telenovela""" + "|" +
            // Asian drama
            """dorama""" +
            """)"""
    )

    // Regex d'extraction spécifiques
    private val seasonExtractionRegex = Regex("""(?i)(?:S|Saison\s*)(\d{1,2})""")
    private val episodeExtractionRegex = Regex("""(?i)(?:E|Episode\s*|\u00c9pisode\s*|x)(\d{1,3})""")

    /**
     * Extrait le numéro de saison depuis le nom d'un flux M3U
     */
    fun extractSeasonNumber(name: String): Int {
        return seasonExtractionRegex.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    /**
     * Extrait le numéro d'épisode depuis le nom d'un flux M3U
     */
    fun extractEpisodeNumber(name: String): Int {
        return episodeExtractionRegex.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Infers the [ContentType] of a stream entry based on all available metadata.
     */
    fun inferContentType(
        name: String,
        groupTitle: String?,
        url: String,
        duration: Int = -1,
        explicitType: String? = null,
        isXtream: Boolean = false
    ): ContentType {
        // Clé de cache basée sur les métadonnées critiques
        val cacheKey = "${name}_${groupTitle}_${url}_${duration}_${explicitType}_${isXtream}"
        contentTypeCache[cacheKey]?.let { return it }

        // 1. TRI NATIF POUR XTREAM CODES (Sécurisé à 100%)
        if (isXtream && explicitType != null) {
            val normalizedExplicit = explicitType.lowercase()
            return when {
                normalizedExplicit.contains("live") -> ContentType.LIVE_TV
                normalizedExplicit.contains("movie") || normalizedExplicit.contains("vod") -> ContentType.VOD_MOVIE
                normalizedExplicit.contains("series") -> ContentType.VOD_SERIES
                else -> ContentType.LIVE_TV
            }.also { contentTypeCache[cacheKey] = it }
        }

        // 2. STRATÉGIE DE SECOURS POUR LE M3U (Algorithme par étapes)
        return inferM3uContentType(name, groupTitle, url).also { contentTypeCache[cacheKey] = it }
    }

    /**
     * Algorithme par étapes pour le M3U (Fallback)
     */
    private fun inferM3uContentType(name: String, groupTitle: String?, url: String): ContentType {
        val normalizedName = normalize(name)
        val normalizedGroup = normalize(groupTitle.orEmpty())
        val normalizedUrl = url.lowercase()

        // Étape A : Analyse du Groupe (groupTitle)
        val movieGroupKeywords = listOf("vod", "films", "movies", "4k")
        val seriesGroupKeywords = listOf("series", "seasons", "saison")

        if (containsAny(normalizedGroup, movieGroupKeywords)) return ContentType.VOD_MOVIE
        if (containsAny(normalizedGroup, seriesGroupKeywords)) return ContentType.VOD_SERIES

        // Étape B : Analyse de l'Extension
        val liveExtensions = listOf(".ts", ".m3u8")
        val movieExtensions = listOf(".mp4", ".mkv", ".avi")

        if (liveExtensions.any { normalizedUrl.endsWith(it) } || normalizedUrl.contains("output=ts") || normalizedUrl.contains("output=mpegts")) {
            return ContentType.LIVE_TV
        }
        if (movieExtensions.any { normalizedUrl.endsWith(it) }) {
            // On vérifie quand même si ce n'est pas une série (Étape C)
            if (seriesEpisodeRegex.containsMatchIn(name)) return ContentType.VOD_SERIES
            return ContentType.VOD_MOVIE
        }

        // Étape C : Analyse du Titre par RegEx (Fallback final)
        if (seriesEpisodeRegex.containsMatchIn(name)) {
            return ContentType.VOD_SERIES
        }

        // Par défaut, si rien n'est trouvé, on considère que c'est du Live
        return inferLiveContentType(name, groupTitle)
    }

    /**
     * Infers live-stream sub-type from name and group title.
     */
    fun inferLiveContentType(name: String, groupTitle: String?): ContentType {
        val normalized = normalize("$name ${groupTitle.orEmpty()}")
        return when {
            containsAny(normalized, adultKeywords) -> ContentType.LIVE_TV   // keep adult channels as LIVE_TV
            containsAny(normalized, liveSportsKeywords) -> ContentType.LIVE_SPORTS
            containsAny(normalized, liveNewsKeywords) -> ContentType.LIVE_NEWS
            containsAny(normalized, musicKeywords) && normalized.contains("radio") -> ContentType.RADIO
            else -> ContentType.LIVE_TV
        }
    }

    /**
     * Infers a human-readable category label for a stream entry.
     * If [groupTitle] is provided it takes priority (via [normalizeCategory]).
     */
    fun inferCategory(name: String, groupTitle: String?, contentType: ContentType): String {
        val explicitGroup = groupTitle?.trim()?.takeIf { it.isNotBlank() }
        if (explicitGroup != null) {
            return normalizeCategory(explicitGroup, contentType)
        }

        val normalizedName = normalize(name)
        return when (contentType) {
            ContentType.VOD_MOVIE -> inferVodCategory(normalizedName, "Films")
            ContentType.VOD_SERIES, ContentType.SERIES_EPISODE -> inferVodCategory(normalizedName, "Séries")
            ContentType.RADIO -> "Radio"
            ContentType.LIVE_SPORTS -> "Sports"
            ContentType.LIVE_NEWS -> "Actualités"
            ContentType.LIVE_TV -> inferLiveCategory(normalizedName)
        }
    }

    /**
     * Maps a raw category string (e.g., M3U group-title) to a canonical label.
     */
    fun normalizeCategory(category: String, contentType: ContentType): String {
        val normalized = normalize(category)
        return when {
            contentType == ContentType.VOD_MOVIE ->
                inferVodCategory(normalized, "Films")
            contentType == ContentType.VOD_SERIES || contentType == ContentType.SERIES_EPISODE ->
                inferVodCategory(normalized, "Séries")
            
            // Priorité aux bouquets spécifiques demandés
            containsAny(normalized, bouquetCanalPlus) -> "Canal+ Afrique"
            containsAny(normalized, bouquetBein) -> "BeIN Sports"
            
            // Classification par pays
            containsAny(normalized, countryIvoryCoast) -> "Côte d'Ivoire"
            containsAny(normalized, countrySenegal) -> "Sénégal"
            containsAny(normalized, countryMali) -> "Mali"
            containsAny(normalized, countryCameroon) -> "Cameroun"
            containsAny(normalized, countryFrance) -> "France"
            containsAny(normalized, countryBenin) -> "Bénin"
            containsAny(normalized, countryTogo) -> "Togo"
            containsAny(normalized, countryGabon) -> "Gabon"
            containsAny(normalized, countryCongo) -> "Congo"
            containsAny(normalized, countryBurkina) -> "Burkina Faso"

            containsAny(normalized, adultKeywords) -> "Adultes"
            containsAny(normalized, liveSportsKeywords) -> "Sports"
            containsAny(normalized, liveNewsKeywords) -> "Actualités"
            containsAny(normalized, musicKeywords) -> "Musique"
            containsAny(normalized, kidsKeywords) -> "Enfants"
            containsAny(normalized, documentaryKeywords) -> "Documentaires"
            containsAny(normalized, religionKeywords) -> "Religieux"
            containsAny(normalized, movieKeywords) ->
                if (contentType == ContentType.LIVE_TV) "Cinéma" else "Films"
            containsAny(normalized, generalKeywords) -> "Généralistes"
            else -> category.trim().ifBlank {
                when (contentType) {
                    ContentType.VOD_MOVIE -> "Films"
                    ContentType.VOD_SERIES, ContentType.SERIES_EPISODE -> "Séries"
                    ContentType.RADIO -> "Radio"
                    else -> "Généralistes"
                }
            }
        }
    }

    /**
     * Returns a VOD genre label for movies based on name and group title.
     *
     * @return one of: "Action", "Comedie", "Horreur", "Animation", "Romance",
     *                 "Documentaire", or "Drame" (fallback)
     */
    fun getVodGenre(name: String, groupTitle: String?): String {
        val normalized = normalize("$name ${groupTitle.orEmpty()}")
        return when {
            containsAny(normalized, vodActionKeywords) -> "Action"
            containsAny(normalized, vodComedyKeywords) -> "Comedie"
            containsAny(normalized, vodHorrorKeywords) -> "Horreur"
            containsAny(normalized, vodAnimationKeywords) -> "Animation"
            containsAny(normalized, vodRomanceKeywords) -> "Romance"
            containsAny(normalized, vodDocumentaryKeywords) -> "Documentaire"
            else -> "Drame"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun inferLiveCategory(normalizedName: String): String {
        return when {
            // Priorité bouquets
            containsAny(normalizedName, bouquetCanalPlus) -> "Canal+ Afrique"
            containsAny(normalizedName, bouquetBein) -> "BeIN Sports"

            // Priorité pays
            containsAny(normalizedName, countryIvoryCoast) -> "Côte d'Ivoire"
            containsAny(normalizedName, countrySenegal) -> "Sénégal"
            containsAny(normalizedName, countryMali) -> "Mali"
            containsAny(normalizedName, countryCameroon) -> "Cameroun"
            containsAny(normalizedName, countryFrance) -> "France"
            containsAny(normalizedName, countryBurkina) -> "Burkina Faso"
            
            containsAny(normalizedName, adultKeywords) -> "Adultes"
            containsAny(normalizedName, liveSportsKeywords) -> "Sports"
            containsAny(normalizedName, liveNewsKeywords) -> "Actualités"
            containsAny(normalizedName, musicKeywords) -> "Musique"
            containsAny(normalizedName, kidsKeywords) -> "Enfants"
            containsAny(normalizedName, documentaryKeywords) -> "Documentaires"
            containsAny(normalizedName, religionKeywords) -> "Religieux"
            containsAny(normalizedName, movieKeywords) -> "Cinéma"
            else -> "Généralistes"
        }
    }

    private fun inferVodCategory(normalizedName: String, fallback: String): String {
        return when {
            containsAny(normalizedName, adultKeywords) -> "Adultes"
            containsAny(normalizedName, kidsKeywords) -> "Enfants"
            containsAny(normalizedName, documentaryKeywords) -> "Documentaires"
            containsAny(normalizedName, movieKeywords) -> if (fallback == "Films") "Films" else fallback
            containsAny(normalizedName, seriesKeywords) -> if (fallback.startsWith("S")) "Séries" else fallback
            else -> fallback
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { wordBoundaryContains(text, normalize(it)) }

    /**
     * Vérifie la présence de [keyword] comme mot entier et non comme sous-chaîne.
     * Évite les faux positifs du type "nta" dans "documentaires" ou "fr" dans "france".
     * Supporte les scripts non latins (\p{L}).
     */
    private fun wordBoundaryContains(text: String, keyword: String): Boolean {
        if (keyword.isEmpty()) return false
        return Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(keyword) + "(?![\\p{L}\\p{N}])").containsMatchIn(text)
    }

    /**
     * Lowercases [value] and strips Latin combining diacritics (NFD decomposition).
     * Arabic/Hebrew/CJK characters pass through unchanged.
     */
    private fun normalize(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
