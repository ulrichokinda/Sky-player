package com.skyplayer.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.data.model.ContentType

/**
 * Composant d'affiche optimisé selon le type de contenu
 * - Films: ratio 2:3 (portrait)
 * - Séries: ratio 2:3 (portrait)
 * - Live TV: ratio 1:1 ou 4:3 (carré)
 */
@Composable
fun ContentPoster(
    channel: Channel,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onClick: (() -> Unit)? = null,
    metadata: ContentMetadata? = null
) {
    when (channel.type) {
        ContentType.VOD_MOVIE, ContentType.VOD_SERIES -> {
            MoviePoster(
                channel = channel,
                modifier = modifier,
                showTitle = showTitle,
                onClick = onClick,
                metadata = metadata
            )
        }
        else -> {
            ChannelLogo(
                channel = channel,
                modifier = modifier,
                showTitle = showTitle,
                onClick = onClick
            )
        }
    }
}

/**
 * Affiche de film/série avec ratio 2:3
 * Affiche les métadonnées (année, rating) si disponibles
 */
@Composable
private fun MoviePoster(
    channel: Channel,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onClick: (() -> Unit)? = null,
    metadata: ContentMetadata? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(2f / 3f) // Ratio portrait cinéma
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            SubcomposeAsyncImage(
                model = metadata?.posterUrl ?: channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                gradientBrush(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Badge année en haut
            metadata?.year?.let { year ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = year.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badge rating si disponible
            metadata?.imdbRating?.let { rating ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = Color(0xFFFFB300),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ $rating",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Overlay avec titre et infos
            if (showTitle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Titre
                    Text(
                        text = metadata?.title ?: channel.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    // Durée si disponible
                    metadata?.duration?.let { duration ->
                        Text(
                            text = duration,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Logo de chaîne avec ratio 1:1 ou 4:3
 */
@Composable
private fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(4f / 3f) // Ratio standard TV
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Fond
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )

            // Logo
            SubcomposeAsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (channel.logoUrl != null) 8.dp else 16.dp),
                contentScale = if (channel.logoUrl != null) ContentScale.Fit else ContentScale.Crop,
                loading = {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // Titre sous le logo
            if (showTitle) {
                Text(
                    text = channel.name,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Petite variante pour les listes horizontales
 */
@Composable
fun CompactContentPoster(
    channel: Channel,
    modifier: Modifier = Modifier,
    size: PosterSize = PosterSize.MEDIUM
) {
    val (width, height) = when (size) {
        PosterSize.SMALL -> Pair(100.dp, 150.dp)   // Films
        PosterSize.MEDIUM -> Pair(120.dp, 180.dp)   // Films
        PosterSize.LARGE -> Pair(150.dp, 225.dp) // Films
    }

    val isMovie = channel.type == ContentType.VOD_MOVIE || channel.type == ContentType.VOD_SERIES

    Card(
        modifier = modifier
            .size(
                width = if (isMovie) width else width * 1.2f,
                height = if (isMovie) height else width * 0.9f
            ),
        shape = RoundedCornerShape(if (isMovie) 12.dp else 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (isMovie) ContentScale.Crop else ContentScale.Fit,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            )
        }
    }
}

enum class PosterSize {
    SMALL, MEDIUM, LARGE
}

/**
 * Helper pour créer un dégradé
 */
@Composable
private fun gradientBrush(startColor: Color, endColor: Color): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(startColor, endColor)
    )
}
