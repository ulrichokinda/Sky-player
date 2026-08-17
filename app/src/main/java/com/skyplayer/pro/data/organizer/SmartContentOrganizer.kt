package com.skyplayer.pro.data.organizer

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Organisateur intelligent du contenu avec tri régional prioritaire
 */
@Singleton
class SmartContentOrganizer @Inject constructor() {

    companion object {
        // Mots-clés pour détection priorité régionale
        private val REGIONAL_KEYWORDS = listOf(
            // France
            "france", "français", "francais", "french", "fr",
            // Afrique francophone (générique)
            "afrique", "africa", "african", "afri",
            // Maghreb / Arabes
            "maghreb", "arabic", "arab", "arabe", "khaliji", "mashreq",
            // Algérie
            "algérie", "algeria", "alg",
            // Maroc
            "maroc", "morocco", "mar",
            // Tunisie
            "tunisie", "tunisia", "tun",
            // Sénégal
            "sénégal", "senegal", "sen",
            // Côte d'Ivoire
            "côte d'ivoire", "cote d'ivoire", "ivoire", "civ",
            // Mali
            "mali", "mli",
            // Burkina Faso
            "burkina", "faso", "burkina faso",
            // Niger
            "niger", "nig",
            // Bénin
            "bénin", "benin", "ben",
            // Togo
            "togo", "tgo",
            // Ghana
            "ghana", "gha",
            // Guinée
            "guinée", "guinea", "gui",
            // Congo
            "congo", "rc", "drc", "rdc",
            // Cameroun
            "cameroun", "cameroon", "cmr",
            // Gabon
            "gabon", "gab",
            // Tchad
            "tchad", "chad", "tcd",
            // Centrafrique
            "centrafrique", "car",
            // Burundi
            "burundi", "bdi",
            // Rwanda
            "rwanda", "rwa",
            // Madagascar
            "madagascar", "mdg",
            // Comores
            "comores", "comoros", "com",
            // Maurice
            "maurice", "mauritius", "mus",
            // Seychelles
            "seychelles", "syc",
            // Djibouti
            "djibouti", "dji",
            // Égypte
            "égypte", "egypte", "egypt", "egy",
            // Libye
            "libye", "libya", "lby",
            // Soudan
            "soudan", "sudan", "sdn",
            // Éthiopie / Amharique romanisé
            "éthiopie", "ethiopie", "ethiopia", "eth",
            "ethiopian", "amhara", "addis",
            // Érythrée
            "érythrée", "erythree", "eritrea", "eri",
            // Somalie
            "somalie", "somalia", "som",
            // Swahili / East Africa
            "swahili", "kenya", "ugandan", "tanzanian",
            // Kenya, Tanzania, Uganda (codes/noms longs déjà partiellement couverts)
            "ken", "tanzanie", "tanzania", "tza", "ouganda", "uganda", "uga",
            // Nigeria
            "nigeria", "ng", "naija", "nigerian", "lagos", "abuja", "nollywood",
            // Zambie
            "zambie", "zambia", "zmb",
            // Zimbabwe
            "zimbabwe", "zwe",
            // Botswana
            "botswana", "bwa",
            // Namibie
            "namibie", "namibia", "nam",
            // Afrique du Sud
            "afrique du sud", "south africa", "zaf",
            // Mozambique / Angola / Lusophone
            "mozambique", "moz", "mozambican",
            "angola", "ago", "angolan",
            "lusophone",
            // Malawi
            "malawi", "mwi",
            // Mauritanie
            "mauritanie", "mauritania", "mrt",
            // Gambie
            "gambie", "gambia", "gmb",
            // Sierra Leone
            "sierra leone", "sle",
            // Libéria
            "libéria", "liberia", "lbr",
            // Guinée-Bissau
            "guinée-bissau", "guinea-bissau", "gnb",
            // Cap-Vert
            "cap-vert", "cap vert", "cape verde", "cpv",
            // Sao Tomé
            "sao tome", "stp",
            // Guinée Équatoriale
            "équatorial", "equatorial", "gnq",
            // Canada / Québec
            "canada", "quebec", "québec", "can",
            // Belgique
            "belgique", "belgium", "bel",
            // Suisse
            "suisse", "switzerland", "swiss", "che"
        )

        // Diffuseurs africains et internationaux reconnus → score 90+
        private val KNOWN_AFRICAN_BROADCASTERS = listOf(
            "rfi", "canal+ afrique", "canal afrique", "supersport", "super sport",
            "bein sports africa", "bein africa", "crtv", "rti", "ortb", "nta",
            "sabc", "ktn", "africa24", "tv5monde", "tv5 monde", "africable",
            "equinoxe tv", "canal 2", "rtci", "2m", "snrt", "al aoula",
            "ait", "channels tv", "arise news", "dstv", "mbc africa"
        )

        // Types de contenu pour séparation
        private val MOVIE_KEYWORDS = listOf(
            "film", "movie", "cinema", "vod", "movies"
        )

        private val SERIES_KEYWORDS = listOf(
            "série", "serie", "series", "tv show", "épisode", "episode", "saison", "season"
        )

        private val SPORTS_KEYWORDS = listOf(
            "sport", "sports", "football", "soccer", "basket", "rugby", "tennis", "bein",
            "canal+ sport", "canal sport", "espn", "fox sports", "eurosport"
        )

        private val NEWS_KEYWORDS = listOf(
            "news", "actualité", "actualites", "info", "infos", "information",
            "journal", "journaux", "bfm", "cnews", "france 24", "al jazeera"
        )

        // Patterns M3U arabes et Xtream à nettoyer dans les noms de catégorie
        private val XTREAM_PREFIX_PATTERN = Regex(
            "^\\s*\\[(?:VOD|FR|AF|AR|EN|US|UK|BE|CH|MA|DZ|TN|SN|CI|CM|GH|NG|ZA|EG|TR|IT|DE|ES|PT|NL|RU|IN|PK|BD|SA|AE|QA|KW|IQ|SY|LB|JO)]\\s*",
            RegexOption.IGNORE_CASE
        )

        // Patterns "|AR|", "AR |", "|| AR |", "|XX|" génériques présents dans les playlists
        private val M3U_TAG_PATTERN = Regex(
            "\\|{1,2}\\s*[A-Z]{2,5}\\s*\\|{1,2}|[A-Z]{2,5}\\s*\\|"
        )
    }

    // Cache de l'organisation : la classification/tri est lourde (regex + tris sur
    // des dizaines de milliers d'éléments). On ne la recalcule que si le catalogue
    // a réellement changé (signature stable), sinon on ressort le dernier résultat.
    private val organizationCache = ConcurrentHashMap<Long, OrganizedContent>()

    /**
     * Organise et trie les chaînes par type avec structure hiérarchique pour les séries.
     *
     * Résultat mis en cache : les collectes répétées du flux Room (refresh, toggle
     * favori, re-rendu) ne relancent pas toute l'organisation tant que le catalogue
     * n'a pas changé.
     */
    fun organizeChannels(channels: List<Channel>): OrganizedContent {
        val signature = computeCatalogSignature(channels)
        organizationCache[signature]?.let { return it }

        val result = doOrganizeChannels(channels)
        organizationCache[signature] = result

        // Cache borné : on garde au plus quelques catalogues pour éviter les fuites mémoire.
        if (organizationCache.size > 8) {
            organizationCache.clear()
        }
        return result
    }

    /**
     * Signature stable du catalogue : ne dépend que des champs réellement lus par
     * l'organisation + des champs affichés dans les listes organisées (favori,
     * dernier visionnage, logo) pour ne jamais renvoyer des références périmées.
     */
    private fun computeCatalogSignature(channels: List<Channel>): Long {
        var hash = 1125899906842597L
        for (channel in channels) {
            hash = hash * 31 + channel.id.hashCode()
            hash = hash * 31 + channel.name.hashCode()
            hash = hash * 31 + (channel.logoUrl?.hashCode() ?: 0)
            hash = hash * 31 + channel.category.hashCode()
            hash = hash * 31 + channel.type.hashCode()
            hash = hash * 31 + (channel.groupTitle?.hashCode() ?: 0)
            hash = hash * 31 + channel.isFavorite.hashCode()
            hash = hash * 31 + (channel.lastWatched ?: 0L)
        }
        return hash
    }

    private fun doOrganizeChannels(channels: List<Channel>): OrganizedContent {
        val startTime = System.currentTimeMillis()

        // 1. Filtrage strict par type (déjà séparé par ContentClassifier)
        val liveChannels = channels.filter {
            it.type == ContentType.LIVE_TV ||
                it.type == ContentType.LIVE_SPORTS ||
                it.type == ContentType.LIVE_NEWS ||
                it.type == ContentType.RADIO
        }
        val vodMovies = channels.filter { it.type == ContentType.VOD_MOVIE }
        val vodSeriesRaw = channels.filter {
            it.type == ContentType.VOD_SERIES || it.type == ContentType.SERIES_EPISODE
        }

        // 2. Organisation hiérarchique des séries (Titre > Saison > Épisode)
        val structuredSeries = organizeSeriesHierarchically(vodSeriesRaw)

        // 3. Organisation classique pour Live et Movies
        val organizedLive = organizeLiveChannels(liveChannels)
        val organizedMovies = organizeMovies(vodMovies)

        val duration = System.currentTimeMillis() - startTime
        Timber.d("✅ Organisation terminée en ${duration}ms")

        return OrganizedContent(
            liveChannels = organizedLive,
            movies = organizedMovies,
            series = structuredSeries
        )
    }

    /**
     * Structure les séries : Titre -> Saison -> Épisode
     */
    private fun organizeSeriesHierarchically(seriesChannels: List<Channel>): List<SeriesItem> {
        if (seriesChannels.isEmpty()) return emptyList()

        // Regex pour extraire le titre propre de la série (avant S01E01 ou Saison 1)
        val seriesTitleRegex = Regex("""(?i)(.*?)(?:\s+S\d{1,2}E\d{1,2}|\s+Saison\s*\d+|\s+\d{1,2}x\d{1,2}).*""")

        return seriesChannels.groupBy {
            // Extraire le titre de la série
            seriesTitleRegex.find(it.name)?.groupValues?.get(1)?.trim() ?: it.name.trim()
        }.mapNotNull { (seriesTitle, episodes) ->
            if (episodes.isEmpty()) return@mapNotNull null

            // Grouper par saison
            val seasons = episodes.groupBy {
                ContentClassifier.extractSeasonNumber(it.name)
            }.map { (seasonNumber, seasonEpisodes) ->
                SeasonItem(
                    seasonNumber = seasonNumber,
                    episodes = seasonEpisodes.sortedBy { ContentClassifier.extractEpisodeNumber(it.name) }
                )
            }.sortedBy { it.seasonNumber }

            SeriesItem(
                title = seriesTitle,
                coverUrl = episodes.firstOrNull()?.logoUrl,
                seasons = seasons,
                category = episodes.firstOrNull()?.category ?: "Séries"
            )
        }.sortedBy { it.title }
    }

    /**
     * Nettoie le nom d'une catégorie :
     * - Supprime les préfixes Xtream ([VOD], [FR], [AF], [AR] …)
     * - Supprime les tags M3U arabes/génériques (|AR|, AR |, || AR |, |XX|)
     * - Supprime les préfixes/suffixes numériques (ex : "01 Sport", "Sport 02", "|Sport|")
     * - Supprime les séparateurs non textuels
     * - Met en forme capitalisée
     */
    private fun cleanCategoryName(raw: String): String {
        if (raw.isBlank()) return raw
        return raw
            // Supprimer préfixes Xtream entre crochets : "[VOD]", "[FR]", "[AF]", etc.
            .replace(XTREAM_PREFIX_PATTERN, "")
            // Supprimer tags M3U de type "|AR|", "|| AR |", "AR |", "|XX|"
            .replace(M3U_TAG_PATTERN, "")
            // Supprimer préfixes numériques : "01 ", "1. ", "001|", etc.
            .replace(Regex("^\\d+[.:\\-|_\\s]+"), "")
            // Supprimer suffixes numériques : " 01", " (1)", " [1]"
            .replace(Regex("[.:\\-|_\\s]+\\d+$"), "")
            .replace(Regex("\\s*\\(\\d+\\)$"), "")
            .replace(Regex("\\s*\\[\\d+]$"), "")
            // Supprimer barres verticales et underscores en début/fin
            .replace(Regex("^[|_\\-:]+"), "")
            .replace(Regex("[|_\\-:]+$"), "")
            // Nettoyer espaces multiples
            .replace(Regex("\\s+"), " ")
            .trim()
            // Capitaliser si tout en majuscules (ex : "SPORT" → "Sport")
            .let { name ->
                if (name.all { it.isUpperCase() || !it.isLetter() })
                    name.lowercase().replaceFirstChar { it.uppercaseChar() }
                else name
            }
    }

    /**
     * Organise les chaînes Live avec tri régional prioritaire et nettoyage des dossiers vides.
     */
    private fun organizeLiveChannels(channels: List<Channel>): List<ChannelCategory> {
        // Grouper par catégorie normalisée (priorité groupTitle si présent, sinon category)
        val grouped = channels.groupBy {
            val rawCategory = if (!it.groupTitle.isNullOrBlank()) it.groupTitle else it.category
            ContentClassifier.normalizeCategory(cleanCategoryName(rawCategory), it.type)
        }

        // Créer les catégories avec score de priorité
        val categories = grouped.mapNotNull { (categoryName, channelList) ->
            // Nettoyage : Si le dossier est vide ou ne contient que des chaînes invalides, on l'ignore
            if (channelList.isEmpty()) return@mapNotNull null

            val categoryScore = calculateRegionalPriority(categoryName)
            val bestChannelScore = channelList.maxOfOrNull {
                calculateRegionalPriority(categoryName, channelName = it.name)
            } ?: 0
            val priority = maxOf(categoryScore, bestChannelScore)

            val subCategories = detectSubCategories(channelList)

            ChannelCategory(
                name = categoryName.ifEmpty { "Général" },
                channels = sortByRegionalPriority(channelList),
                priority = priority,
                subCategories = subCategories,
                type = detectCategoryType(categoryName)
            )
        }

        // Trier : priorité régionale d'abord, puis ordre alphabétique
        return categories.sortedWith(
            compareByDescending<ChannelCategory> { it.priority }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Organise les films
     */
    private fun organizeMovies(movies: List<Channel>): List<ChannelCategory> {
        val grouped = movies.groupBy {
            ContentClassifier.normalizeCategory(
                cleanCategoryName(it.category),
                ContentType.VOD_MOVIE
            )
        }

        return grouped.mapNotNull { (category, movieList) ->
            if (movieList.isEmpty()) return@mapNotNull null

            ChannelCategory(
                name = category.ifEmpty { "Films" },
                channels = movieList.sortedBy { it.name },
                priority = calculateRegionalPriority(category),
                subCategories = emptyList(),
                type = ContentType.VOD_MOVIE
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Organise les séries
     */
    private fun organizeSeries(series: List<Channel>): List<ChannelCategory> {
        val grouped = series.groupBy {
            ContentClassifier.normalizeCategory(
                cleanCategoryName(it.category),
                ContentType.VOD_SERIES
            )
        }

        return grouped.map { (category, seriesList) ->
            ChannelCategory(
                name = category.ifEmpty { "Séries" },
                channels = seriesList.sortedBy { it.name },
                priority = calculateRegionalPriority(category),
                subCategories = emptyList(),
                type = ContentType.VOD_SERIES
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Calcule le score de priorité régionale (plus élevé = plus prioritaire).
     *
     * @param categoryName Nom de la catégorie (obligatoire).
     * @param channelName  Nom de la chaîne individuelle (optionnel). Quand fourni, les diffuseurs
     *                     africains reconnus et les mots-clés régionaux présents dans le nom
     *                     contribuent au score final.
     */
    private fun calculateRegionalPriority(
        categoryName: String,
        channelName: String = ""
    ): Int {
        val normalizedCategory = categoryName.lowercase()
        val normalizedChannel = channelName.lowercase()
        var score = 0

        // --- Score basé sur la catégorie ---
        for (keyword in REGIONAL_KEYWORDS) {
            if (normalizedCategory.contains(keyword)) {
                score += when {
                    keyword in listOf("france", "français", "francais", "french", "fr") -> 100
                    keyword in listOf("afrique", "africa", "african", "afri") -> 90
                    keyword in listOf(
                        "maghreb", "arabic", "arab", "arabe", "khaliji", "mashreq"
                    ) -> 85
                    else -> 80 // Autres pays africains / régionaux
                }
            }
        }

        // --- Score supplémentaire basé sur le nom de la chaîne ---
        if (normalizedChannel.isNotBlank()) {
            // Diffuseurs africains connus → bonus garanti ≥ 90
            for (broadcaster in KNOWN_AFRICAN_BROADCASTERS) {
                if (normalizedChannel.contains(broadcaster)) {
                    score = maxOf(score, 90)
                    break
                }
            }

            // Mots-clés régionaux dans le nom de la chaîne
            for (keyword in REGIONAL_KEYWORDS) {
                if (normalizedChannel.contains(keyword)) {
                    score += when {
                        keyword in listOf("france", "français", "francais", "french", "fr") -> 50
                        keyword in listOf("afrique", "africa", "african", "afri") -> 45
                        keyword in listOf(
                            "maghreb", "arabic", "arab", "arabe", "khaliji", "mashreq"
                        ) -> 40
                        else -> 35
                    }
                }
            }
        }

        return score
    }

    /**
     * Trie les chaînes au sein d'une catégorie par priorité régionale (nom de chaîne inclus).
     */
    private fun sortByRegionalPriority(channels: List<Channel>): List<Channel> {
        return channels.sortedWith(
            compareByDescending<Channel> { calculateRegionalPriority("", channelName = it.name) }
                .thenBy { it.name }
        )
    }

    /**
     * Détecte les sous-catégories au sein d'une catégorie.
     * Reconnaît désormais : Sport, Actualités, Régional, Radio, Nollywood, Bollywood,
     * Télévision locale.
     */
    private fun detectSubCategories(channels: List<Channel>): List<String> {
        val subCategories = mutableSetOf<String>()

        for (channel in channels) {
            val name = channel.name.lowercase()
            when {
                SPORTS_KEYWORDS.any { name.contains(it) } ->
                    subCategories.add("Sport")
                NEWS_KEYWORDS.any { name.contains(it) } ->
                    subCategories.add("Actualités")
                // Nollywood (cinéma nigérian)
                name.contains("nollywood") ->
                    subCategories.add("Nollywood")
                // Bollywood (cinéma indien)
                name.contains("bollywood") ->
                    subCategories.add("Bollywood")
                // Télévision locale / communautaire
                name.contains("local") || name.contains("communaut") || name.contains("régional") ->
                    subCategories.add("Télévision locale")
                REGIONAL_KEYWORDS.any { name.contains(it) } ->
                    subCategories.add("Régional")
                name.contains("radio") ->
                    subCategories.add("Radio")
            }
        }

        return subCategories.toList()
    }

    /**
     * Détecte le type de catégorie (Général, Sport, News, etc.)
     */
    private fun detectCategoryType(categoryName: String): ContentType {
        val normalized = categoryName.lowercase()
        return when {
            SPORTS_KEYWORDS.any { normalized.contains(it) } -> ContentType.LIVE_SPORTS
            NEWS_KEYWORDS.any { normalized.contains(it) } -> ContentType.LIVE_NEWS
            normalized.contains("radio") -> ContentType.RADIO
            MOVIE_KEYWORDS.any { normalized.contains(it) } -> ContentType.VOD_MOVIE
            SERIES_KEYWORDS.any { normalized.contains(it) } -> ContentType.VOD_SERIES
            else -> ContentType.LIVE_TV
        }
    }

    /**
     * Retourne le score de priorité régionale pour un nom donné (catégorie ou chaîne).
     * Utile pour interroger le score depuis l'extérieur ou pour les tests.
     *
     * @param name Nom de catégorie ou de chaîne à évaluer.
     * @return Score entier (0 = aucune affinité régionale, plus élevé = plus prioritaire).
     */
    fun getRegionalScore(name: String): Int =
        calculateRegionalPriority(categoryName = name, channelName = name)

    /**
     * Version Flow pour opérations asynchrones
     */
    fun organizeChannelsFlow(channels: List<Channel>): Flow<OrganizedContent> = flow {
        emit(organizeChannels(channels))
    }.flowOn(Dispatchers.Default)
}

/**
 * Représente une catégorie de contenu organisée
 */
data class ChannelCategory(
    val name: String,
    val channels: List<Channel>,
    val priority: Int = 0,
    val subCategories: List<String> = emptyList(),
    val type: ContentType = ContentType.LIVE_TV
) {
    val isRegionalPriority: Boolean get() = priority > 50
    val channelCount: Int get() = channels.size
}

/**
 * Contenu organisé complet avec structure hiérarchique pour les séries
 */
data class OrganizedContent(
    val liveChannels: List<ChannelCategory>,
    val movies: List<ChannelCategory>,
    val series: List<SeriesItem> // Utilise SeriesItem pour une structure hiérarchique
)

/**
 * Structure hiérarchique pour une série
 */
data class SeriesItem(
    val title: String,
    val coverUrl: String? = null,
    val seasons: List<SeasonItem>,
    val category: String = "Séries"
)

/**
 * Structure pour une saison
 */
data class SeasonItem(
    val seasonNumber: Int,
    val episodes: List<Channel> // Chaque épisode est un Channel de type SERIES_EPISODE
)
