package com.skyplayer.pro.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.skyplayer.pro.ui.theme.ElevatedBlack

/**
 * Image d'affiche premium pour les cartes de contenu.
 *
 * Pendant le chargement : shimmer teinté émeraude animé.
 * Quand l'image est prête : fondu progressif (crossfade) — rendu fluide
 * et premium, plus d'apparition brutale.
 */
@Composable
fun PremiumPosterImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    errorIcon: ImageVector = Icons.Default.Movie
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        content = {
            val imageState = painter?.state
            val loaded = imageState is AsyncImagePainter.State.Success
            Crossfade(
                targetState = loaded,
                animationSpec = tween(durationMillis = 450),
                label = "posterFade"
            ) { isLoaded ->
                when {
                    isLoaded && painter != null -> {
                        Image(
                            painter = painter,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale
                        )
                    }
                    imageState is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ElevatedBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = errorIcon,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = contentDescription?.take(1)?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                    else -> {
                        PremiumShimmerCard(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    )
}
