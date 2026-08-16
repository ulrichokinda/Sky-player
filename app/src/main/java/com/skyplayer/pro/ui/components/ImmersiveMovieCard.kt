package com.skyplayer.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Carte de film immersive avec ratio cinématographique et effets premium
 * Utilise un ratio de 0.7f (format plus cinématographique que le 0.67f standard)
 */
@Composable
fun ImmersiveMovieCard(
    movie: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PremiumEmerald
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f) // Format plus cinématographique
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBlack
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = accentColor.copy(alpha = 0.22f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image avec loading et error states
            SubcomposeAsyncImage(
                model = movie.logoUrl,
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    PremiumShimmerCard(
                        modifier = Modifier.fillMaxSize()
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ElevatedBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = movie.name.take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            )
            
            // Overlay gradient plus subtil et élégant
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 0.3f
                        )
                    )
            )
            
            // Informations mieux organisées
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                // Badge qualité (HD/4K) si détecté dans le nom
                if (movie.name.contains("4K", true) || movie.name.contains("UHD", true)) {
                    QualityBadge("4K", PremiumGold)
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (movie.name.contains("HD", true) || movie.name.contains("1080", true)) {
                    QualityBadge("HD", PremiumEmerald)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Titre avec shadow pour meilleure lisibilité
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Année et genre si disponibles (extraits du nom)
                val year = extractYear(movie.name)
                val genre = extractGenre(movie.name)
                
                if (year != null || genre != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        if (year != null) {
                            Text(
                                text = year,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (year != null && genre != null) {
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        if (genre != null) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Bouton play discret au centre
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun QualityBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PureBlack,
            letterSpacing = 0.5.sp
        )
    }
}

private fun extractYear(name: String): String? {
    // Cherche une année entre 1900 et 2099
    val yearPattern = "\\b(19|20)\\d{2}\\b".toRegex()
    val match = yearPattern.find(name)
    return match?.value
}

private fun extractGenre(name: String): String? {
    // Liste de genres courants à détecter
    val genres = listOf(
        "Action", "Comedy", "Drama", "Horror", "Thriller",
        "Sci-Fi", "Romance", "Adventure", "Animation", "Crime",
        "Fantasy", "Mystery", "War", "Western", "Documentary"
    )
    
    for (genre in genres) {
        if (name.contains(genre, ignoreCase = true)) {
            return genre
        }
    }
    return null
}
