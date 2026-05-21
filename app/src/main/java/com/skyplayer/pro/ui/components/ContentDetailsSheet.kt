package com.skyplayer.pro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.data.model.ContentType

/**
 * Bottom Sheet affichant les détails d'un film ou série
 * Date de sortie, description, acteurs, réalisateur
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailsSheet(
    channel: Channel,
    metadata: ContentMetadata?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    isFavorite: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ContentDetailsContent(
                channel = channel,
                metadata = metadata,
                onPlayClick = onPlayClick,
                onFavoriteClick = onFavoriteClick,
                isFavorite = isFavorite
            )
        }
    }
}

@Composable
private fun ContentDetailsContent(
    channel: Channel,
    metadata: ContentMetadata?,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Header avec poster et infos principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poster
            Card(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = metadata?.posterUrl ?: channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Infos principales
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = metadata?.title ?: channel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date de sortie
                metadata?.releaseDate?.let { date ->
                    InfoRow(
                        icon = Icons.Default.DateRange,
                        text = formatReleaseDate(date),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Durée
                metadata?.duration?.let { duration ->
                    InfoRow(
                        icon = Icons.Default.Schedule,
                        text = duration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Note
                metadata?.imdbRating?.let { rating ->
                    if (rating.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.Star,
                            text = "$rating/10",
                            color = Color(0xFFFFB300)
                        )
                    }
                }

                // Réalisateur
                metadata?.director?.let { director ->
                    if (director.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.Person,
                            text = "De $director",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            isItalic = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Genres
        metadata?.getGenresList()?.let { genres ->
            if (genres.isNotEmpty()) {
                GenreChips(genres = genres)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Synopsis/Description
        Text(
            text = "Synopsis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = metadata?.plot ?: "Aucune description disponible pour ce contenu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Acteurs
        metadata?.getActorsList()?.let { actors ->
            if (actors.isNotEmpty()) {
                Text(
                    text = "Avec",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActorsRow(actors = actors.take(6))

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bouton Lecture
            Button(
                onClick = onPlayClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Regarder")
            }

            // Bouton Favoris
            Button(
                onClick = onFavoriteClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) 
                        Icons.Default.Star 
                    else 
                        Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFavorite) "Retirer" else "Favoris")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    isItalic: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(genres: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            FilterChip(
                selected = false,
                onClick = { },
                label = { Text(genre) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ActorsRow(actors: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        actors.forEach { actor ->
            ActorChip(name = actor)
        }
    }
}

@Composable
private fun ActorChip(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Formate la date de sortie
 */
private fun formatReleaseDate(date: String): String {
    return try {
        // Essaie différents formats
        when {
            date.contains("-") -> {
                // Format YYYY-MM-DD
                val parts = date.split("-")
                when (parts.size) {
                    3 -> "Sorti le ${parts[2]}/${parts[1]}/${parts[0]}"
                    2 -> "Sorti en ${parts[1]}/${parts[0]}"
                    1 -> "Sorti en ${parts[0]}"
                    else -> "Sorti en $date"
                }
            }
            date.length == 4 -> "Sorti en $date"  // Juste l'année
            else -> "Sorti le $date"
        }
    } catch (e: Exception) {
        "Date: $date"
    }
}

/**
 * Variante compacte pour l'affichage inline (dans les grilles)
 */
@Composable
fun ContentMetadataOverlay(
    metadata: ContentMetadata?,
    modifier: Modifier = Modifier
) {
    if (metadata == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(12.dp)
    ) {
        // Titre
        Text(
            text = metadata.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Année et durée
        val info = buildString {
            metadata.year?.let { append(it) }
            metadata.duration?.let { 
                if (isNotEmpty()) append(" • ")
                append(it) 
            }
        }

        if (info.isNotEmpty()) {
            Text(
                text = info,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }

        // Rating
        metadata.imdbRating?.let { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFFFFB300)
                )
                Text(
                    text = rating,
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
