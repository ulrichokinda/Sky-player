package com.skyplayer.pro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Composant de navigation par onglets horizontaux pour mobile
 * Remplace la sidebar sur les petits écrans pour gagner de l'espace
 */
@Composable
fun HorizontalCategoryTabs(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (category == selectedCategory) 
                                FontWeight.SemiBold 
                            else 
                                FontWeight.Normal
                        ),
                        color = if (category == selectedCategory)
                            PureBlack
                        else
                            Color.White.copy(alpha = 0.7f)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    labelColor = Color.White.copy(alpha = 0.7f),
                    selectedContainerColor = ElectricSkyBlue,
                    selectedLabelColor = PureBlack
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = category == selectedCategory,
                    borderColor = if (category == selectedCategory)
                        ElectricSkyBlue
                    else
                        Color.White.copy(alpha = 0.2f),
                    borderWidth = if (category == selectedCategory) 2.dp else 1.dp
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

/**
 * Version compacte avec icônes pour les catégories principales
 */
@Composable
fun CompactCategoryTabs(
    categories: List<CategoryTab>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories) { category ->
            CategoryTabChip(
                category = category,
                isSelected = category.name == selectedCategory,
                onClick = { onCategorySelected(category.name) }
            )
        }
    }
}

@Composable
private fun CategoryTabChip(
    category: CategoryTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) PureBlack else Color.White.copy(alpha = 0.7f)
            )
        },
        leadingIcon = if (category.icon != null) {
            {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (isSelected) PureBlack else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            labelColor = Color.White.copy(alpha = 0.7f),
            selectedContainerColor = category.color ?: ElectricSkyBlue,
            selectedLabelColor = PureBlack
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected)
                (category.color ?: ElectricSkyBlue)
            else
                Color.White.copy(alpha = 0.2f),
            borderWidth = if (isSelected) 2.dp else 1.dp
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(40.dp)
    )
}

data class CategoryTab(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val color: Color? = null
)
