package com.skyplayer.pro.ui.components.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.repository.Recommendation
import com.skyplayer.pro.data.repository.RecommendationType
import com.skyplayer.pro.ui.theme.*

/**
 * Section de recommandations intelligentes pour l'écran d'accueil
 *
 * Affiche :
 * - Recommandations contextuelles (jour + heure)
 * - Alertes proactives (30 min avant créneau)
 * - Chaînes favorites mises en avant
 * - Design Glassmorphism premium
 */
@Composable
fun SmartRecommendationsSection(
    recommendations: List<Recommendation>,
    highlightedChannel: String?,
    onChannelClick: (Channel) -> Unit,
    onDismissProactive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricSkyBlue,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Pour vous",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            
            // Badge si recommandation proactive active
            recommendations.find { it.type == RecommendationType.PROACTIVE }?.let {
                ProactiveBadge()
            }
        }

        // Liste horizontale des recommandations
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(recommendations) { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    isHighlighted = recommendation.channels.any { it.id == highlightedChannel },
                    onChannelClick = { channel ->
                        onChannelClick(channel)
                        if (recommendation.type == RecommendationType.PROACTIVE) {
                            onDismissProactive()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Badge pour notification proactive
 */
@Composable
private fun ProactiveBadge() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PremiumGold.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = PremiumGold.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Bientôt",
                style = MaterialTheme.typography.labelMedium,
                color = PremiumGold
            )
        }
    }
}

/**
 * Carte de recommandation individuelle
 */
@Composable
private fun RecommendationCard(
    recommendation: Recommendation,
    isHighlighted: Boolean,
    onChannelClick: (Channel) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1f,
        label = "scale"
    )

    Card(
        modifier = Modifier
            .width(280.dp)
            .scale(scale)
            .clickable { 
                recommendation.channels.firstOrNull()?.let { onChannelClick(it) }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.type) {
                RecommendationType.PROACTIVE -> PremiumGold.copy(alpha = 0.15f)
                RecommendationType.CONTEXTUAL -> ElectricSkyBlue.copy(alpha = 0.15f)
                RecommendationType.FAVORITES -> SuccessGreen.copy(alpha = 0.15f)
                RecommendationType.DISCOVERY -> GlassWhite.copy(alpha = 0.1f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = when (recommendation.type) {
                RecommendationType.PROACTIVE -> PremiumGold.copy(alpha = if (isHighlighted) 0.8f else 0.5f)
                RecommendationType.CONTEXTUAL -> ElectricSkyBlue.copy(alpha = if (isHighlighted) 0.8f else 0.5f)
                RecommendationType.FAVORITES -> SuccessGreen.copy(alpha = if (isHighlighted) 0.8f else 0.5f)
                RecommendationType.DISCOVERY -> GlassWhite.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header avec icône et titre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = recommendation.icon,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = recommendation.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini liste des chaînes
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recommendation.channels.take(3).forEach { channel ->
                    ChannelRowMini(
                        channel = channel,
                        isHighlighted = channel.id == isHighlighted.toString(),
                        onClick = { onChannelClick(channel) }
                    )
                }
            }

            // Indicateur de confiance
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { recommendation.confidence.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = when (recommendation.type) {
                    RecommendationType.PROACTIVE -> PremiumGold
                    RecommendationType.CONTEXTUAL -> ElectricSkyBlue
                    RecommendationType.FAVORITES -> SuccessGreen
                    RecommendationType.DISCOVERY -> TextSecondary
                },
                trackColor = GlassWhite.copy(alpha = 0.2f)
            )
        }
    }
}

/**
 * Ligne de chaîne miniature
 */
@Composable
private fun ChannelRowMini(
    channel: Channel,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isHighlighted) {
        ElectricSkyBlue.copy(alpha = 0.3f)
    } else {
        GlassWhite.copy(alpha = 0.1f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Logo placeholder
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isHighlighted) ElectricSkyBlue.copy(alpha = 0.5f)
                    else GlassWhite.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.name.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlighted) Color.White else TextSecondary
            )
        }

        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (channel.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Notification proactive flottante (30 min avant créneau)
 */
@Composable
fun ProactiveNotificationBanner(
    recommendation: Recommendation?,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = recommendation != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        recommendation?.let { rec ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PremiumGold.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = PremiumGold.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icône animée
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PremiumGold.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Votre créneau commence bientôt",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold
                        )
                        
                        Text(
                            text = rec.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        
                        Text(
                            text = "${rec.channels.size} chaînes disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section "Parce que vous aimez..."
 */
@Composable
fun BecauseYouWatchedSection(
    watchedChannel: Channel,
    similarChannels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (similarChannels.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Parce que vous regardez ${watchedChannel.name}",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(similarChannels) { channel ->
                SimilarChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

/**
 * Carte chaîne similaire compacte
 */
@Composable
private fun SimilarChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassWhite.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricSkyBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
