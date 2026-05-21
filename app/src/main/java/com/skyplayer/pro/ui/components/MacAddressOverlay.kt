package com.skyplayer.pro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.ui.viewmodel.DeviceInfoViewModel

/**
 * Overlay affichant l'adresse MAC en haut à droite de l'écran
 * Visible sur tous les écrans, format lowercase
 * Pour TV: taille légèrement augmentée
 *
 * @param isVisible Si false, le MAC est masqué (mode plein écran player)
 */
@Composable
fun MacAddressOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    viewModel: DeviceInfoViewModel = hiltViewModel()
) {
    val deviceId by viewModel.deviceId.collectAsState()

    AnimatedVisibility(
        visible = isVisible && deviceId.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .padding(top = 8.dp, end = 8.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = deviceId.lowercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Version TV avec texte plus grand
 * @param isVisible Si false, le MAC est masqué (mode plein écran player)
 */
@Composable
fun MacAddressOverlayTV(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    viewModel: DeviceInfoViewModel = hiltViewModel()
) {
    val deviceId by viewModel.deviceId.collectAsState()

    AnimatedVisibility(
        visible = isVisible && deviceId.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .padding(top = 16.dp, end = 16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = deviceId.lowercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
