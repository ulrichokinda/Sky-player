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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.organizer.ChannelCategory
import com.skyplayer.pro.data.security.ParentalControlManager
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Barre de navigation latérale pour catégories (Style Hot Player)
 */
@Composable
fun CategorySidebar(
    categories: List<ChannelCategory>,
    selectedCategory: String?,
    onCategorySelected: (ChannelCategory) -> Unit,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    parentalManager: ParentalControlManager = hiltViewModel<com.skyplayer.pro.ui.viewmodel.ParentalViewModel>().manager
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp),
        color = PureBlack,
        border = androidx.compose.foundation.BorderStroke(1.dp, color = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            // Recherche intégrée en haut de la sidebar
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
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PremiumEmerald) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PremiumEmerald,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
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
                        onClick = { onCategorySelected(ChannelCategory("ALL", emptyList())) }
                    )
                }

                val filteredCategories = categories.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
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
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    count: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PremiumEmerald.copy(alpha = 0.15f) else Color.Transparent)
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
                        color = if (isLocked && !isSelected) Color.White.copy(alpha = 0.4f) else if (isSelected) PremiumEmerald else Color.White.copy(alpha = 0.7f),
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
                        tint = if (isSelected) PremiumEmerald else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) PremiumEmerald else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        // Indicateur de sélection à gauche
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-16).dp)
                    .width(4.dp)
                    .height(20.dp)
                    .background(PremiumEmerald, RoundedCornerShape(2.dp))
            )
        }
    }
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
