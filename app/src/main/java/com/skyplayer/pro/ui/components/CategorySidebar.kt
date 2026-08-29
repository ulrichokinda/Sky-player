package com.skyplayer.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.security.ParentalControlManager
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Barre de navigation latérale pour catégories (Style Hot Player)
 *
 * Design « verre fusionné » : fond translucide laissant transparaître le halo
 * de la section (Live/VOD/Séries), séparateur doux à droite — plus de panneau
 * noir plein ni de bordure dure qui coupe l'écran.
 */
@Composable
fun CategorySidebar(
    categories: List<ChannelCategory>,
    selectedCategory: String?,
    onCategorySelected: (ChannelCategory) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PremiumEmerald,
    sectionLabel: String = "CATÉGORIES",
    onSearchQueryChange: (String) -> Unit = {},
    parentalManager: ParentalControlManager = hiltViewModel<com.skyplayer.pro.ui.viewmodel.ParentalViewModel>().manager
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    // Catégories filtrées par la recherche (partagées scroll + liste)
    val filteredCategories = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    // Fusion avec le changement de section : le sidebar défile en douceur
    // vers la catégorie active pour que la sélection reste toujours visible
    LaunchedEffect(selectedCategory, filteredCategories) {
        val targetIndex = when {
            selectedCategory == null || selectedCategory == "ALL" -> 0
            else -> filteredCategories.indexOfFirst { it.name == selectedCategory }
        }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {
        // Halo fusionné avec le fond de la section (émeraude/cyan/violet)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.12f),
                            PureBlack.copy(alpha = 0.35f),
                            PureBlack.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // Séparateur doux à droite (au lieu d'une bordure dure)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            // En-tête de section — fusionne le sidebar avec la section courante
            Text(
                text = sectionLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(
                        listOf(accentColor, Color.White.copy(alpha = 0.9f))
                    ),
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recherche intégrée en haut de la sidebar (style verre)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = PureBlack.copy(alpha = 0.4f),
                    unfocusedContainerColor = PureBlack.copy(alpha = 0.25f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Liste des catégories
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Catégorie "TOUT" en premier
                item {
                    CategoryItem(
                        name = "TOUT",
                        count = categories.sumOf { it.channelCount },
                        isSelected = selectedCategory == null || selectedCategory == "ALL",
                        isLocked = false,
                        accentColor = accentColor,
                        onClick = { onCategorySelected(ChannelCategory("ALL", emptyList())) }
                    )
                }

                items(
                    items = filteredCategories,
                    key = { it.name }
                ) { category ->
                    CategoryItem(
                        name = category.name,
                        count = category.channelCount,
                        isSelected = category.name == selectedCategory,
                        isLocked = parentalManager.isSensitiveCategory(category.name),
                        accentColor = accentColor,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }
    }
}

/**
 * Synchronise la catégorie visible avec le scroll de la liste de contenu.
 *
 * Le premier élément visible détermine la catégorie affichée/surlignée dans le
 * sidebar : quand l'utilisateur fait défiler la liste (doigt OU télécommande),
 * le sidebar suit automatiquement. Retourne un MutableState pour pouvoir
 * réinitialiser la valeur au clic sur une catégorie.
 *
 * Accepte n'importe quelle source d'index (LazyListState, LazyGridState, …)
 * via [firstVisibleIndexProvider].
 */
@Composable
fun rememberVisibleIndexCategory(
    firstVisibleIndexProvider: () -> Int,
    itemCategory: (Int) -> String?
): MutableState<String?> {
    val category = remember { mutableStateOf<String?>(null) }
    val currentItemCategory by rememberUpdatedState(itemCategory)
    val currentIndexProvider by rememberUpdatedState(firstVisibleIndexProvider)
    LaunchedEffect(Unit) {
        snapshotFlow { currentIndexProvider() }
            .distinctUntilChanged()
            .collect { index -> category.value = currentItemCategory(index) }
    }
    return category
}

/**
 * Version amelioree : observe le scroll d'une LazyColumn et retourne
 * la categorie du premier element visible. Plus fiable que la version
 * precedente car elle observe directement le LazyListState.
 */
@Composable
fun rememberScrollSyncedCategory(
    listState: androidx.compose.foundation.lazy.LazyListState,
    categories: List<com.skyplayer.pro.data.organizer.ChannelCategory>,
    channels: List<com.skyplayer.pro.data.model.Channel>,
    selectedCategory: String?
): MutableState<String?> {
    val category = remember { mutableStateOf<String?>(null) }
    val currentChannels by rememberUpdatedState(channels)

    // Le LaunchedEffect re-lance quand categories ou channels changent (ajout/suppression
    // de playlist, refresh). Sans categories comme key, le snapshotFlow ne redémarre pas
    // quand la liste change mais firstVisibleItemIndex reste identique.
    LaunchedEffect(listState, categories, currentChannels) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val channel = currentChannels.getOrNull(index)
                category.value = channel?.category
            }
    }
    return category
}

/**
 * Version pour LazyVerticalGrid : observe le scroll d une grille et retourne
 * la categorie du premier element visible. Se re-evalue quand les donnees changent.
 */
@Composable
fun rememberGridScrollSyncedCategory(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    categories: List<com.skyplayer.pro.data.organizer.ChannelCategory>,
    entries: List<GridSectionEntry>,
    selectedCategory: String?
): MutableState<String?> {
    val category = remember { mutableStateOf<String?>(null) }
    val currentEntries by rememberUpdatedState(entries)

    LaunchedEffect(gridState, categories, currentEntries) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val entry = currentEntries.getOrNull(index)
                category.value = entry?.category
            }
    }
    return category
}


@Composable
private fun CategoryItem(
    name: String,
    count: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.06f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isLocked && !isSelected) Color.White.copy(alpha = 0.4f)
                        else if (isSelected) accentColor
                        else Color.White.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isLocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Verrouillé",
                        tint = if (isSelected) accentColor else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) accentColor else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Indicateur de sélection lumineux à gauche
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-12).dp)
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
                    .drawBehind {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.35f),
                            radius = size.height * 1.2f,
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        )
                    }
            )
        }
    }
}

/**
 * En-tête de section affiché au début de chaque catégorie (mode TOUT).
 * Utilisé par les listes (Live) et les grilles (VOD/Séries) pour marquer
 * visuellement le début de chaque section pendant le scroll.
 */
@Composable
fun CategorySectionHeader(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(accentColor.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = accentColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
    }
}

/**
 * Élément d'une grille en mode TOUT : soit un en-tête de catégorie, soit une carte.
 *
 * [key] doit être unique dans la grille (utilisé par LazyVerticalGrid).
 */
sealed interface GridSectionEntry {
    val category: String
    val key: String

    data class Header(override val category: String) : GridSectionEntry {
        override val key: String get() = "section_header_$category"
    }

    data class Content(val channel: Channel, override val category: String) : GridSectionEntry {
        override val key: String get() = channel.id
    }
}

/**
 * Convertit une liste triée par catégorie en entrées de grille avec un en-tête
 * au début de chaque catégorie (mode TOUT).
 */
fun List<Channel>.withSectionHeaders(): List<GridSectionEntry> {
    val entries = ArrayList<GridSectionEntry>(size)
    var previousCategory: String? = null
    for (channel in this) {
        if (channel.category != previousCategory) {
            entries.add(GridSectionEntry.Header(channel.category))
            previousCategory = channel.category
        }
        entries.add(GridSectionEntry.Content(channel, channel.category))
    }
    return entries
}

/**
 * Version compacte pour mobile/petits écrans
 */
@Composable
fun CategoryChips(
    categories: List<ChannelCategory>,
    selectedCategory: String?,
    onCategorySelected: (ChannelCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = categories.indexOfFirst { it.name == selectedCategory }

    LaunchedEffect(selectedCategory) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(
            items = categories,
            key = { it.name }
        ) { category ->
            CategoryChip(
                category = category,
                isSelected = category.name == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: ChannelCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        category.isRegionalPriority -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        category.isRegionalPriority -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category.isRegionalPriority) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${category.channelCount}",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
