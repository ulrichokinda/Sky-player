package com.skyplayer.pro.ui.components.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplayer.pro.data.monitor.StreamHealth
import com.skyplayer.pro.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Panneau détaillé de santé du streaming - renforce la confiance
 * Affiche statistiques en temps réel et historique des adaptations
 */
@Composable
fun StreamHealthDashboard(
    healthState: StreamHealth,
    currentQuality: String = "Auto",
    bandwidthEstimate: Long = 0L,
    bufferHealth: Float = 0f,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            PremiumEmerald.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = null,
                        tint = PremiumEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
                    Column {
                        Text(
                            text = "Santé du streaming",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Informations en temps réel",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // État principal
            HealthStatusCard(healthState = healthState)

            Spacer(modifier = Modifier.height(16.dp))

            // Statistiques détaillées
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    iconTint = PremiumGold,
                    label = "Qualité",
                    value = currentQuality
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.NetworkCheck,
                    iconTint = PremiumEmerald,
                    label = "Bande passante",
                    value = formatBandwidth(bandwidthEstimate)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    iconTint = if (bufferHealth > 0.6f) SuccessGreen else WarningOrange,
                    label = "Tampon",
                    value = "${(bufferHealth * 100).toInt()}%"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconTint = SuccessGreen,
                    label = "État",
                    value = when (healthState) {
                        is StreamHealth.Healthy -> "Excellent"
                        is StreamHealth.Degraded -> "En adaptation"
                        is StreamHealth.UsingAlternative -> "Source alt."
                        is StreamHealth.Unrecoverable -> "Problème"
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Conseils
            InfoTipCard(healthState = healthState)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HealthStatusCard(healthState: StreamHealth) {
    val color: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val title: String
    val description: String
    when (healthState) {
        is StreamHealth.Healthy -> {
            color = SuccessGreen
            icon = Icons.Default.CheckCircle
            title = "Flux stable"
            description = "Votre connexion fonctionne parfaitement !"
        }
        is StreamHealth.Degraded -> {
            color = WarningOrange
            icon = Icons.Default.Troubleshoot
            title = "Adaptation en cours"
            description = "Optimisation de la qualité pour éviter les coupures"
        }
        is StreamHealth.UsingAlternative -> {
            color = PremiumEmerald
            icon = Icons.Default.SwapHoriz
            title = "Source alternative"
            description = "Passage automatique vers une source plus stable"
        }
        is StreamHealth.Unrecoverable -> {
            color = ErrorRed
            icon = Icons.Default.WifiOff
            title = "Problème de connexion"
            description = "Vérifiez votre réseau et réessayez"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        iconTint.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun InfoTipCard(healthState: StreamHealth) {
    val tips = when (healthState) {
        is StreamHealth.Healthy -> listOf(
            "Profitez de votre streaming !",
            "Qualité optimale active"
        )
        is StreamHealth.Degraded -> listOf(
            "Rapprochez-vous de votre routeur Wi-Fi",
            "Fermez les autres applications utilisant le réseau"
        )
        is StreamHealth.UsingAlternative -> listOf(
            "Source alternative sélectionnée automatiquement",
            "Retour à la source principale d'abord possible"
        )
        is StreamHealth.Unrecoverable -> listOf(
            "Vérifiez votre connexion Internet",
            "Redémarrez votre routeur si nécessaire"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            PremiumEmerald.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Conseils",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            tips.forEach { tip ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(PremiumEmerald, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatBandwidth(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "${(bytes / 1_000_000).toInt()} Mbps"
        bytes >= 1_000 -> "${(bytes / 1_000).toInt()} Kbps"
        else -> "$bytes bps"
    }
}
