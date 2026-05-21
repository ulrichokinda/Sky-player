package com.skyplayer.pro.data.repository

import com.skyplayer.pro.data.local.ChannelDao
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour le filtrage par groupe et recherche performante
 * 
 * Fonctionnalités :
 * - Extraction des groupes depuis group-title
 * - Filtrage par catégorie (Sport, Cinéma, etc.)
 * - Recherche insensible aux accents et majuscules
 * - Regroupement intelligent des chaînes
 */
@Singleton
class ChannelGroupRepository @Inject constructor(
    private val channelDao: ChannelDao
) {

    companion object {
        // Mapping des groupes communs pour normalisation
        private val GROUP_ALIASES = mapOf(
            "sports" to listOf("sport", "sports", "football", "basket", "tennis", "rugby", "f1", "formule 1"),
            "cinema" to listOf("cinema", "cinéma", "movies", "films", "movie", "film"),
            "news" to listOf("news", "actualités", "actualites", "info", "infos", "information"),
            "music" to listOf("music", "musique", "mtv", "clips"),
            "kids" to listOf("kids", "enfants", "children", "jeunesse", "cartoon", "anime"),
            "documentary" to listOf("documentary", "documentaire", "discovery", "nat geo"),
            "entertainment" to listOf("entertainment", "divertissement", "series", "séries", "tv series"),
            "religious" to listOf("religious", "religion", "chrétien", "chretien", "islam", "gospel"),
            "local" to listOf("local", "locale", "regional", "régional", "national"),
            "international" to listOf("international", "world", "monde", "foreign", "etranger", "étranger")
        )
    }

    /**
     * Récupère tous les groupes uniques avec leur nombre de chaînes
     * Triés par nombre de chaînes décroissant
     */
    fun getAllGroups(contentType: ContentType): Flow<List<GroupInfo>> {
        return channelDao.getChannelsByType(contentType)
            .map { channels ->
                channels.groupBy { extractGroupName(it) }
                    .map { (groupName, channels) ->
                        GroupInfo(
                            name = groupName,
                            displayName = normalizeGroupDisplayName(groupName),
                            channelCount = channels.size,
                            icon = getGroupIcon(groupName)
                        )
                    }
                    .sortedByDescending { it.channelCount }
            }
    }

    /**
     * Filtre les chaînes par groupe
     */
    fun getChannelsByGroup(
        contentType: ContentType,
        groupName: String
    ): Flow<List<Channel>> {
        return channelDao.getChannelsByType(contentType)
            .map { channels ->
                channels.filter { channel ->
                    val channelGroup = extractGroupName(channel)
                    matchesGroup(channelGroup, groupName)
                }
            }
    }

    /**
     * Recherche performante avec normalisation
     * - Ignore les accents
     * - Ignore les majuscules/minuscules
     * - Cherche dans le nom ET le groupe
     */
    fun searchChannels(query: String, contentType: ContentType? = null): Flow<List<Channel>> {
        val normalizedQuery = normalizeText(query)
        
        val sourceFlow = contentType?.let {
            channelDao.getChannelsByType(it)
        } ?: channelDao.getAllChannels()
        
        return sourceFlow.map { channels ->
            channels.filter { channel ->
                matchesSearch(channel, normalizedQuery)
            }
        }
    }

    /**
     * Recherche combinée : groupe + texte
     */
    fun searchInGroup(
        groupName: String,
        searchQuery: String,
        contentType: ContentType
    ): Flow<List<Channel>> {
        val normalizedQuery = normalizeText(searchQuery)
        
        return getChannelsByGroup(contentType, groupName)
            .map { channels ->
                channels.filter { matchesSearch(it, normalizedQuery) }
            }
    }

    /**
     * Regroupe les chaînes par catégorie prédéfinie
     * Utilise les alias pour regrouper intelligemment
     */
    fun getChannelsByCategory(
        category: GroupCategory,
        contentType: ContentType
    ): Flow<List<Channel>> {
        return channelDao.getChannelsByType(contentType)
            .map { channels ->
                channels.filter { channel ->
                    val group = extractGroupName(channel)
                    matchesCategory(group, category)
                }
            }
    }

    /**
     * Récupère les groupes populaires (plus de N chaînes)
     */
    fun getPopularGroups(
        contentType: ContentType,
        minChannels: Int = 5
    ): Flow<List<GroupInfo>> {
        return getAllGroups(contentType)
            .map { groups ->
                groups.filter { it.channelCount >= minChannels }
            }
    }

    // === FONCTIONS PRIVÉES ===

    /**
     * Extrait le nom du groupe depuis une chaîne
     */
    private fun extractGroupName(channel: Channel): String {
        return channel.groupTitle?.trim()?.ifEmpty { "Autres" } ?: "Autres"
    }

    /**
     * Vérifie si un groupe correspond à la recherche
     */
    private fun matchesGroup(channelGroup: String, searchGroup: String): Boolean {
        val normalizedChannel = normalizeText(channelGroup)
        val normalizedSearch = normalizeText(searchGroup)
        
        return normalizedChannel == normalizedSearch ||
               normalizedChannel.contains(normalizedSearch) ||
               normalizedSearch.contains(normalizedChannel)
    }

    /**
     * Vérifie si la chaîne correspond à la recherche textuelle
     */
    private fun matchesSearch(channel: Channel, normalizedQuery: String): Boolean {
        val normalizedName = normalizeText(channel.name)
        val normalizedGroup = normalizeText(channel.groupTitle ?: "")
        val normalizedCategory = normalizeText(channel.category)
        
        return normalizedName.contains(normalizedQuery) ||
               normalizedGroup.contains(normalizedQuery) ||
               normalizedCategory.contains(normalizedQuery)
    }

    /**
     * Vérifie si un groupe appartient à une catégorie
     */
    private fun matchesCategory(groupName: String, category: GroupCategory): Boolean {
        val normalizedGroup = normalizeText(groupName)
        
        return when (category) {
            GroupCategory.SPORTS -> GROUP_ALIASES["sports"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.CINEMA -> GROUP_ALIASES["cinema"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.NEWS -> GROUP_ALIASES["news"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.MUSIC -> GROUP_ALIASES["music"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.KIDS -> GROUP_ALIASES["kids"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.DOCUMENTARY -> GROUP_ALIASES["documentary"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.ENTERTAINMENT -> GROUP_ALIASES["entertainment"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.RELIGIOUS -> GROUP_ALIASES["religious"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.LOCAL -> GROUP_ALIASES["local"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.INTERNATIONAL -> GROUP_ALIASES["international"]?.any { 
                normalizedGroup.contains(it) 
            } ?: false
            GroupCategory.OTHER -> true
        }
    }

    /**
     * Normalise le texte pour la recherche :
     * - Supprime les accents
     * - Convertit en minuscules
     * - Supprime les espaces superflus
     */
    private fun normalizeText(text: String): String {
        return text.trim()
            .lowercase()
            .let { removeAccents(it) }
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Supprime les accents d'une chaîne
     */
    private fun removeAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    /**
     * Normalise l'affichage du nom de groupe
     */
    private fun normalizeGroupDisplayName(name: String): String {
        return name.replaceFirstChar { it.uppercase() }
    }

    /**
     * Détermine l'icône appropriée pour un groupe
     */
    private fun getGroupIcon(groupName: String): String {
        val normalized = normalizeText(groupName)
        
        return when {
            GROUP_ALIASES["sports"]?.any { normalized.contains(it) } == true -> "sports"
            GROUP_ALIASES["cinema"]?.any { normalized.contains(it) } == true -> "movie"
            GROUP_ALIASES["news"]?.any { normalized.contains(it) } == true -> "news"
            GROUP_ALIASES["music"]?.any { normalized.contains(it) } == true -> "music"
            GROUP_ALIASES["kids"]?.any { normalized.contains(it) } == true -> "child_care"
            GROUP_ALIASES["documentary"]?.any { normalized.contains(it) } == true -> "menu_book"
            GROUP_ALIASES["entertainment"]?.any { normalized.contains(it) } == true -> "tv"
            GROUP_ALIASES["religious"]?.any { normalized.contains(it) } == true -> "church"
            GROUP_ALIASES["local"]?.any { normalized.contains(it) } == true -> "location_on"
            GROUP_ALIASES["international"]?.any { normalized.contains(it) } == true -> "public"
            else -> "tv"
        }
    }
}

/**
 * Informations sur un groupe
 */
data class GroupInfo(
    val name: String,
    val displayName: String,
    val channelCount: Int,
    val icon: String
)

/**
 * Catégories de groupes prédéfinies
 */
enum class GroupCategory {
    SPORTS, CINEMA, NEWS, MUSIC, KIDS, 
    DOCUMENTARY, ENTERTAINMENT, RELIGIOUS,
    LOCAL, INTERNATIONAL, OTHER
}
