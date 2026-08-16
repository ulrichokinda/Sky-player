package com.skyplayer.pro.ui.screens.epg

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.EpgProgram
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.LiveTvColor
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack

import com.skyplayer.pro.ui.viewmodel.EpgGuideEntry
import com.skyplayer.pro.ui.viewmodel.EpgGuideViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guide Électronique des Programmes (EPG)
 * Affiche les programmes en cours et à venir pour toutes les chaînes live
 */
@Composable
fun EpgGuideScreen(
    onBackClick: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    viewModel: EpgGuideViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRefreshTime.collectAsStateWithLifecycle()

    // Auto-refresh every 60 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            viewModel.refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // ——— Header ———
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Guide des Programmes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Text(
                    text = formatRefreshTime(lastRefreshTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = viewModel::refresh,
                modifier = Modifier.background(ElevatedBlack, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualiser",
                    tint = PremiumEmerald
                )
            }
        }

        // ——— Filtres catégories ———
        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryFilterPill(
                        label = "Toutes",
                        isSelected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(categories) { cat ->
                    CategoryFilterPill(
                        label = cat,
                        isSelected = cat == selectedCategory,
                        onClick = { viewModel.selectCategory(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // ——— Contenu ———
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PremiumEmerald)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chargement du guide…",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            entries.isEmpty() -> {
                EpgEmptyState()
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.channel.id }) { entry ->
                        EpgChannelRow(
                            entry = entry,
                            onClick = { onChannelClick(entry.channel) }
                        )
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.04f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    entry: EpgGuideEntry,
    onClick: () -> Unit
) {
    val channel = entry.channel
    val currentProgram = entry.currentProgram
    val nextPrograms = entry.nextPrograms

    val progressAnimation by animateFloatAsState(
        targetValue = currentProgram?.getProgress() ?: 0f,
        animationSpec = tween(durationMillis = 800),
        label = "epg_progress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ——— Logo chaîne ———
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ElevatedBlack),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = LiveTvColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ——— Info programme ———
        Column(modifier = Modifier.weight(1f)) {
            // Nom chaîne
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (currentProgram != null) {
                // Titre programme en cours
                Text(
                    text = currentProgram.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PremiumEmerald
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Barre de progression + horaires
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(currentProgram.start),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { progressAnimation },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = PremiumEmerald,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTime(currentProgram.stop),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Prochains programmes
                if (nextPrograms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(nextPrograms.take(2)) { next ->
                            NextProgramPill(program = next)
                        }
                    }
                }
            } else {
                // Aucune info EPG disponible
                Text(
                    text = "Aucune info programme",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ——— Indicateur EN DIRECT ———
        if (currentProgram != null) {
            Box(
                modifier = Modifier
                    .background(LiveTvColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(LiveTvColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = LiveTvColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NextProgramPill(program: EpgProgram) {
    Box(
        modifier = Modifier
            .background(ElevatedBlack, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = PremiumGold.copy(alpha = 0.7f),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${formatTime(program.start)} · ${program.title}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) PremiumEmerald.copy(alpha = 0.15f) else ElevatedBlack,
                RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PremiumEmerald else Color.White.copy(alpha = 0.55f)
            )
        )
    }
}

@Composable
private fun EpgEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun programme disponible",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ajoutez une playlist avec source EPG\npour voir le guide des programmes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

private fun formatRefreshTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "Mis à jour à ${sdf.format(Date(timestampMs))}"
}
