package com.skyplayer.pro.ui.screens.license

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

private const val ACTIVATION_SITE_URL = "https://skyplayerapp.xyz"

/**
 * Écran de licence et d'activation
 * Affiche le statut de l'essai, l'ID appareil, et permet l'activation
 */
@Composable
fun LicenseScreen(
    onNavigateToHome: () -> Unit,
    viewModel: LicenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Vérifier si l'accès est autorisé
    LaunchedEffect(uiState.hasValidAccess) {
        if (uiState.hasValidAccess && !uiState.isLoading) {
            delay(500) // Petit délai pour montrer le succès
            onNavigateToHome()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header avec statut
            LicenseHeader(uiState)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ID Appareil (toujours visible)
            DeviceIdCard(
                deviceId = uiState.deviceId,
                onRefresh = { viewModel.refreshLicenseStatus() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statut détaillé
            LicenseStatusCard(uiState)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Message de blocage si essai expiré
            if (uiState.showTrialExpired && !uiState.isActivated) {
                TrialExpiredCard(
                    onOpenActivationSite = {
                        openActivationSite(context)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mention légale obligatoire
            LegalNoticeCard()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Boutons d'action
            ActionButtons(
                uiState = uiState,
                onRefresh = { viewModel.refreshLicenseStatus() },
                onHealthCheck = { viewModel.performHealthCheck() },
                onOpenActivationSite = {
                    openActivationSite(context)
                }
            )
        }
    }
}

@Composable
private fun LicenseHeader(uiState: LicenseViewModel.LicenseUiState) {
    val icon = when {
        uiState.isActivated -> Icons.Default.CheckCircle
        uiState.licenseInfo?.isTrialValid == true -> Icons.Default.Info
        else -> Icons.Default.Lock
    }
    
    val color = when {
        uiState.isActivated -> SuccessGreen
        uiState.licenseInfo?.isTrialValid == true -> GradientElectricStart
        else -> WarningOrange
    }
    
    val title = when {
        uiState.isActivated -> "Application Activée"
        uiState.licenseInfo?.isTrialValid == true -> "Essai Gratuit"
        else -> "Activation Requise"
    }
    
    val subtitle = when {
        uiState.isActivated -> "Votre accès est illimité"
        uiState.licenseInfo?.isTrialValid == true -> 
            "${uiState.licenseInfo?.trialDaysRemaining} jours restants"
        else -> "La période d'essai est terminée"
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = color
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
    }
}

@Composable
private fun DeviceIdCard(
    deviceId: String,
    onRefresh: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Identifiant Appareil",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rafraîchir"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Affichage de l'ID en format MAC
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GradientElectricStart.copy(alpha = 0.1f),
                                GradientElectricEnd.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deviceId,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = GradientElectricStart
                    )
                    
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(deviceId))
                            copied = true
                            Timber.i("ID copied to clipboard")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier",
                            tint = GradientElectricStart
                        )
                    }
                }
            }
            
            if (copied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Copié !",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    modifier = Modifier.align(Alignment.End)
                )
                LaunchedEffect(Unit) {
                    delay(2000)
                    copied = false
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Transmettez cet ID à votre revendeur pour activation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LicenseStatusCard(uiState: LicenseViewModel.LicenseUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Statut de la Licence",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statut Essai
            StatusRow(
                label = "Période d'essai",
                value = if (uiState.licenseInfo?.isTrialValid == true) 
                    "${uiState.licenseInfo?.trialDaysRemaining} jours restants" 
                else 
                    "Expirée",
                isActive = uiState.licenseInfo?.isTrialValid == true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Statut Activation
            StatusRow(
                label = "Activation",
                value = if (uiState.isActivated) "Activée" else "En attente",
                isActive = uiState.isActivated
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Connexion Firebase
            StatusRow(
                label = "Connexion serveur",
                value = if (uiState.isFirebaseConnected) "Connectée" else "Hors ligne",
                isActive = uiState.isFirebaseConnected
            )
            
            uiState.licenseInfo?.installDate?.let { installDate ->
                Spacer(modifier = Modifier.height(8.dp))
                val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
                val dateStr = formatter.format(java.util.Date(installDate))
                StatusRow(
                    label = "Date d'installation",
                    value = dateStr,
                    isActive = true
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur visuel
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) SuccessGreen else WarningOrange)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (isActive) SuccessGreen else WarningOrange
            )
        }
    }
}

@Composable
private fun TrialExpiredCard(
    onOpenActivationSite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WarningOrange.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = WarningOrange
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Période d'essai terminée",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = WarningOrange,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Pour continuer à utiliser SkyPlayer, veuillez activer votre application sur",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "skyplayerapp.xyz",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GradientElectricStart, GradientElectricEnd)
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onOpenActivationSite
            ) {
                Text("Ouvrir skyplayerapp.xyz")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Instructions:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "1. Copiez votre Identifiant Appareil ci-dessus\n" +
                               "2. Rendez-vous sur skyplayerapp.xyz\n" +
                               "3. Entrez votre ID et procédez au paiement\n" +
                               "4. L'activation est instantanée !",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.size(12.dp))
            
            Text(
                text = "SkyPlayer est un lecteur multimédia uniquement. " +
                       "Nous ne fournissons ni ne vendons aucune liste de chaînes ou contenu TV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: LicenseViewModel.LicenseUiState,
    onRefresh: () -> Unit,
    onHealthCheck: () -> Unit,
    onOpenActivationSite: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bouton principal
        if (uiState.hasValidAccess) {
            Button(
                onClick = { /* Navigation gérée par LaunchedEffect */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Continuer vers l'application")
            }
        } else {
            FilledTonalButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Vérifier l'activation")
            }
        }
        
        // Bouton Health Check (debug/diagnostic)
        OutlinedButton(
            onClick = onOpenActivationSite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activer sur skyplayerapp.xyz")
        }

        OutlinedButton(
            onClick = onHealthCheck,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vérifier la connexion serveur")
        }
        
        // Résultat du health check
        uiState.healthCheckResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.success) 
                        SuccessGreen.copy(alpha = 0.1f) 
                    else 
                        WarningOrange.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.success) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.success) SuccessGreen else WarningOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.size(12.dp))
                    
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.success) SuccessGreen else WarningOrange
                    )
                }
            }
        }
    }
}

private fun openActivationSite(context: android.content.Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ACTIVATION_SITE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure { error ->
        Timber.e(error, "Impossible d'ouvrir le site d'activation")
    }
}
