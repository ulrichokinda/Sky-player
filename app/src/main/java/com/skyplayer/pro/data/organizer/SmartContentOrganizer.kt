package com.skyplayer.pro.data.organizer

import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Organisateur intelligent du contenu avec tri régional prioritaire
 * Sépare Live TV, Films et Séries avec algorithmes de tri avancés
 */
@Singleton
class SmartContentOrganizer @Inject constructor() {

    companion object {
        // Mots-clés pour détection priorité régionale
        private val REGIONAL_KEYWORDS = listOf(
            // France
            "france", "français", "francais", "french", "fr",
            // Afrique francophone
            "afrique", "africa", "african", "afri",
            // Pays africains spécifiques
            "algérie", "algeria", "alg",
            "maroc", "morocco", "mar",
            "tunisie", "tunisia", "tun",
            "sénégal", "senegal", "sen",
            "côte d'ivoire", "cote d'ivoire", "ivoire", "civ",
            "mali", "mli",
            "burkina", "faso", "burkina faso",
            "niger", "nig",
            "bénin", "benin", "ben",
            "togo", "tgo",
            "ghana", "gha",
            "guinée", "guinea", "gui",
            "congo", "rc", "drc", "rdc",
            "cameroun", "cameroon", "cmr",
            "gabon", "gab",
            "tchad", "chad", "tcd",
            "centrafrique", "car",
            "burundi", "bdi",
            "rwanda", "rwa",
            "madagascar", "mdg",
            "comores", "comoros", "com",
            "maurice", "mauritius", "mus",
            "seychelles", "syc",
            "djibouti", "dji",
            "égypte", "egypte", "egypt", "egy",
            "libye", "libya", "lby",
            "soudan", "sudan", "sdn",
            "éthiopie", "ethiopie", "ethiopia", "eth",
            "érythrée", "erythree", "eritrea", "eri",
            "somalie", "somalia", "som",
            "kenya", "ken",
            "tanzanie", "tanzania", "tza",
            "ouganda", "uganda", "uga",
            "zambie", "zambia", "zmb",
            "zimbabwe", "zwe",
            "botswana", "bwa",
            "namibie", "namibia", "nam",
            "afrique du sud", "south africa", "zaf",
            "mozambique", "moz",
            "malawi", "mwi",
            "mauritanie", "mauritania", "mrt",
            "gambie", "gambia", "gmb",
            "sierra leone", "sle",
            "libéria", "liberia", "lbr",
            "guinée-bissau", "guinea-bissau", "gnb",
            "cap-vert", "cap vert", "cape verde", "cpv",
            "sao tome", "stp",
            "gabon", "gab",
            "équatorial", "equatorial", "gnq",
            "angola", "ago",
            "canada", "quebec", "québec", "can",
            "belgique", "belgium", "bel",
            "suisse", "switzerland", "swiss", "che"
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
    }

    /**
     * Organise et trie les chaînes par type et priorité régionale
     */
    fun organizeChannels(channels: List<Channel>): OrganizedContent {
        val startTime = System.currentTimeMillis()

        // Séparer par type de contenu
        val liveChannels = channels.filter { it.type == ContentType.LIVE_TV }
        val vodMovies = channels.filter { it.type == ContentType.VOD_MOVIE }
        val vodSeries = channels.filter { it.type == ContentType.VOD_SERIES }

        // Organiser chaque catégorie
        val organizedLive = organizeLiveChannels(liveChannels)
        val organizedMovies = organizeMovies(vodMovies)
        val organizedSeries = organizeSeries(vodSeries)

        val duration = System.currentTimeMillis() - startTime
        Timber.d("✅ Organisation terminée en ${duration}ms: ${liveChannels.size} live, ${vodMovies.size} films, ${vodSeries.size} séries")

        return OrganizedContent(
            liveChannels = organizedLive,
            movies = organizedMovies,
            series = organizedSeries
        )
    }

    /**
     * Nettoie le nom d'une catégorie:
     * - Supprime les préfixes/suffixes numériques (ex: "01 Sport", "Sport 02", "|Sport|")
     * - Supprime les séparateurs non textuels
     * - Met en forme capitalisée
     */
    private fun cleanCategoryName(raw: String): String {
        if (raw.isBlank()) return raw
        return raw
            // Supprimer préfixes numériques: "01 ", "1. ", "001|", etc.
            .replace(Regex("^\\d+[.:\\-|_\\s]+"), "")
            // Supprimer suffixes numériques: " 01", " (1)", " [1]"
            .replace(Regex("[.:\\-|_\\s]+\\d+$"), "")
            .replace(Regex("\\s*\\(\\d+\\)$"), "")
            .replace(Regex("\\s*\\[\\d+\\]$"), "")
            // Supprimer barres verticales et underscores en début/fin
            .replace(Regex("^[|_\\-:]+"), "")
            .replace(Regex("[|_\\-:]+$"), "")
            // Nettoyer espaces multiples
            .replace(Regex("\\s+"), " ")
            .trim()
            // Capitaliser si tout en majuscules (ex: "SPORT" → "Sport")
            .let { name ->
                if (name.all { it.isUpperCase() || !it.isLetter() })
                    name.lowercase().replaceFirstChar { it.uppercaseChar() }
                else name
            }
    }

    /**
     * Organise les chaînes Live avec tri régional prioritaire
     */
    private fun organizeLiveChannels(channels: List<Channel>): List<ChannelCategory> {
        // Grouper par catégorie brute puis nettoyer le nom
        val grouped = channels.groupBy { cleanCategoryName(it.category) }

        // Créer les catégories avec score de priorité
        val categories = grouped.map { (categoryName, channelList) ->
            val priority = calculateRegionalPriority(categoryName)
            val subCategories = detectSubCategories(channelList)

            ChannelCategory(
                name = categoryName.ifEmpty { "Général" },
                channels = sortByRegionalPriority(channelList),
                priority = priority,
                subCategories = subCategories,
                type = detectCategoryType(categoryName)
            )
        }

        // Trier: priorité régionale d'abord, puis ordre alphabétique
        return categories.sortedWith(
            compareByDescending<ChannelCategory> { it.priority }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Organise les films
     */
    private fun organizeMovies(movies: List<Channel>): List<ChannelCategory> {
        val grouped = movies.groupBy { cleanCategoryName(it.category) }

        return grouped.map { (category, movieList) ->
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
        val grouped = series.groupBy { cleanCategoryName(it.category) }

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
     * Calcule le score de priorité régionale (plus élevé = plus prioritaire)
     */
    private fun calculateRegionalPriority(categoryName: String): Int {
        val normalizedName = categoryName.lowercase()
        var score = 0

        // Vérifier chaque mot-clé régional
        for (keyword in REGIONAL_KEYWORDS) {
            if (normalizedName.contains(keyword)) {
                // France et pays francophones ont priorité maximale
                score += when {
                    keyword in listOf("france", "français", "francais", "french", "fr") -> 100
                    keyword in listOf("afrique", "africa", "african") -> 90
                    else -> 80 // Autres pays africains
                }
            }
        }

        return score
    }

    /**
     * Trie les chaînes au sein d'une catégorie par priorité régionale
     */
    private fun sortByRegionalPriority(channels: List<Channel>): List<Channel> {
        return channels.sortedWith(
            compareByDescending<Channel> { calculateRegionalPriority(it.name) }
                .thenBy { it.name }
        )
    }

    /**
     * Détecte les sous-catégories au sein d'une catégorie
     */
    private fun detectSubCategories(channels: List<Channel>): List<String> {
        val subCategories = mutableSetOf<String>()

        for (channel in channels) {
            val name = channel.name.lowercase()
            when {
                SPORTS_KEYWORDS.any { name.contains(it) } -> subCategories.add("Sport")
                NEWS_KEYWORDS.any { name.contains(it) } -> subCategories.add("Actualités")
                REGIONAL_KEYWORDS.any { name.contains(it) } -> subCategories.add("Régional")
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
            MOVIE_KEYWORDS.any { normalized.contains(it) } -> ContentType.VOD_MOVIE
            SERIES_KEYWORDS.any { normalized.contains(it) } -> ContentType.VOD_SERIES
            else -> ContentType.LIVE_TV
        }
    }

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
 * Contenu organisé complet
 */
data class OrganizedContent(
    val liveChannels: List<ChannelCategory>,
    val movies: List<ChannelCategory>,
    val series: List<ChannelCategory>
) {
    val totalLiveChannels: Int get() = liveChannels.sumOf { it.channels.size }
    val totalMovies: Int get() = movies.sumOf { it.channels.size }
    val totalSeries: Int get() = series.sumOf { it.channels.size }

    val allCategories: List<ChannelCategory> get() = liveChannels + movies + series
}
