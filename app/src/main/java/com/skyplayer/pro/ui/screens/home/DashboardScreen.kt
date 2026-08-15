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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.ui.components.PinDialog
import com.skyplayer.pro.ui.components.AdvancedMenuDialog
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.skyplayer.pro.R

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
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAddPlaylist: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER")
    onNavigateToScannerTV: () -> Unit = {},
    onNavigateToEditPlaylist: () -> Unit = {},
    onNavigateToParentalSetup: () -> Unit = {},
    onPlayChannel: (Channel) -> Unit = {},
    onPlayContent: (Channel) -> Unit = {},
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
    val checkingStatus = stringResource(R.string.dash_checking)
    val offlineStatus = stringResource(R.string.dash_offline)
    val wifiStatus = stringResource(R.string.dash_signal_wifi)
    val dataStatus = stringResource(R.string.dash_signal_data)
    val connectedStatus = stringResource(R.string.dash_signal_connected)

    var networkStatus by remember { mutableStateOf(checkingStatus) }
    var networkColor by remember { mutableStateOf(Color.Gray) }

    // Parental control states
    var showPinDialog by remember { mutableStateOf(false) }
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
                networkStatus = offlineStatus
                networkColor = Color.Red
            } else {
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        networkStatus = wifiStatus
                        networkColor = ElectricSkyBlue
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        networkStatus = dataStatus
                        networkColor = WarningOrange
                    }
                    else -> {
                        networkStatus = connectedStatus
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
    val recentlyWatched by dashboardViewModel.recentlyWatched.collectAsState()
    val isSyncing by dashboardViewModel.isSyncing.collectAsState()
    val syncProgress by dashboardViewModel.syncProgress.collectAsState()

    // Navigation vers Live TV quand téléchargement terminé
    LaunchedEffect(downloadComplete) {
        if (downloadComplete) {
            onNavigateToLive()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = PureBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                return@Scaffold
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
                            text = stringResource(R.string.dash_trial_expired_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.dash_trial_expired_msg),
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
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.dash_mac_copied)
                                        )
                                    }
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
                                    text = stringResource(R.string.dash_your_mac),
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
                return@Scaffold
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
            return@Scaffold
        }

        // ═══════════════════════════════════════════════════════════════
        // ÉTAT 4: DASHBOARD NORMAL (Scenario B: aucune playlist)
        // ═══════════════════════════════════════════════════════════════

        // Dashboard simplifié à 5 tuiles essentielles + bouton "Plus"
        val mainItems = listOf(
            DashboardItem("LIVE TV", Icons.Default.LiveTv, ElectricSkyBlue, onNavigateToLive),
            DashboardItem("FILMS", Icons.Default.Movie, PremiumGold, onNavigateToVOD),
            DashboardItem("SÉRIES", Icons.Default.Tv, Color(0xFFE91E63), onNavigateToSeries),
            DashboardItem("FAVORIS", Icons.Default.Favorite, Color.Red, onNavigateToFavorites),
            DashboardItem("RECHERCHE", Icons.Default.Search, ElectricSkyBlue.copy(alpha = 0.8f), onNavigateToSearch)
        )

        // Options avancées accessibles via bouton "Plus"
        var showAdvancedMenu by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Indicateur permanent compact
                CompactStatusIndicator(
                    isSyncing = isSyncing,
                    syncProgress = syncProgress,
                    channelCount = channelCount,
                    expiryLabel = expiryLabel,
                    expiryColor = expiryColor,
                    isOffline = networkStatus == offlineStatus,
                    isNetworkUnstable = networkColor == WarningOrange
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (recentlyWatched.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dash_continue_watching),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyWatched, key = { it.id }) { channel ->
                            ContinueWatchingCard(
                                channel = channel,
                                onClick = {
                                    when (channel.type) {
                                        ContentType.VOD_MOVIE, ContentType.VOD_SERIES -> onPlayContent(channel)
                                        else -> onPlayChannel(channel)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

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
                    items(mainItems) { item ->
                        DashboardTile(item, isTV = isTV)
                    }

                    // Tuile "Plus" pour options avancées
                    item {
                        DashboardTile(
                            DashboardItem(
                                title = "PLUS",
                                icon = Icons.Default.Settings,
                                color = Color.Gray,
                                onClick = { showAdvancedMenu = true }
                            ),
                            isTV = isTV
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
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.dash_mac_copied)
                                    )
                                }
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
                                text = stringResource(R.string.dash_mac_label),
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

    // Menu avancé (Options supplémentaires)
    if (showAdvancedMenu) {
        AdvancedMenuDialog(
            onDismiss = { showAdvancedMenu = false },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToRemoteConfig = onNavigateToRemoteConfig,
            onNavigateToAddPlaylist = onNavigateToAddPlaylist,
            onNavigateToEditPlaylist = onNavigateToEditPlaylist,
            onNavigateToParental = {
                showAdvancedMenu = false
                if (parentalViewModel.manager.isPinSet()) {
                    showPinDialog = true
                } else {
                    onNavigateToParentalSetup()
                }
            }
        )
    }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    channel: Channel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBlack),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ElectricSkyBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
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

/**
 * Indicateur permanent compact pour le Dashboard
 */
@Composable
private fun CompactStatusIndicator(
    isSyncing: Boolean,
    syncProgress: Float,
    channelCount: Int,
    expiryLabel: String,
    expiryColor: Color,
    isOffline: Boolean,
    isNetworkUnstable: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = Color(0xFF0F0F0F),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isOffline -> Color.Red.copy(alpha = 0.3f)
                isSyncing -> ElectricSkyBlue.copy(alpha = 0.3f)
                isNetworkUnstable -> WarningOrange.copy(alpha = 0.3f)
                else -> Color.White.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Status
                when {
                    isOffline -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Hors ligne — contenu local uniquement",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Red
                            )
                        }
                    }
                    isSyncing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = ElectricSkyBlue
                            )
                            Text(
                                text = "Mise à jour… ${(syncProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricSkyBlue
                            )
                        }
                    }
                    isNetworkUnstable -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Connexion instable",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarningOrange
                            )
                        }
                    }
                    else -> {
                        // Playlist OK
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = SuccessGreen
                            )
                            Text(
                                text = "$channelCount chaînes",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Right: Expiry/License
                Text(
                    text = expiryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = expiryColor
                )
            }

            // Progress bar if syncing
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { syncProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ElectricSkyBlue,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)
