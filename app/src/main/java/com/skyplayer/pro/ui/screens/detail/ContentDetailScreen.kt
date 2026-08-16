package com.skyplayer.pro.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.data.model.ContentMetadata
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.ElevatedBlack
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.VodColor

/**
 * Écran de détails d'un Film ou d'une Série
 * Affiche: poster, synopsis, date de sortie, acteurs, bouton LIRE
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentDetailScreen(
    contentId: String,
    onPlayClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ContentDetailViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    LaunchedEffect(contentId) {
        viewModel.loadContent(contentId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (val state = uiState) {
            is ContentDetailUiState.Loading -> {
                CircularProgressIndicator(
                    color = PremiumEmerald,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ContentDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Contenu introuvable",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
                    ) {
                        Text("Retour")
                    }
                }
            }

            is ContentDetailUiState.Success -> {
                ContentDetailBody(
                    channel = state.channel,
                    metadata = state.metadata,
                    onPlayClick = { onPlayClick(contentId) },
                    onBackClick = onBackClick
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentDetailBody(
    channel: com.skyplayer.pro.data.model.Channel,
    metadata: ContentMetadata?,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val posterUrl = metadata?.posterUrl ?: channel.logoUrl

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond flouté (backdrop)
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .blur(20.dp)
            )
        }
        // Dégradé de recouvrement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            PureBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Barre haut avec bouton retour
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
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Contenu principal en ligne: poster + infos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Poster grand format
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedBlack)
                ) {
                    SubcomposeAsyncImage(
                        model = posterUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = PremiumEmerald,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ElevatedBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = PremiumEmerald.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                }

                // Informations rapides
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Titre
                    Text(
                        text = metadata?.title ?: channel.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Note IMDB
                    metadata?.imdbRating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$rating / 10",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PremiumGold
                            )
                        }
                    }

                    // Date de sortie
                    val releaseDate = metadata?.releaseDate ?: metadata?.year?.toString()
                    if (releaseDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = releaseDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Durée
                    metadata?.duration?.let { dur ->
                        Text(
                            text = dur,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Genres
                    val genres = metadata?.getGenresList() ?: emptyList()
                    if (genres.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            genres.forEach { genre ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = PremiumEmerald.copy(alpha = 0.15f),
                                        labelColor = PremiumEmerald
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bouton LIRE central et visible
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumEmerald
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LIRE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Synopsis
            val synopsis = metadata?.plot
            if (!synopsis.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Acteurs
            val actors = metadata?.getActorsList() ?: emptyList()
            if (actors.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Acteurs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        actors.take(10).forEach { actor ->
                            ActorChip(name = actor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Réalisateur
            metadata?.director?.let { dir ->
                if (dir.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Réalisateur",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dir,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ActorChip(name: String) {
    Row(
        modifier = Modifier
            .background(
                color = ElevatedBlack,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(PremiumEmerald.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = PremiumEmerald,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
