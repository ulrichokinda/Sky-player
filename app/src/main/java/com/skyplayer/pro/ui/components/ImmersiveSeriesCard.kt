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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SeriesColor

/**
 * Carte de série immersive avec ratio cinématographique et effets premium
 * Similaire à ImmersiveMovieCard mais adaptée pour les séries TV
 */
@Composable
fun ImmersiveSeriesCard(
    series: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image avec loading et error states
            SubcomposeAsyncImage(
                model = series.logoUrl,
                contentDescription = series.name,
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
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = series.name.take(1).uppercase(),
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
            
            // Badge "SÉRIE" en haut à gauche
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        SeriesColor.copy(alpha = 0.8f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SÉRIE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = PureBlack,
                    letterSpacing = 1.sp
                )
            }
            
            // Informations mieux organisées
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                // Titre avec shadow pour meilleure lisibilité
                Text(
                    text = series.name,
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
                
                // Saison/épisode si détecté dans le nom
                val seasonInfo = extractSeasonInfo(series.name)
                
                if (seasonInfo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = seasonInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = SeriesColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
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

private fun extractSeasonInfo(name: String): String? {
    // Cherche des patterns comme "S01", "Season 1", "Saison 1"
    val seasonPatterns = listOf(
        "\\bS(\\d{1,2})\\b".toRegex(), // S01, S1
        "\\bSeason\\s*(\\d{1,2})\\b".toRegex(RegexOption.IGNORE_CASE), // Season 1
        "\\bSaison\\s*(\\d{1,2})\\b".toRegex(RegexOption.IGNORE_CASE) // Saison 1
    )
    
    for (pattern in seasonPatterns) {
        val match = pattern.find(name)
        if (match != null) {
            val seasonNumber = match.groupValues[1]
            return "S${seasonNumber.padStart(2, '0')}"
        }
    }
    
    // Cherche "Complete" ou "Complet"
    if (name.contains("Complete", ignoreCase = true) || 
        name.contains("Complet", ignoreCase = true)) {
        return "Complète"
    }
    
    return null
}
