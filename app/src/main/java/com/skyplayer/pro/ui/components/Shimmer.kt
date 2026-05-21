package com.skyplayer.pro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Effet Shimmer (Suggestion 3) pour des chargements élégants et fluides
 */
@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    width: Float = 300f
) {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.6f),
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .background(brush, shape = RoundedCornerShape(8.dp))
    )
}

@Composable
fun ChannelListShimmer() {
    Column(modifier = Modifier.padding(16.dp)) {
        repeat(5) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ShimmerItem(modifier = Modifier.size(50.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    ShimmerItem(modifier = Modifier.height(20.dp).width(150.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerItem(modifier = Modifier.height(14.dp).width(100.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
