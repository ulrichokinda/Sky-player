package com.skyplayer.pro.ui.screens.share

import android.net.nsd.NsdServiceInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.localshare.NetworkShareManager
import com.skyplayer.pro.data.localshare.ShareState
import com.skyplayer.pro.ui.theme.*
import com.skyplayer.pro.ui.viewmodel.NetworkShareViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Écran de partage réseau local (Wi-Fi Direct)
 *
 * Permet de :
 * 1. Partager une playlist vers un autre appareil (mode "Envoyer")
 * 2. Recevoir une playlist d'un autre appareil (mode "Recevoir")
 *
 * Sans consommation de data internet - uniquement réseau local Wi-Fi
 */
@Composable
fun NetworkShareScreen(
    playlistToShare: NetworkShareManager.ShareData? = null,
    viewModel: NetworkShareViewModel = hiltViewModel(),
    onPlaylistReceived: (NetworkShareManager.ReceivedShare) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val shareState by viewModel.shareState.collectAsState()
    val discoveredServices by viewModel.discoveredServices.collectAsState()
    
    var selectedMode by remember { mutableStateOf<ShareMode?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var receivedShare by remember { mutableStateOf<NetworkShareManager.ReceivedShare?>(null) }

    // Démarrer le partage automatiquement si des données sont fournies
    LaunchedEffect(playlistToShare) {
        playlistToShare?.let {
            selectedMode = ShareMode.SEND
            viewModel.startSharingPlaylist(it.playlistUrl, it.playlistName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Partage Local",
                style = MaterialTheme.typography.headlineMedium,
                color = ElectricSkyBlue
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Transférez vos playlists sans internet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = selectedMode,
                modifier = Modifier.weight(1f)
            ) { mode ->
                when (mode) {
                    null -> ModeSelectionScreen(
                        onSendSelected = { selectedMode = ShareMode.SEND },
                        onReceiveSelected = { 
                            selectedMode = ShareMode.RECEIVE
                            viewModel.startDiscovery()
                        }
                    )
                    ShareMode.SEND -> SendModeScreen(
                        shareState = shareState,
                        onStopSharing = {
                            viewModel.stopSharing()
                            selectedMode = null
                        }
                    )
                    ShareMode.RECEIVE -> ReceiveModeScreen(
                        shareState = shareState,
                        discoveredServices = discoveredServices,
                        onConnect = { service ->
                            viewModel.connectToDevice(
                                serviceInfo = service,
                                onSuccess = { share ->
                                    receivedShare = share
                                    showSuccessDialog = true
                                    onPlaylistReceived(share)
                                },
                                onError = { _ ->
                                    // Gérer erreur
                                }
                            )
                        },
                        onRefresh = { viewModel.startDiscovery() },
                        onCancel = {
                            viewModel.stopDiscovery()
                            selectedMode = null
                        }
                    )
                }
            }
        }

        // Bouton retour
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Color.White
            )
        }
    }

    // Dialog de succès
    if (showSuccessDialog) {
        SuccessDialog(
            receivedShare = receivedShare,
            onDismiss = {
                showSuccessDialog = false
                selectedMode = null
            }
        )
    }
}

@Composable
private fun ModeSelectionScreen(
    onSendSelected: () -> Unit,
    onReceiveSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Mode Envoyer
        GlassmorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            isFocused = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = ElectricSkyBlue,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Envoyer",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                Text(
                    text = "Partager vers un autre appareil",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onSendSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricSkyBlue
                    )
                ) {
                    Text("Démarrer")
                }
            }
        }

        // Mode Recevoir
        GlassmorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            isFocused = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Recevoir",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                Text(
                    text = "Récupérer depuis un autre appareil",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onReceiveSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold
                    )
                ) {
                    Text("Rechercher", color = PureBlack)
                }
            }
        }
    }
}

@Composable
private fun SendModeScreen(
    shareState: ShareState,
    onStopSharing: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (shareState) {
            is ShareState.Sharing -> {
                // Animation de partage actif
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(ElectricSkyBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = ElectricSkyBlue,
                        strokeWidth = 6.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Partage actif",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                Text(
                    text = "Recherchez cet appareil sur l'autre téléphone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Playlist: ${shareState.data.playlistName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricSkyBlue
                )
            }
            
            is ShareState.Transferring -> {
                Text(
                    text = "Transfert en cours...",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    color = ElectricSkyBlue,
                    trackColor = GlassWhite
                )
            }
            
            ShareState.Completed -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Transfert réussi!",
                    style = MaterialTheme.typography.titleLarge,
                    color = SuccessGreen
                )
            }
            
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = onStopSharing,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ErrorRed
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Arrêter le partage")
        }
    }
}

@Composable
private fun ReceiveModeScreen(
    shareState: ShareState,
    discoveredServices: List<NsdServiceInfo>,
    onConnect: (NsdServiceInfo) -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Appareils disponibles",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            if (shareState is ShareState.Discovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PremiumGold,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rafraîchir",
                        tint = ElectricSkyBlue
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Liste des appareils
        if (discoveredServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Aucun appareil trouvé",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    
                    Text(
                        text = "Vérifiez que les deux appareils sont sur le même Wi-Fi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(discoveredServices) { service ->
                    DeviceItem(
                        serviceInfo = service,
                        onClick = { onConnect(service) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Annuler")
        }
    }
}

@Composable
private fun DeviceItem(
    serviceInfo: NsdServiceInfo,
    onClick: () -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        isFocused = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = ElectricSkyBlue,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = serviceInfo.serviceName.replace("SkyPlayerShare-", ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                
                Text(
                    text = "Appuyez pour recevoir la playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Se connecter",
                    tint = ElectricSkyBlue
                )
            }
        }
    }
}

@Composable
private fun SuccessDialog(
    receivedShare: NetworkShareManager.ReceivedShare?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen
            )
        },
        title = {
            Text("Playlist reçue!")
        },
        text = {
            receivedShare?.let { share ->
                Column {
                    Text(
                        text = "Nom: ${share.shareData.playlistName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Depuis: ${share.senderDeviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricSkyBlue
                )
            ) {
                Text("Importer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        containerColor = CardBlack
    )
}

private enum class ShareMode {
    SEND,       // Mode expéditeur
    RECEIVE     // Mode récepteur
}
