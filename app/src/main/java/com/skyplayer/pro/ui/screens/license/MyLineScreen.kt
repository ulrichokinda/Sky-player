package com.skyplayer.pro.ui.screens.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import com.skyplayer.pro.ui.screens.home.DashboardViewModel
import com.skyplayer.pro.util.QrCodeUtils
import kotlinx.coroutines.delay

/**
 * Écran "Ma Ligne" — accessible depuis Paramètres > Mes Playlists
 *
 * Affiche:
 * - L'adresse MAC / Device ID (copiable)
 * - Le nom de la playlist active
 * - Le statut d'abonnement (activé / jours d'essai restants)
 * - Un bouton d'activation vers skyplayerapp.xyz
 *
 * Contrairement à TrialExpiredScreen, cet écran EST navigable (bouton Retour actif).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLineScreen(
    onBackClick: () -> Unit,
    licenseViewModel: LicenseViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by licenseViewModel.uiState.collectAsStateWithLifecycle()
    val playlistName by dashboardViewModel.activePlaylistName.collectAsStateWithLifecycle()
    val expiryLabel by dashboardViewModel.expiryLabel.collectAsStateWithLifecycle()
    val expiryColor by dashboardViewModel.expiryColor.collectAsStateWithLifecycle()

    val deviceId = uiState.deviceId
    val isActivated = uiState.isActivated
    val trialDays = uiState.licenseInfo?.trialDaysRemaining ?: 0

    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ma Ligne",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        },
        containerColor = PureBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Statut abonnement ─────────────────────────────────
            val statusIcon = if (isActivated) Icons.Default.CheckCircle
                             else if (trialDays > 0) Icons.Default.Timer
                             else Icons.Default.Warning
            val statusColor = if (isActivated) SuccessGreen
                              else if (trialDays > 0) PremiumGold
                              else WarningOrange

            MyLineCard(
                icon = statusIcon,
                iconTint = statusColor,
                label = "Statut abonnement",
                value = when {
                    isActivated       -> "✅ Activé"
                    trialDays > 0     -> "🎁 Essai — $trialDays jours restants"
                    else              -> "⏰ Essai expiré"
                },
                valueColor = statusColor
            )

            // ── Playlist active ───────────────────────────────────
            MyLineCard(
                icon = Icons.Default.PlaylistPlay,
                iconTint = ElectricSkyBlue,
                label = "Playlist active",
                value = playlistName.ifBlank { "Aucune playlist configurée" }
            )

            // ── Device ID / Adresse MAC ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E0E0E), RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(GradientElectricStart, GradientElectricEnd)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = GradientElectricStart,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Adresse MAC / Identifiant d'activation",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                Text(
                    text = deviceId.ifBlank { "Génération en cours..." },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(
                            listOf(GradientElectricStart, GradientElectricEnd)
                        ),
                        letterSpacing = 1.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (deviceId.isNotBlank()) {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
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

                Text(
                    text = "Communiquez cet identifiant pour activer votre ligne",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.4f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // ── QR Code d'activation ────────────────────────────
            // Généré une seule fois, recomposé seulement si deviceId change
            val qrBitmap = remember(deviceId) {
                if (deviceId.isNotBlank())
                    QrCodeUtils.generateQrCodeForMac(deviceId, size = 400)
                else null
            }

            if (qrBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "QR CODE D'ACTIVATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricSkyBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )

                    // Image QR code
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code d'activation",
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        filterQuality = FilterQuality.None
                    )

                    Text(
                        text = "Scannez pour activer sur skyplayerapp.xyz",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.45f)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "skyplayerapp.xyz/connect?mac=…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = GradientElectricStart.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Bouton activation ─────────────────────────────
            if (!isActivated) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://skyplayerapp.xyz"))
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricSkyBlue)
                ) {
                    Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Activer sur skyplayerapp.xyz",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // ── Pied ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "© 2026 Sky Player Pro",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MyLineCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            )
        }
    }
}
