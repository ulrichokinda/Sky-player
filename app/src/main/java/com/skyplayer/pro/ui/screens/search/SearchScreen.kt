package com.skyplayer.pro.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.skyplayer.pro.data.model.Channel
import com.skyplayer.pro.data.model.ContentType
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.LiveTvColor
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SeriesColor
import com.skyplayer.pro.ui.theme.VodColor
import com.skyplayer.pro.ui.viewmodel.SearchFilter
import com.skyplayer.pro.ui.viewmodel.SearchViewModel

/**
 * Écran de recherche globale temps réel
 * Recherche sur Live TV, VOD, Séries avec FTS4
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onContentClick: (Channel) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isShowingRecents by viewModel.isShowingRecents.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    // Launcher pour la recherche vocale
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let { viewModel.onQueryChange(it) }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // ——— Barre de recherche ———
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "Rechercher chaînes, films, séries…",
                        color = Color.White.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PremiumEmerald
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites le nom d'une chaîne ou d'un film")
                            }
                            try {
                                voiceSearchLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Gérer l'absence de SpeechRecognizer
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Recherche vocale",
                                tint = PremiumEmerald
                            )
                        }
                        AnimatedVisibility(
                            visible = query.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = viewModel::clearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PremiumEmerald,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PremiumEmerald
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* la recherche est en temps réel */ })
            )
        }

        // ——— Filtres par type ———
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.values()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.onFilterChange(f) },
                    label = {
                        Text(
                            text = f.label,
                            fontWeight = if (filter == f) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (filter == f) {
                        {
                            Icon(
                                imageVector = when (f) {
                                    SearchFilter.LIVE -> Icons.Default.LiveTv
                                    SearchFilter.VOD -> Icons.Default.Movie
                                    SearchFilter.SERIES -> Icons.Default.Tv
                                    SearchFilter.ALL -> Icons.Default.Search
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PremiumEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = PremiumEmerald,
                        selectedLeadingIconColor = PremiumEmerald,
                        containerColor = ElevatedBlack,
                        labelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filter == f,
                        selectedBorderColor = PremiumEmerald.copy(alpha = 0.5f),
                        borderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ——— Résultats ———
        when {
            results.isEmpty() && query.isNotBlank() -> {
                EmptySearchState(query = query)
            }
            results.isEmpty() && query.isBlank() -> {
                SearchHintState()
            }
            else -> {
                // Section header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isShowingRecents) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Récemment regardés",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    } else {
                        Text(
                            text = "${results.size} résultat${if (results.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                ) {
                    items(results, key = { it.id }) { channel ->
                        SearchResultItem(
                            channel = channel,
                            onClick = {
                                when (channel.type) {
                                    ContentType.VOD_SERIES -> onContentClick(channel)
                                    ContentType.VOD_MOVIE -> onContentClick(channel)
                                    else -> onChannelClick(channel)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    channel: Channel,
    onClick: () -> Unit
) {
    val typeColor = when (channel.type) {
        ContentType.LIVE_TV, ContentType.LIVE_SPORTS, ContentType.LIVE_NEWS -> LiveTvColor
        ContentType.VOD_MOVIE -> VodColor
        ContentType.VOD_SERIES -> SeriesColor
        ContentType.RADIO -> PremiumGold
        else -> PremiumEmerald
    }

    val typeLabel = when (channel.type) {
        ContentType.LIVE_TV -> "LIVE"
        ContentType.LIVE_SPORTS -> "SPORT"
        ContentType.LIVE_NEWS -> "NEWS"
        ContentType.VOD_MOVIE -> "FILM"
        ContentType.VOD_SERIES -> "SÉRIE"
        ContentType.RADIO -> "RADIO"
        else -> ""
    }

    val typeIcon = when (channel.type) {
        ContentType.LIVE_TV, ContentType.LIVE_SPORTS, ContentType.LIVE_NEWS -> Icons.Default.LiveTv
        ContentType.VOD_MOVIE -> Icons.Default.Movie
        ContentType.VOD_SERIES -> Icons.Default.Tv
        ContentType.RADIO -> Icons.Default.Radio
        else -> Icons.Default.PlayArrow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail / Logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElevatedBlack),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Type badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(typeColor.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = typeLabel,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = channel.category.ifBlank { channel.groupTitle ?: "" },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play arrow
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Lire",
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun résultat pour",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = "\"$query\"",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vérifiez l'orthographe ou essayez un autre terme",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SearchHintState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PremiumEmerald.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = PremiumEmerald.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Recherche Universelle",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tapez le nom d'une chaîne, d'un film\nou d'une série pour commencer",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
