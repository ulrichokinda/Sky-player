package com.skyplayer.pro.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.BuildConfig
import com.skyplayer.pro.data.model.VideoQuality
import com.skyplayer.pro.ui.components.PinDialog
import com.skyplayer.pro.ui.components.QualitySelector
import com.skyplayer.pro.ui.components.QuickStreamingModes
import com.skyplayer.pro.ui.components.StreamingAdvancedSettings
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.GradientStart
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import com.skyplayer.pro.ui.viewmodel.ParentalViewModel
import com.skyplayer.pro.ui.viewmodel.SettingsViewModel
import com.skyplayer.pro.ui.viewmodel.StreamingPreferencesViewModel

/**
 * Écran Paramètres
 * Configuration de l'application et gestion des données
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToRemoteConfig: () -> Unit = {},
    onNavigateToMyLine: () -> Unit = {},
    onNavigateToParentalSetup: () -> Unit = {},
    viewModel: LicenseViewModel = hiltViewModel(),
    streamingViewModel: StreamingPreferencesViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streamingPrefs by streamingViewModel.preferences.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    var showVerifyPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        settingsViewModel.events.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paramètres",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section: Apparence
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Apparence")
                SettingsCard {
                    Column {
                        // Mode Thème
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Thème",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = when (settingsUiState.themeMode) {
                                        "system" -> "Automatique (système)"
                                        "light" -> "Clair"
                                        "dark" -> "Sombre"
                                        "amoled" -> "AMOLED (noir absolu)"
                                        else -> "Sombre"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Sélecteur de thème
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOption(
                                label = "Auto",
                                selected = settingsUiState.themeMode == "system",
                                onClick = { settingsViewModel.setThemeMode("system") }
                            )
                            ThemeOption(
                                label = "Clair",
                                selected = settingsUiState.themeMode == "light",
                                onClick = { settingsViewModel.setThemeMode("light") }
                            )
                            ThemeOption(
                                label = "Sombre",
                                selected = settingsUiState.themeMode == "dark",
                                onClick = { settingsViewModel.setThemeMode("dark") }
                            )
                            ThemeOption(
                                label = "AMOLED",
                                selected = settingsUiState.themeMode == "amoled",
                                onClick = { settingsViewModel.setThemeMode("amoled") }
                            )
                        }
                    }
                }
            }

            // Section: Sécurité
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Sécurité & Famille")
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Code Parental (PIN)",
                        subtitle = if (parentalViewModel.manager.isPinSet()) "Modifier votre code de protection" else "Définir un code pour verrouiller des catégories",
                        onClick = {
                            if (parentalViewModel.manager.isPinSet()) {
                                showVerifyPinDialog = true
                            } else {
                                onNavigateToParentalSetup()
                            }
                        }
                    )
                }
            }

            // Section: Qualité Vidéo (Nouvelle section)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Qualité Vidéo & Streaming")

                // Modes rapides
                QuickStreamingModes(
                    onDataSaverClick = { streamingViewModel.enableDataSaverMode() },
                    onBalancedClick = {
                        streamingViewModel.setPreferredQuality(VideoQuality.MEDIUM)
                        streamingViewModel.setAutoAdjustQuality(true)
                        streamingViewModel.setBufferDuration(30)
                    },
                    onPerformanceClick = { streamingViewModel.enablePerformanceMode() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sélecteur de qualité
                QualitySelector(
                    currentQuality = streamingPrefs.preferredQuality,
                    onQualitySelected = { streamingViewModel.setPreferredQuality(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options avancées
                StreamingAdvancedSettings(
                    autoAdjustQuality = streamingPrefs.autoAdjustQuality,
                    onAutoAdjustChanged = { streamingViewModel.setAutoAdjustQuality(it) },
                    bufferDuration = streamingPrefs.bufferDurationSeconds,
                    onBufferDurationChanged = { streamingViewModel.setBufferDuration(it) },
                    lowLatencyMode = streamingPrefs.lowLatencyMode,
                    onLowLatencyChanged = { streamingViewModel.setLowLatencyMode(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Section: Performance
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Performance & Réseau")
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Memory,
                            title = "Tampon mémoire",
                            subtitle = "${streamingPrefs.bufferDurationSeconds}s - ${if (streamingPrefs.bufferDurationSeconds >= 60) "Optimisé réseau lent" else "Optimisé réseau rapide"}",
                            onClick = { /* Géré dans Qualité Vidéo */ }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(
                            icon = Icons.Default.NetworkCheck,
                            title = "Reconnexion auto",
                            checked = settingsUiState.autoReconnect,
                            onCheckedChange = { settingsViewModel.setAutoReconnect(it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Default.Refresh,
                            title = "Mettre à jour l'EPG",
                            subtitle = if (settingsUiState.isRefreshingEpg) "Mise à jour en cours..." else "Actualiser le guide des programmes de la playlist active",
                            onClick = { settingsViewModel.refreshActivePlaylistEpg() }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(
                            icon = Icons.Default.Update,
                            title = "Mises à jour auto",
                            checked = settingsUiState.autoUpdate,
                            onCheckedChange = { settingsViewModel.setAutoUpdate(it) }
                        )
                    }
                }
            }

            // Section: Données
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Gestion des données")
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            title = "Mes playlists",
                            subtitle = "Adresse MAC, playlist active et statut abonnement",
                            onClick = { onNavigateToMyLine() }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Default.Delete,
                            title = if (settingsUiState.isClearingCache) "Vidage du cache..." else "Vider le cache",
                            subtitle = "Libérer l'espace de stockage temporaire",
                            onClick = { settingsViewModel.clearCache() },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Section: Configuration TV (QR Code)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Configuration TV")
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Configurer par QR Code",
                            subtitle = "Scannez avec votre smartphone pour ajouter une playlist",
                            onClick = onNavigateToRemoteConfig,
                            tint = GradientElectricStart
                        )
                    }
                }
            }

            // Section: Licence & Activation
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Licence & Activation")
                SettingsCard {
                    Column {
                        // ID Appareil
                        val deviceId = uiState.deviceId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(deviceId))
                                })
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = GradientElectricStart
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ID Appareil",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = GradientElectricStart
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Statut licence
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isActivated) Icons.Default.Info else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (uiState.isActivated) SuccessGreen else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Statut",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = when {
                                        uiState.isActivated -> "✅ Activée"
                                        uiState.licenseInfo?.isTrialValid == true ->
                                            "🎁 Essai - ${uiState.licenseInfo?.trialDaysRemaining} jours restants"
                                        else -> "⏳ En attente d'activation"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isActivated) SuccessGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Section: Mention légale
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Mention Légale")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SkyPlayer est un lecteur multimédia uniquement. " +
                                   "Nous ne fournissons ni ne vendons aucune liste de chaînes ou contenu TV.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section: À propos
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("À propos")
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "Sky Player Pro v${BuildConfig.VERSION_NAME}",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Sky Player Pro v${BuildConfig.VERSION_NAME}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "© 2026 Sky Player Pro\nOptimisé pour l'Afrique",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showVerifyPinDialog) {
        PinDialog(
            onConfirm = { pin ->
                if (parentalViewModel.manager.checkPin(pin)) {
                    showVerifyPinDialog = false
                    pinError = null
                    onNavigateToParentalSetup()
                } else {
                    pinError = "Code PIN incorrect"
                }
            },
            onDismiss = {
                showVerifyPinDialog = false
                pinError = null
            },
            error = pinError
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = GradientStart,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = tint ?: MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RowScope.ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
