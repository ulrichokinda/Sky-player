package com.skyplayer.pro.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Sd
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplayer.pro.data.model.VideoQuality

/**
 * Sélecteur de qualité vidéo pour optimiser la connexion
 */
@Composable
fun QualitySelector(
    currentQuality: VideoQuality,
    onQualitySelected: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Qualité Vidéo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choisissez une qualité adaptée à votre connexion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options de qualité
            VideoQuality.values().forEach { quality ->
                QualityOption(
                    quality = quality,
                    isSelected = currentQuality == quality,
                    onClick = { onQualitySelected(quality) }
                )
            }
        }
    }
}

@Composable
private fun QualityOption(
    quality: VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (quality) {
        VideoQuality.AUTO -> Icons.Default.NetworkCheck
        VideoQuality.LOW -> Icons.Default.Sd
        VideoQuality.MEDIUM -> Icons.Default.Videocam
        VideoQuality.HIGH -> Icons.Default.HighQuality
        VideoQuality.UHD -> Icons.Default.Speed
    }

    val color = when (quality) {
        VideoQuality.AUTO -> MaterialTheme.colorScheme.primary
        VideoQuality.LOW -> Color(0xFF4CAF50) // Vert
        VideoQuality.MEDIUM -> Color(0xFFFF9800) // Orange
        VideoQuality.HIGH -> Color(0xFFF44336) // Rouge
        VideoQuality.UHD -> Color(0xFF9C27B0) // Violet
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icône
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Texte
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quality.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            val description = when (quality) {
                VideoQuality.AUTO -> "S'adapte automatiquement à votre connexion"
                VideoQuality.LOW -> "Idéal pour connexions lentes (< 1 Mbps)"
                VideoQuality.MEDIUM -> "Bon équilibre qualité/débit (2-3 Mbps)"
                VideoQuality.HIGH -> "Qualité HD, nécessite 5+ Mbps"
                VideoQuality.UHD -> "4K Ultra HD, nécessite 15+ Mbps"
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Radio button
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}

/**
 * Section de configuration avancée du streaming
 */
@Composable
fun StreamingAdvancedSettings(
    autoAdjustQuality: Boolean,
    onAutoAdjustChanged: (Boolean) -> Unit,
    bufferDuration: Int,
    onBufferDurationChanged: (Int) -> Unit,
    lowLatencyMode: Boolean,
    onLowLatencyChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Options Avancées",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ajustement automatique
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ajustement automatique",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Adapte la qualité selon votre connexion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoAdjustQuality,
                    onCheckedChange = onAutoAdjustChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Durée du tampon
            Text(
                text = "Durée du tampon: ${bufferDuration}s",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Plus le tampon est grand, moins il y a de risques de saccades",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = bufferDuration.toFloat(),
                onValueChange = { onBufferDurationChanged(it.toInt()) },
                valueRange = 10f..90f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode faible latence
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mode faible latence",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Réduit le délai mais augmente les risques de buffering",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = lowLatencyMode,
                    onCheckedChange = onLowLatencyChanged
                )
            }
        }
    }
}

/**
 * Badge indiquant la qualité actuelle
 */
@Composable
fun CurrentQualityBadge(
    quality: VideoQuality,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (quality) {
        VideoQuality.AUTO -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        VideoQuality.LOW -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        VideoQuality.MEDIUM -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        VideoQuality.HIGH -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        VideoQuality.UHD -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = quality.label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Indicateur de vitesse de connexion
 */
@Composable
fun NetworkSpeedIndicator(
    speedKbps: Long,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when {
        speedKbps < 1_000 -> Triple(
            Icons.Default.Sd,
            Color(0xFFF44336),
            "Connexion lente"
        )
        speedKbps < 3_000 -> Triple(
            Icons.Default.Videocam,
            Color(0xFFFF9800),
            "Connexion moyenne"
        )
        speedKbps < 6_000 -> Triple(
            Icons.Default.HighQuality,
            Color(0xFF4CAF50),
            "Bonne connexion"
        )
        else -> Triple(
            Icons.Default.Speed,
            Color(0xFF2196F3),
            "Excellente connexion"
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "${speedKbps / 1000} Mbps - $label",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Boutons rapides pour modes prédéfinis
 */
@Composable
fun QuickStreamingModes(
    onDataSaverClick: () -> Unit,
    onBalancedClick: () -> Unit,
    onPerformanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeButton(
            label = "Économie",
            description = "Données limitées",
            color = Color(0xFF4CAF50),
            onClick = onDataSaverClick,
            modifier = Modifier.weight(1f)
        )
        ModeButton(
            label = "Équilibré",
            description = "Qualité standard",
            color = Color(0xFFFF9800),
            onClick = onBalancedClick,
            modifier = Modifier.weight(1f)
        )
        ModeButton(
            label = "Performance",
            description = "Maximale qualité",
            color = Color(0xFF2196F3),
            onClick = onPerformanceClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
