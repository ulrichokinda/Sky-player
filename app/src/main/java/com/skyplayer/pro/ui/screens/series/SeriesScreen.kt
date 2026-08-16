package com.skyplayer.pro.ui.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.CustomFolder
import com.skyplayer.pro.data.model.itemCount
import com.skyplayer.pro.ui.components.CategorySidebar
import com.skyplayer.pro.ui.components.ChannelListShimmer
import com.skyplayer.pro.ui.components.SectionTopBar
import com.skyplayer.pro.ui.components.HorizontalCategoryTabs
import com.skyplayer.pro.ui.components.TrustAction
import com.skyplayer.pro.ui.components.ImmersiveSeriesCard
import com.skyplayer.pro.ui.components.TrustStateView
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.res.stringResource
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.components.ShimmerItem
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.GlassWhite
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SeriesColor
import com.skyplayer.pro.ui.components.PinDialog
import com.skyplayer.pro.ui.viewmodel.ParentalViewModel
import androidx.activity.compose.BackHandler

/**
 * Écran Séries (Style Hot Player)
 * Sidebar à gauche, Grille de séries à droite
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    onSeriesClick: (Channel) -> Unit,
    onBackToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAddPlaylist: () -> Unit = {},
    viewModel: SeriesViewModel = hiltViewModel(),
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    BackHandler { onBackToHome() }
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    
    val series by viewModel.series.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // États pour le contrôle parental
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingCategoryName by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SeriesColor.copy(alpha = 0.07f),
                        PureBlack,
                        PureBlack
                    )
                )
            )
    ) {
        // Navigation conditionnelle: Sidebar pour TV, Onglets horizontaux pour Mobile
        if (isTV) {
            // Sidebar Latérale pour TV
            CategorySidebar(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    if (parentalViewModel.manager.isSensitiveCategory(category.name)) {
                        pendingCategoryName = category.name
                        showPinDialog = true
                    } else {
                        viewModel.selectCategory(category.name)
                    }
                },
                accentColor = SeriesColor,
                sectionLabel = "SÉRIES",
                onSearchQueryChange = { viewModel.searchSeries(it) }
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Onglets horizontaux pour mobile uniquement
            if (!isTV) {
                HorizontalCategoryTabs(
                    categories = categories.map { it.name },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        val categoryObj = categories.find { it.name == category }
                        categoryObj?.let {
                            if (parentalViewModel.manager.isSensitiveCategory(it.name)) {
                                pendingCategoryName = it.name
                                showPinDialog = true
                            } else {
                                viewModel.selectCategory(it.name)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            SectionTopBar(
                title = selectedCategory?.uppercase()
                    ?: stringResource(R.string.section_series),
                subtitle = stringResource(R.string.section_items_count, series.size),
                accentColor = SeriesColor,
                onNavigateHome = onBackToHome,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    ChannelListShimmer()
                } else if (series.isEmpty()) {
                    TrustStateView(
                        icon = Icons.Default.Tv,
                        title = stringResource(R.string.trust_empty_series_title),
                        message = stringResource(R.string.trust_empty_series_message),
                        iconTint = SeriesColor.copy(alpha = 0.5f),
                        primaryAction = TrustAction(
                            label = stringResource(R.string.trust_action_add_playlist),
                            onClick = onNavigateToAddPlaylist
                        )
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(series, key = { it.id }) { item ->
                            ImmersiveSeriesCard(
                                series = item,
                                onClick = { onSeriesClick(item) },
                                accentColor = SeriesColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            onConfirm = { pin ->
                if (parentalViewModel.manager.checkPin(pin)) {
                    pendingCategoryName?.let { viewModel.selectCategory(it) }
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
}

@Composable
private fun SeriesTile(
    series: Channel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = CardBlack
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = series.logoUrl,
                contentDescription = series.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { ShimmerItem(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize().background(ElevatedBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = series.name.take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            )
            
            // Overlay dégradé immersif
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            
            // Titre en bas
            Text(
                text = series.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Grille des dossiers personnalisés pour Séries
 */
@Composable
private fun FoldersGridSeries(
    folders: List<CustomFolder>,
    onFolderClick: (CustomFolder) -> Unit,
    onCreateFolder: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Bouton créer nouveau dossier
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(onClick = onCreateFolder),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GlassWhite
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = PremiumEmerald.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Créer",
                        modifier = Modifier.size(48.dp),
                        tint = PremiumEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nouveau dossier",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Dossiers existants
        items(folders, key = { it.id }) { folder ->
            FolderCardSeries(
                folder = folder,
                onClick = { onFolderClick(folder) }
            )
        }
    }
}

/**
 * Carte de dossier personnalisé pour Séries
 */
@Composable
private fun FolderCardSeries(
    folder: CustomFolder,
    onClick: () -> Unit
) {
    val color = folder.colorHex?.let { 
        try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it))
        } catch (_: Exception) { PremiumEmerald }
    } ?: PremiumEmerald
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icône emoji
            Text(
                text = folder.icon ?: "📁",
                style = MaterialTheme.typography.displaySmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Nom du dossier
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Nombre d'éléments
            Text(
                text = "${folder.itemCount()} séries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
