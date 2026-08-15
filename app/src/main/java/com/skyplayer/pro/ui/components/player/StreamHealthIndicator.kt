package com.skyplayer.pro.ui.components.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.monitor.FallbackInfo
import com.skyplayer.pro.data.monitor.StreamHealth
import com.skyplayer.pro.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Indicateur de santé du stream - UI Premium sans messages d'erreur brut
 *
 * Affichage élégant des changements automatiques :
 * - Transition vers miroir (indicateur subtil)
 * - Changement de chaîne alternative (notification glassmorphism)
 * - Pas de message d'erreur technique visible
 */
@Composable
fun StreamHealthIndicator(
    healthState: StreamHealth,
    fallbackInfo: FallbackInfo?,
    modifier: Modifier = Modifier
) {
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationIcon by remember { mutableStateOf(Icons.Default.Info) }
    var notificationColor by remember { mutableStateOf(ElectricSkyBlue) }

    // Détecter les changements et afficher notification élégante
    LaunchedEffect(healthState, fallbackInfo) {
        when {
            healthState is StreamHealth.UsingAlternative -> {
                val alt = healthState.alternative
                notificationMessage = "Passage à ${alt.name}"
                notificationIcon = Icons.Default.SwitchAccessShortcut
                notificationColor = WarningOrange
                showNotification = true
            }
            fallbackInfo is FallbackInfo.Mirror -> {
                notificationMessage = "Optimisation du flux..."
                notificationIcon = Icons.Default.Speed
                notificationColor = ElectricSkyBlue
                showNotification = true
            }
            healthState is StreamHealth.Degraded -> {
                notificationMessage = "Recherche meilleure qualité..."
                notificationIcon = Icons.Default.Search
                notificationColor = PremiumGold
                showNotification = true
            }
            healthState is StreamHealth.Healthy -> {
                delay(2000) // Garder visible un moment puis cacher
                showNotification = false
            }
        }

        // Auto-hide après 5 secondes si pas Healthy
        if (showNotification) {
            delay(5000)
            showNotification = false
        }
    }

    // Notification élégante glassmorphism
    AnimatedVisibility(
        visible = showNotification,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        GlassmorphismHealthCard(
            message = notificationMessage,
            icon = notificationIcon,
            accentColor = notificationColor
        )
    }
}

/**
 * Carte Glassmorphism pour notification de santé
 */
@Composable
private fun GlassmorphismHealthCard(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône animée
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        accentColor.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Message
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Sous-texte indicatif
                Text(
                    text = "Adaptation automatique",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Indicateur subtil (pas de spinner agressif)
            if (accentColor == ElectricSkyBlue || accentColor == PremiumGold) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp),
                    color = accentColor,
                    trackColor = GlassWhite.copy(alpha = 0.3f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Indicateur discret de qualité en overlay
 */
@Composable
fun QualityIndicatorOverlay(
    healthState: StreamHealth,
    modifier: Modifier = Modifier
) {
    val qualityIcon = when (healthState) {
        is StreamHealth.Healthy -> Icons.Default.HighQuality
        is StreamHealth.Degraded -> Icons.Default.NetworkCheck
        is StreamHealth.UsingAlternative -> Icons.Default.SyncAlt
        is StreamHealth.Unrecoverable -> Icons.Default.SignalWifiOff
    }

    val qualityColor = when (healthState) {
        is StreamHealth.Healthy -> SuccessGreen
        is StreamHealth.Degraded -> WarningOrange
        is StreamHealth.UsingAlternative -> ElectricSkyBlue
        is StreamHealth.Unrecoverable -> ErrorRed
    }

    val alpha = if (healthState is StreamHealth.Healthy) 0.3f else 0.8f

    Box(
        modifier = modifier
            .alpha(alpha)
            .background(
                color = PureBlack.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = qualityIcon,
                contentDescription = null,
                tint = qualityColor,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = when (healthState) {
                    is StreamHealth.Healthy -> "HD"
                    is StreamHealth.Degraded -> "ADJ"
                    is StreamHealth.UsingAlternative -> "ALT"
                    is StreamHealth.Unrecoverable -> "OFF"
                },
                style = MaterialTheme.typography.labelSmall,
                color = qualityColor
            )
        }
    }
}

/**
 * Composant complet pour le player avec gestion santé
 */
@Composable
fun PlayerHealthOverlay(
    healthState: StreamHealth,
    fallbackInfo: FallbackInfo?,
    currentChannel: Channel?,
    alternativeChannel: Channel?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Indicateur principal en haut
        StreamHealthIndicator(
            healthState = healthState,
            fallbackInfo = fallbackInfo,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Indicateur discret en bas à droite
        QualityIndicatorOverlay(
            healthState = healthState,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // Affichage de l'alternative trouvée (si changement automatique)
        AnimatedVisibility(
            visible = healthState is StreamHealth.UsingAlternative && alternativeChannel != null,
            enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AlternativeChannelCard(
                originalChannel = currentChannel,
                alternativeChannel = alternativeChannel,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Carte affichant le changement vers alternative
 */
@Composable
private fun AlternativeChannelCard(
    originalChannel: Channel?,
    alternativeChannel: Channel?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ElectricSkyBlue.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SwitchAccessShortcut,
                contentDescription = null,
                tint = ElectricSkyBlue,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Flux optimisé",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Passage automatique à une chaîne similaire",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Détails du changement
            if (originalChannel != null && alternativeChannel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Original (barré/fadé)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = originalChannel.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                        Text(
                            text = "Indisponible",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ElectricSkyBlue,
                        modifier = Modifier.alpha(0.7f)
                    )

                    // Nouveau (highlight)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = alternativeChannel.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "En cours",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricSkyBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continuer")
            }
        }
    }
}
