package com.skyplayer.pro.ui.screens.license

import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.WarningOrange
import kotlinx.coroutines.delay

private const val ACTIVATION_SITE = "skyplayerapp.xyz"
private const val ACTIVATION_URL  = "https://skyplayerapp.xyz"

/**
 * Écran de blocage — expiration d'essai 15 jours.
 *
 * SÉCURITÉ:
 * - BackHandler intercepte et BLOQUE le bouton Retour Android.
 * - L'écran est non-dismissable: pas de bouton de fermeture.
 * - Affiche le Device ID (Virtual MAC) en très grand pour l'activation.
 */
@Composable
fun TrialExpiredScreen(
    viewModel: LicenseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceId = uiState.deviceId
    var copied by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // ══════════════════════════════════════════════════
    // BLOCAGE ABSOLU du bouton Retour Android
    // Ne fait RIEN — l'utilisateur ne peut pas contourner
    // ══════════════════════════════════════════════════
    BackHandler(enabled = true) {
        /* Intentionnellement vide — retour bloqué */
    }

    // Animation pulsante sur le bloc MAC pour attirer l'attention
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mac_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070707), Color(0xFF0D0D0D), PureBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // ── Icône verrou animée ──────────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        WarningOrange.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, WarningOrange.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(48.dp)
                )
            }

            // ── Titre en gras grande taille ──────────────────────
            Text(
                text = "Votre période d'essai de\n15 jours a expiré",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 34.sp
                ),
                textAlign = TextAlign.Center
            )

            // ── Message réglementaire exact ──────────────────────
            Text(
                text = "Veuillez activer votre application sur\nskyplayerapp.xyz",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── MAC / Device ID en TRÈS GRAND au centre ──────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale)  // animation pulsante
                    .background(Color(0xFF0E0E0E), RoundedCornerShape(20.dp))
                    .border(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(GradientElectricStart, GradientElectricEnd)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Votre adresse MAC / Identifiant d'activation",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Adresse MAC en très gros
                Text(
                    text = deviceId.ifBlank { "Génération en cours..." },
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientElectricStart, GradientElectricEnd)
                        ),
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Sous-label
                Text(
                    text = "Notez ou photographiez cet identifiant",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.4f)
                    ),
                    textAlign = TextAlign.Center
                )

                // Bouton copier
                Button(
                    onClick = {
                        if (deviceId.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device MAC", deviceId))
                            copied = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricSkyBlue.copy(alpha = 0.15f),
                        contentColor = ElectricSkyBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copied) "✓ Copié !" else "Copier l'identifiant",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (copied) {
                    LaunchedEffect(Unit) {
                        delay(2500)
                        copied = false
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Bouton principal Activation ──────────────────────
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(ACTIVATION_URL))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricSkyBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Activer maintenant",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // ── Lien texte cliquable ─────────────────────────────
            Text(
                text = ACTIVATION_SITE,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = ElectricSkyBlue,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(ACTIVATION_URL))
                    )
                }
            )

            // ── Note de pied ─────────────────────────────────────
            Text(
                text = "© Sky Player Pro — L'accès est suspendu jusqu'à activation",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.25f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
