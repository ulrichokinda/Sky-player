package com.skyplayer.pro.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.data.remote.DownloadProgress
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange

/**
 * Écran de téléchargement progressif de la playlist M3U
 *
 * Affiché automatiquement quand le serveur détecte une playlist
 * associée à l'adresse MAC de l'appareil.
 *
 * Affiche en temps réel : X.X Mo / Y.Y Mo + barre de progression animée.
 * Navigue automatiquement vers Home dès que le parsing est terminé.
 */
@Composable
fun DownloadProgressScreen(
    onDownloadComplete: () -> Unit,
    viewModel: DownloadProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Naviguer automatiquement quand terminé
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onDownloadComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 32.dp)
        ) {

            // ── Icône animée ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                ElectricSkyBlue.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.error != null -> Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(52.dp)
                    )
                    state.isComplete -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(52.dp),
                            color = ElectricSkyBlue,
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = ElectricSkyBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ── Titre playlist ───────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        state.error != null  -> "Erreur de téléchargement"
                        state.isComplete     -> "Playlist prête !"
                        else                 -> "Chargement de votre playlist"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                if (state.playlistName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.playlistName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PremiumGold,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Bloc progression ─────────────────────────────────────
            if (state.error == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0E0E), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Barre de progression animée
                    val animatedProgress by animateFloatAsState(
                        targetValue = if (state.progress.percent in 0..100)
                            state.progress.percent / 100f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "download_progress"
                    )

                    if (state.progress.percent >= 0) {
                        // Taille connue → LinearProgressIndicator déterministe
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricSkyBlue,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        // Taille inconnue (chunked) → indéterministe
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricSkyBlue,
                            trackColor = Color.White.copy(alpha = 0.08f),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Texte "Téléchargement de la playlist : X.X Mo / Y.Y Mo"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                state.isComplete ->
                                    "✓ ${"%.1f".format(state.progress.readMb)} Mo chargés"
                                state.progress.totalBytes > 0 ->
                                    "Téléchargement de la playlist :"
                                else ->
                                    "Téléchargement en cours..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        )
                        Text(
                            text = when {
                                state.isComplete ->
                                    "100%"
                                state.progress.percent >= 0 ->
                                    "${state.progress.percent}%"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ElectricSkyBlue,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Label Mo/Mo en monospace — mis à jour en temps réel
                    if (!state.isComplete) {
                        Text(
                            text = state.progress.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                brush = Brush.horizontalGradient(
                                    listOf(GradientElectricStart, GradientElectricEnd)
                                )
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Nb chaînes après parsing
                    if (state.isComplete && state.channelCount > 0) {
                        Text(
                            text = "${state.channelCount} chaînes importées avec succès",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SuccessGreen
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Erreur ───────────────────────────────────────────────
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WarningOrange
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.retry() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricSkyBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Réessayer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
