package com.skyplayer.pro.ui.screens.player

import androidx.media3.common.util.UnstableApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.VideoQuality
import com.skyplayer.pro.ui.screens.player.AdaptiveBitrateManager.NetworkStability
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.GlassWhite
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Dialog de sélection de qualité vidéo avec statut réseau
 * Permet choix manuel ou mode adaptatif
 */
@UnstableApi
@Composable
fun QualitySelectionDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val adaptiveManager = viewModel.abrManager
    val currentQuality by adaptiveManager.currentQuality.collectAsStateWithLifecycle(initialValue = AdaptiveBitrateManager.VideoQuality.AUTO)
    val availableQualities by adaptiveManager.availableQualities.collectAsStateWithLifecycle(initialValue = emptyList())
    val networkStability by adaptiveManager.networkStability.collectAsStateWithLifecycle(initialValue = AdaptiveBitrateManager.NetworkStability.UNKNOWN)
    val bandwidthEstimate by adaptiveManager.bandwidthEstimate.collectAsStateWithLifecycle(initialValue = 0L)
    val recommendation = adaptiveManager.getQualityRecommendation()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = PureBlack.copy(alpha = 0.95f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = PremiumEmerald.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = PremiumEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Qualité Vidéo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Statut réseau
                NetworkStatusCard(
                    stability = networkStability,
                    bandwidthKbps = bandwidthEstimate / 1000,
                    recommendation = recommendation
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Liste des qualités
                Text(
                    text = "Sélectionner la qualité",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableQualities) { quality ->
                        QualityOptionItem(
                            quality = quality,
                            isSelected = currentQuality == quality,
                            onClick = {
                                viewModel.setVideoQuality(quality)
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info mode auto
                AnimatedVisibility(
                    visible = currentQuality == VideoQuality.AUTO,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Mode Auto: La qualité s'adapte automatiquement à votre connexion",
                        style = MaterialTheme.typography.bodySmall,
                        color = PremiumEmerald.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                // Bouton fermer
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Fermer", color = PremiumEmerald)
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    stability: NetworkStability,
    bandwidthKbps: Long,
    recommendation: AdaptiveBitrateManager.QualityRecommendation
) {
    val (backgroundColor, statusText, icon) = when (stability) {
        NetworkStability.EXCELLENT -> 
            Triple(Color(0xFF00E676), "Excellent", Icons.Default.Speed)
        NetworkStability.GOOD -> 
            Triple(Color(0xFF00AEEF), "Bon", Icons.Default.NetworkCheck)
        NetworkStability.STABLE -> 
            Triple(Color(0xFF7C4DFF), "Stable", Icons.Default.NetworkCheck)
        NetworkStability.UNSTABLE -> 
            Triple(Color(0xFFFFD700), "Instable", Icons.Default.NetworkCheck)
        NetworkStability.POOR -> 
            Triple(Color(0xFFFF3D71), "Faible", Icons.Default.NetworkCheck)
        NetworkStability.UNKNOWN -> 
            Triple(Color.Gray, "Analyse...", Icons.Default.NetworkCheck)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = backgroundColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Réseau: $statusText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                if (bandwidthKbps > 0) {
                    Text(
                        text = "Bande passante: ${formatBandwidth(bandwidthKbps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = backgroundColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QualityOptionItem(
    quality: VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when {
        quality == VideoQuality.AUTO -> Icons.Default.Speed
        quality.height >= 1080 -> Icons.Default.HighQuality
        else -> Icons.Default.Tv
    }
    
    val resolutionBadge = when {
        quality.height >= 2160 -> "4K"
        quality.height >= 1080 -> "HD"
        quality.height >= 720 -> "HD"
        else -> "SD"
    }
    
    val badgeColor = when {
        quality.height >= 2160 -> Color(0xFFFFD700) // Or
        quality.height >= 1080 -> Color(0xFF00E676) // Vert
        quality.height >= 720 -> Color(0xFF00AEEF) // Bleu
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                PremiumEmerald.copy(alpha = 0.2f) 
            else 
                GlassWhite.copy(alpha = 0.3f)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = PremiumEmerald
            ) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PremiumEmerald else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                    
                    if (quality != VideoQuality.AUTO) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = resolutionBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Text(
                    text = quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sélectionné",
                    tint = PremiumEmerald,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatBandwidth(kbps: Long): String {
    return when {
        kbps >= 1000 -> String.format("%.1f Mbps", kbps / 1000.0)
        else -> "$kbps kbps"
    }
}
