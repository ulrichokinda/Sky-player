package com.skyplayer.pro.ui.screens.home

import android.app.UiModeManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.ui.components.PinDialog
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import com.skyplayer.pro.ui.viewmodel.ParentalViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dashboard premium inspiré de Hot Player
 * Grille 3 colonnes, contrôle parental intégré
 */
@Composable
fun DashboardScreen(
    onNavigateToLive: () -> Unit,
    onNavigateToVOD: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRemoteConfig: () -> Unit,
    onNavigateToAddPlaylist: () -> Unit = {},
    onNavigateToScannerTV: () -> Unit = {},
    onNavigateToEditPlaylist: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Détection TV (Android TV / Firestick)
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    // Responsive: colonnes de grille adaptées à la taille d'écran
    val gridColumns = when {
        isTV                  -> 4  // TV: 4 colonnes
        screenWidthDp >= 840  -> 4  // Grande tablette paysage
        screenWidthDp >= 600  -> 3  // Tablette portrait
        else                  -> 2  // Smartphone: 2 colonnes fixes (zéro débordement)
    }
    val tileMinSize = when {
        isTV                 -> 180.dp
        screenWidthDp >= 600 -> 160.dp
        else                 -> 140.dp
    }

    // En-tête intelligent — VISIBLE sur tous les appareils (Mobile, Tablette, TV)
    val playlistName by dashboardViewModel.activePlaylistName.collectAsState()
    val expiryLabel by dashboardViewModel.expiryLabel.collectAsState()
    val expiryColor by dashboardViewModel.expiryColor.collectAsState()
    val deviceId by dashboardViewModel.deviceId.collectAsState()
    var headerVisible by remember { mutableStateOf(true) } // TOUJOURS visible initialement

    // Auto-masquage de l'en-tête après 6s (uniquement si l'utilisateur ne clique pas)
    LaunchedEffect(Unit) {
        delay(8000)
        headerVisible = false
    }

    // Horloge en temps réel
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // État réseau
    var networkStatus by remember { mutableStateOf("VÉRIFICATION...") }
    var networkColor by remember { mutableStateOf(Color.Gray) }

    // Parental control states
    var showPinDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var parentalUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

        while (true) {
            val now = Calendar.getInstance().time
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }

            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)

            if (activeNetwork == null || caps == null) {
                networkStatus = "HORS LIGNE"
                networkColor = Color.Red
            } else {
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        networkStatus = "SIGNAL WIFI"
                        networkColor = ElectricSkyBlue
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        networkStatus = "SIGNAL DATA"
                        networkColor = WarningOrange
                    }
                    else -> {
                        networkStatus = "SIGNAL CONNECTÉ"
                        networkColor = Color.Green
                    }
                }
            }

            delay(5000)
        }
    }

    // Observer les états du ViewModel
    val isChecking by dashboardViewModel.isChecking.collectAsState()
    val trialStatus by dashboardViewModel.trialStatus.collectAsState()
    val macPlaylistStatus by dashboardViewModel.macPlaylistStatus.collectAsState()
    val downloadProgress by dashboardViewModel.downloadProgress.collectAsState()
    val downloadComplete by dashboardViewModel.downloadComplete.collectAsState()
    val downloadError by dashboardViewModel.downloadError.collectAsState()
    val channelCount by dashboardViewModel.channelCount.collectAsState()
    
    // Navigation vers Live TV quand téléchargement terminé
    LaunchedEffect(downloadComplete) {
        if (downloadComplete) {
            onNavigateToLive()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ÉTAT 1: VÉRIFICATION EN COURS (afficher logo + chargement)
    // ═══════════════════════════════════════════════════════════════
    if (isChecking && trialStatus == DashboardViewModel.TrialStatus.Checking) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = ElectricSkyBlue,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Vérification de l'appareil...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        return
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ÉTAT 2: TRIAL EXPIRÉ (afficher MAC + lien activation)
    // ═══════════════════════════════════════════════════════════════
    if (trialStatus == DashboardViewModel.TrialStatus.Expired) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Icône avertissement
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = WarningOrange
                )
                
                Text(
                    text = "Période d'essai expirée",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Votre essai de 15 jours est terminé.\nPour continuer à utiliser Sky Player Pro, veuillez activer votre appareil.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Carte MAC en grand
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            cm.setPrimaryClip(
                                ClipData.newPlainText("Adresse MAC", deviceId)
                            )
                            Toast.makeText(
                                context,
                                "Adresse MAC copiée",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        WarningOrange.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VOTRE ADRESSE MAC",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WarningOrange,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "(Appuyez pour copier)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Text(
                    text = "Contactez votre fournisseur avec cette adresse MAC pour l'activation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ÉTAT 3: TÉLÉCHARGEMENT EN COURS (Scenario A: playlist trouvée)
    // ═══════════════════════════════════════════════════════════════
    if (macPlaylistStatus is DashboardViewModel.MacPlaylistStatus.Found && 
        !downloadComplete && 
        downloadError == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                // Icône téléchargement animée
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = ElectricSkyBlue,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = ElectricSkyBlue
                    )
                }
                
                val playlistInfo = (macPlaylistStatus as DashboardViewModel.MacPlaylistStatus.Found).info
                Text(
                    text = "Téléchargement de votre playlist",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = playlistInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PremiumGold,
                    textAlign = TextAlign.Center
                )
                
                // Barre de progression
                if (downloadProgress.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.readBytes.toFloat() / downloadProgress.totalBytes.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricSkyBlue,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricSkyBlue,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
                
                // Texte "Téléchargement : X.X Mo / Y.Y Mo"
                Text(
                    text = "Téléchargement : ${downloadProgress.label}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.horizontalGradient(
                            listOf(GradientElectricStart, GradientElectricEnd)
                        )
                    ),
                    textAlign = TextAlign.Center
                )
                
                // Bouton pour ignorer et aller au Dashboard
                TextButton(
                    onClick = { dashboardViewModel.skipToDashboard() }
                ) {
                    Text(
                        text = "Ignorer et aller au Dashboard",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        return
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ÉTAT 4: DASHBOARD NORMAL (Scenario B: aucune playlist)
    // ═══════════════════════════════════════════════════════════════
    
    val items = listOf(
        DashboardItem("LIVE TV", Icons.Default.LiveTv, ElectricSkyBlue, onNavigateToLive),
        DashboardItem("FILMS", Icons.Default.Movie, PremiumGold, onNavigateToVOD),
        DashboardItem("SÉRIES", Icons.Default.Tv, Color(0xFFE91E63), onNavigateToSeries),
        DashboardItem("FAVORIS", Icons.Default.Favorite, Color.Red, onNavigateToFavorites),
        DashboardItem(
            title = "PARENTAL",
            icon = Icons.Default.Lock,
            color = if (parentalUnlocked) SuccessGreen else WarningOrange,
            onClick = {
                if (parentalViewModel.manager.isPinSet()) {
                    showPinDialog = true
                } else {
                    showSetupPinDialog = true
                }
            }
        ),
        DashboardItem("RÉGLAGES", Icons.Default.Settings, Color.Gray, onNavigateToSettings),
        DashboardItem("QR SYNC", Icons.Default.QrCodeScanner, ElectricSkyBlue, onNavigateToRemoteConfig)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // En-tête intelligent: nom playlist + expiration
            AnimatedVisibility(
                visible = headerVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .background(
                            color = Color(0xFF111111),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { headerVisible = false }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = playlistName.ifBlank { "Sky Player Pro" },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        if (expiryLabel.isNotBlank()) {
                            Text(
                                text = expiryLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = expiryColor
                                )
                            )
                        }
                    }
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Header minimaliste avec Horloge et Statut Réseau
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Sky Player",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PRO VERSION",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = PremiumGold,
                                letterSpacing = 3.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = networkColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, networkColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SignalCellularAlt, null, tint = networkColor, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(networkStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = networkColor)
                            }
                        }
                    }
                }

                // Horloge Digitale Premium
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Grille responsive: colonnes fixes sur mobile (zéro débordement),
            // adaptive sur tablette/TV
            LazyVerticalGrid(
                columns = if (isTV || screenWidthDp >= 600)
                    GridCells.Adaptive(minSize = tileMinSize)
                else
                    GridCells.Fixed(gridColumns),  // 2 colonnes fixes sur smartphone
                horizontalArrangement = Arrangement.spacedBy(if (isTV) 16.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTV) 16.dp else 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    DashboardTile(item, isTV = isTV)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════════
            // SECTION BOUTONS D'ACTION CONFIGURABLES — TOUJOURS VISIBLE
            // Sur Mobile: boutons empilés verticalement
            // Sur Tablette/TV: boutons alignés horizontalement
            // ═══════════════════════════════════════════════════════════════
            val actionButtonHeight = if (isTV) 56.dp else 48.dp
            val actionFontSize = if (isTV) 16.sp else 14.sp
            val actionIconSize = if (isTV) 24.dp else 20.dp

            if (screenWidthDp >= 600 || isTV) {
                // Mode Tablette/TV: Boutons alignés horizontalement
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton 1: Ajouter une playlist
                    ActionButton(
                        text = "Ajouter une playlist",
                        icon = Icons.Default.Add,
                        onClick = onNavigateToAddPlaylist,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.weight(1f)
                    )

                    // Bouton 2: Scanner pour TV
                    ActionButton(
                        text = "Scanner pour TV",
                        icon = Icons.Default.QrCode2,
                        onClick = onNavigateToScannerTV,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.weight(1f)
                    )

                    // Bouton 3: Gérer la playlist
                    ActionButton(
                        text = "Gérer playlist",
                        icon = Icons.Default.Edit,
                        onClick = onNavigateToEditPlaylist,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Mode Mobile: Boutons empilés verticalement pour éviter débordement
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bouton 1: Ajouter une playlist
                    ActionButton(
                        text = "Ajouter une playlist",
                        icon = Icons.Default.Add,
                        onClick = onNavigateToAddPlaylist,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bouton 2: Scanner pour TV
                    ActionButton(
                        text = "Scanner pour TV",
                        icon = Icons.Default.QrCode2,
                        onClick = onNavigateToScannerTV,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bouton 3: Gérer la playlist
                    ActionButton(
                        text = "Gérer playlist",
                        icon = Icons.Default.Edit,
                        onClick = onNavigateToEditPlaylist,
                        height = actionButtonHeight,
                        fontSize = actionFontSize,
                        iconSize = actionIconSize,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Bloc MAC — Visible sur tous les appareils (Mobile, Tablette, TV) ──
            if (deviceId.isNotBlank()) {
                // Design unifié: carte MAC avec style premium sur tous les écrans
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            cm.setPrimaryClip(
                                ClipData.newPlainText("Adresse MAC", deviceId)
                            )
                            Toast.makeText(
                                context,
                                "Adresse MAC copiée",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    color = Color(0xFF0E0E0E),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        GradientElectricStart.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADRESSE MAC",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 2.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = deviceId,
                                style = if (isTV) {
                                    // TV: texte plus grand
                                    MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GradientElectricStart
                                    )
                                } else {
                                    // Mobile/Tablette: taille standard
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = GradientElectricStart
                                    )
                                },
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier MAC",
                            tint = GradientElectricStart.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (isTV) 24.dp else 20.dp)
                        )
                    }
                }
            }

            // Footer Info
            Text(
                text = "© Sky Player Pro • Optimisé pour l'Afrique",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }

    // PIN Verification Dialog (code already set)
    if (showPinDialog) {
        PinDialog(
            onConfirm = { pin ->
                if (parentalViewModel.manager.checkPin(pin)) {
                    parentalUnlocked = !parentalUnlocked
                    showPinDialog = false
                    pinError = null
                } else {
                    pinError = "Code PIN incorrect"
                }
            },
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            error = pinError
        )
    }

    // PIN Setup Dialog (first time)
    if (showSetupPinDialog) {
        SetupPinDialog(
            onPinCreated = { pin ->
                parentalViewModel.manager.setPin(pin)
                showSetupPinDialog = false
                parentalUnlocked = true
            },
            onDismiss = { showSetupPinDialog = false }
        )
    }
}

/**
 * Dialog pour créer un nouveau code PIN parental
 */
@Composable
private fun SetupPinDialog(
    onPinCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1=create, 2=confirm
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ElectricSkyBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (step == 1) "CRÉER UN CODE PIN" else "CONFIRMER LE CODE",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (step == 1) "Choisissez un code PIN à 4 chiffres"
                    else "Confirmez votre code PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = {
                        if (it.length <= 4) {
                            if (step == 1) pin = it else confirmPin = it
                            error = null
                        }
                    },
                    modifier = Modifier.width(160.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = 8.sp,
                        color = ElectricSkyBlue
                    ),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = PureBlack,
                        unfocusedContainerColor = PureBlack,
                        focusedIndicatorColor = ElectricSkyBlue
                    )
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length == 4) {
                            step = 2
                        } else {
                            error = "Le code doit contenir 4 chiffres"
                        }
                    } else {
                        if (confirmPin == pin) {
                            onPinCreated(pin)
                        } else {
                            error = "Les codes ne correspondent pas"
                            confirmPin = ""
                        }
                    }
                },
                enabled = if (step == 1) pin.length == 4 else confirmPin.length == 4,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ElectricSkyBlue
                )
            ) {
                Text(
                    text = if (step == 1) "SUIVANT" else "CRÉER",
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("ANNULER", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
private fun DashboardTile(item: DashboardItem, isTV: Boolean = false) {
    val iconSize = if (isTV) 56.dp else 48.dp
    val glowSize = if (isTV) 64.dp else 50.dp
    val cornerRadius = if (isTV) 20.dp else 24.dp
    val ratio = if (isTV) 1.1f else 1.3f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { item.onClick() },
        color = CardBlack,
        border = androidx.compose.foundation.BorderStroke(1.dp, item.color.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Gradient Overlay subtil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                item.color.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glow icône
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(glowSize)
                            .background(item.color.copy(alpha = 0.1f), CircleShape)
                            .blur(20.dp)
                    )
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.color,
                        modifier = Modifier.size(iconSize)
                    )
                }

                Spacer(modifier = Modifier.height(if (isTV) 12.dp else 8.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        fontSize = if (isTV) 16.sp else 12.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * Bouton d'action configurable pour le Dashboard
 * Affiche texte complet sans troncature avec icône
 * Adapté pour Mobile, Tablette et TV
 */
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            ElectricSkyBlue.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = ElectricSkyBlue
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible // Pas de troncature
            )
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)
