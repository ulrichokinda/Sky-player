package com.skyplayer.pro.ui.screens.welcome

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange

/**
 * Écran de bienvenue pour première utilisation
 * Invite l'utilisateur à ajouter sa première playlist
 */
@Composable
fun WelcomeScreen(
    onAddPlaylist: () -> Unit,
    onRemoteConfig: () -> Unit = {},
    onSkip: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val playlistStatus by viewModel.playlistStatus

    val finishOnboarding: () -> Unit = {
        viewModel.completeOnboarding()
        onSkip()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo avec effet glassmorphism
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GradientElectricStart.copy(alpha = 0.8f),
                                GradientElectricEnd.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Halo glow interne
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                    androidx.compose.ui.graphics.Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Sky Player Pro",
                    modifier = Modifier.size(70.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Titre avec gradient électrique
            Text(
                text = "Sky Player",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GradientElectricStart, GradientElectricEnd)
                    )
                )
            )
            
            // Badge PRO en or avec glassmorphism
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(
                        color = PremiumGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumGold,
                        letterSpacing = 8.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sous-titre
            Text(
                text = "La meilleure expérience IPTV",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Optimisée pour l'Afrique",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Bouton principal avec glassmorphism
            Button(
                onClick = onAddPlaylist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricSkyBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddLink,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = "Ajouter une playlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Bouton Configuration TV par QR Code
            OutlinedButton(
                onClick = onRemoteConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CardBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ElectricSkyBlue
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = "Configurer par QR Code (TV)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ElectricSkyBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicateur dynamique de l'état de la playlist
            when (playlistStatus) {
                PlaylistState.LOADING -> {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = false,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardBlack
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricSkyBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Vérification...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                PlaylistState.LOADED -> {
                    Button(
                        onClick = finishOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = SuccessGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Playlist chargée — Accéder au contenu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SuccessGreen
                        )
                    }
                }
                PlaylistState.EMPTY -> {
                    OutlinedButton(
                        onClick = onAddPlaylist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aucune playlist — Veuillez ajouter un flux",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarningOrange
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItem("✓", "Lecture optimisée réseaux lents (Edge/3G/4G)", ElectricSkyBlue)
                FeatureItem("✓", "Buffering agressif 90-120s sans coupure", ElectricSkyBlue)
                FeatureItem("✓", "Reconnexion automatique exponentielle", ElectricSkyBlue)
                FeatureItem("✓", "Multi-vue: 2-4 chaînes simultanées", PremiumGold)
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: String, text: String, iconColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge,
            color = iconColor
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
