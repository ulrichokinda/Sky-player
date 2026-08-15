package com.skyplayer.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.WarningOrange

/**
 * Menu avancé pour les options supplémentaires du dashboard
 * Affiché quand l'utilisateur clique sur la tuile "PLUS"
 */
@Composable
fun AdvancedMenuDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRemoteConfig: () -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    onNavigateToEditPlaylist: () -> Unit = onNavigateToAddPlaylist,
    onNavigateToParental: () -> Unit
) {
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // En-tête
                Text(
                    text = "Options Avancées",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Option: Réglages
                AdvancedMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Réglages",
                    description = "Paramètres de l'application",
                    color = Color.Gray,
                    onClick = {
                        onNavigateToSettings()
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Option: Contrôle Parental
                AdvancedMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Contrôle Parental",
                    description = "Protéger l'accès aux contenus sensibles",
                    color = WarningOrange,
                    onClick = {
                        onNavigateToParental()
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Option: QR Sync
                AdvancedMenuItem(
                    icon = Icons.Default.QrCodeScanner,
                    title = "QR Sync",
                    description = "Synchroniser avec TV via QR Code",
                    color = ElectricSkyBlue,
                    onClick = {
                        onNavigateToRemoteConfig()
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Option: Ajouter Playlist
                AdvancedMenuItem(
                    icon = Icons.Default.Add,
                    title = "Ajouter Playlist",
                    description = "Importer une nouvelle playlist M3U",
                    color = PremiumGold,
                    onClick = {
                        onNavigateToAddPlaylist()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option: Gérer les playlists
                AdvancedMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Gérer les playlists",
                    description = "Renommer, activer ou supprimer",
                    color = Color(0xFF9C27B0),
                    onClick = {
                        onNavigateToEditPlaylist()
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Bouton Annuler
                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Annuler",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color.copy(alpha = 0.2f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            // Texte
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
