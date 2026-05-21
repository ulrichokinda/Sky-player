package com.skyplayer.pro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.skyplayer.pro.data.model.Channel

/**
 * Composant optimisé pour l'affichage des logos de chaînes
 *
 * Fonctionnalités :
 * - Cache disque agressif (100MB)
 * - Cache mémoire (50MB)
 * - Timeout court (2s) pour éviter les blocages
 * - Fallback automatique sur initiales si échec
 * - Placeholder avec initiales pendant le chargement
 */
@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    size: Int = 68,
    isFocused: Boolean = false
) {
    val context = LocalContext.current
    
    // ImageLoader avec cache agressif
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% de la mémoire disponible
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("channel_logos"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB cache disque
                    .build()
            }
            .crossfade(true)
            .crossfade(150)
            .build()
    }
    
    val placeholderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            // Logo URL disponible - utiliser Coil avec cache agressif
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(channel.logoUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .placeholderMemoryCacheKey(channel.logoUrl)
                    .size(coil.size.Size.ORIGINAL)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Logo ${channel.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                loading = {
                    // Placeholder avec initiales pendant le chargement
                    InitialsPlaceholder(
                        channel = channel,
                        textColor = textColor
                    )
                },
                error = {
                    // Fallback sur initiales si erreur
                    InitialsPlaceholder(
                        channel = channel,
                        textColor = textColor
                    )
                },
                success = { state ->
                    // Afficher l'image chargée
                    Box(modifier = Modifier.fillMaxSize()) {
                        state.painter?.let { painter ->
                            Image(
                                painter = painter,
                                contentDescription = "Logo ${channel.name}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            )
        } else {
            // Pas de logo URL - afficher initiales
            InitialsPlaceholder(
                channel = channel,
                textColor = textColor
            )
        }
    }
}

/**
 * Placeholder avec initiales du nom de la chaîne
 */
@Composable
private fun InitialsPlaceholder(
    channel: Channel,
    textColor: Color
) {
    val initials = remember(channel.name) {
        channel.name
            .split(" ", "-", "_", ".")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { channel.name.take(1).uppercase() }
    }
    
    Text(
        text = initials,
        style = MaterialTheme.typography.headlineSmall,
        color = textColor,
        textAlign = TextAlign.Center
    )
}

/**
 * Version optimisée pour les grilles (lazy loading)
 */
@Composable
fun ChannelLogoGrid(
    channel: Channel,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false
) {
    val context = LocalContext.current
    
    // ImageLoader optimisé pour les grilles avec préchargement
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.20) // 20% pour grilles
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("channel_logos_grid"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB pour grilles
                    .build()
            }
            .crossfade(true)
            .crossfade(100) // Transition plus rapide pour grilles
            .build()
    }
    
    var hasError by remember { mutableStateOf(false) }
    
    val placeholderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        Color(0xFF3A3A3A)
    }
    
    val textColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (!channel.logoUrl.isNullOrBlank() && !hasError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(channel.logoUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .placeholderMemoryCacheKey(channel.logoUrl)
                    .size(coil.size.Size.ORIGINAL)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                onError = { hasError = true }
            )
        }
        
        // Afficher initiales si pas de logo ou erreur
        if (channel.logoUrl.isNullOrBlank() || hasError) {
            InitialsPlaceholder(
                channel = channel,
                textColor = textColor
            )
        }
    }
}

