package com.skyplayer.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.skyplayer.pro.data.local.Converters
import java.util.Date

/**
 * Dossier personnalisé créé par l'utilisateur
 * Permet d'organiser films et séries selon ses préférences
 */
@Entity(tableName = "custom_folders")
@TypeConverters(Converters::class)
data class CustomFolder(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val type: FolderType, // MOVIES ou SERIES
    val icon: String? = null, // Emoji ou code d'icône
    val colorHex: String? = null, // Couleur personnalisée
    val itemIds: List<String> = emptyList(), // IDs des films/séries dans ce dossier
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    companion object {
        const val FAVORITES_FOLDER_ID = "favorites"
        const val WATCH_LATER_FOLDER_ID = "watch_later"
        const val MY_MOVIES_FOLDER_ID = "my_movies"
        const val BEST_OF_FOLDER_ID = "best_of"
    }
}

enum class FolderType {
    MOVIES,    // Dossier pour films (VOD)
    SERIES     // Dossier pour séries
}

/**
 * Dossiers par défaut suggérés à la création
 */
fun getDefaultFolders(type: FolderType): List<CustomFolder> {
    return when (type) {
        FolderType.MOVIES -> listOf(
            CustomFolder(
                id = CustomFolder.MY_MOVIES_FOLDER_ID,
                name = "Mes Films Préférés",
                description = "Ma collection personnelle de films",
                type = FolderType.MOVIES,
                icon = "🎬",
                colorHex = "#FF00AEEF"
            ),
            CustomFolder(
                id = CustomFolder.BEST_OF_FOLDER_ID,
                name = "Mes Classiques",
                description = "Les films que je peux regarder encore et encore",
                type = FolderType.MOVIES,
                icon = "⭐",
                colorHex = "#FFFFD700"
            ),
            CustomFolder(
                id = CustomFolder.WATCH_LATER_FOLDER_ID,
                name = "À Regarder Plus Tard",
                description = "Films mis de côté pour plus tard",
                type = FolderType.MOVIES,
                icon = "📋",
                colorHex = "#FF7C4DFF"
            )
        )
        FolderType.SERIES -> listOf(
            CustomFolder(
                id = "my_series",
                name = "Mes Séries",
                description = "Ma collection de séries",
                type = FolderType.SERIES,
                icon = "📺",
                colorHex = "#FF00AEEF"
            ),
            CustomFolder(
                id = "currently_watching",
                name = "En Cours",
                description = "Séries que je regarde actuellement",
                type = FolderType.SERIES,
                icon = "▶️",
                colorHex = "#FFFF3D71"
            ),
            CustomFolder(
                id = "to_start",
                name = "À Découvrir",
                description = "Séries à commencer",
                type = FolderType.SERIES,
                icon = "🔍",
                colorHex = "#FF00E676"
            )
        )
    }
}

/**
 * Extension pour obtenir le nombre d'éléments dans un dossier
 */
fun CustomFolder.itemCount(): Int = itemIds.size
