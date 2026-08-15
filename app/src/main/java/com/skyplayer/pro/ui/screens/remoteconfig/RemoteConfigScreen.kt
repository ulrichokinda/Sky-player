package com.skyplayer.pro.ui.screens.remoteconfig

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.model.RemoteConfig
import com.skyplayer.pro.data.model.RemoteConfigState
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange
import com.skyplayer.pro.ui.viewmodel.RemoteConfigViewModel
import com.skyplayer.pro.utils.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Écran expert de configuration à distance par QR Code
 * Design optimisé pour TV - QR visible à 3m avec mentions légales
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RemoteConfigScreen(
    onConfigApplied: () -> Unit = {},
    viewModel: RemoteConfigViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Fullscreen pour TV pendant l'affichage du QR Code, puis restauration à la sortie
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val window = activity?.window

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowInsetsControllerCompat(it, it.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val deviceId by viewModel.deviceId.collectAsState()
    val qrUrl by viewModel.qrUrl.collectAsState()
    val configState by viewModel.configState.collectAsState()

    // Collecter les événements (Toasts)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Générer QR bitmap
    val qrBitmap = remember(qrUrl) {
        QrCodeGenerator.generateQrCode(
            content = qrUrl,
            size = QrCodeGenerator.Sizes.TV_DISTANCE_3M,
            foregroundColor = android.graphics.Color.BLACK,
            backgroundColor = android.graphics.Color.WHITE
        )
    }

    // Observer les configs reçues pour auto-appliquer
    LaunchedEffect(configState) {
        if (configState is RemoteConfigState.Received) {
            val config = (configState as RemoteConfigState.Received).config
            viewModel.applyRemoteConfig(config, onConfigApplied)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = configState,
            transitionSpec = {
                fadeIn(tween(400)) + scaleIn(tween(400)) togetherWith
                fadeOut(tween(200)) + scaleOut(tween(200))
            },
            label = "config_animation"
        ) { state ->
            when (state) {
                is RemoteConfigState.Idle,
                is RemoteConfigState.Waiting -> {
                    QrCodeDisplay(
                        deviceId = deviceId,
                        qrBitmap = qrBitmap,
                        onRefresh = { viewModel.refreshQrCode() }
                    )
                }

                is RemoteConfigState.Received -> {
                    ApplyingConfigState(
                        config = state.config
                    )
                }

                is RemoteConfigState.Applied -> {
                    SuccessState(
                        playlistName = (state as RemoteConfigState.Applied).playlistName
                    )
                }

                is RemoteConfigState.Offline -> {
                    OfflineState(
                        onRetry = { viewModel.retryConnection() }
                    )
                }

                is RemoteConfigState.Error -> {
                    com.skyplayer.pro.ui.components.TrustErrorView(
                        message = (state as RemoteConfigState.Error).message,
                        onRetry = { viewModel.retryConnection() }
                    )
                }
            }
        }
    }
}

/**
 * Affichage principal du QR Code - Design expert TV
 */
@Composable
private fun QrCodeDisplay(
    deviceId: String,
    qrBitmap: Bitmap?,
    onRefresh: () -> Unit
) {
    // onRefresh could be used for a manual refresh button if needed
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header avec icône TV
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = ElectricSkyBlue,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Configuration à Distance",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // QR Code Container - 400dp pour visibilité 3m
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(pulse)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = ElectricSkyBlue.copy(alpha = 0.2f)
                )
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(
                    width = 3.dp,
                    color = ElectricSkyBlue,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code configuration à distance",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = ElectricSkyBlue,
                    strokeWidth = 6.dp
                )
            }
        }

        // Mention sous QR
        Text(
            text = "Scannez pour configurer votre playlist depuis votre smartphone",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ID MAC affiché
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(
                text = "ID APPAREIL",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Text(
                text = deviceId,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = PremiumGold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions étapes
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InstructionStep("1", "Ouvrez l'appareil photo de votre téléphone")
            InstructionStep("2", "Scannez le QR code ci-dessus")
            InstructionStep("3", "Remplissez les informations sur la page web")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // URL
        Text(
            text = "skyplayerapp.xyz/connect",
            fontSize = 20.sp,
            color = ElectricSkyBlue,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mention légale obligatoire
        Text(
            text = "Mention légale : SkyPlayer ne fournit aucun contenu TV. " +
                   "Nous sommes un lecteur uniquement.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(ElectricSkyBlue, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Text(
            text = text,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ApplyingConfigState(config: RemoteConfig) {
    val configType = when (config) {
        is RemoteConfig.XtreamConfig -> "Xtream Codes"
        is RemoteConfig.M3uConfig -> "Playlist M3U"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(100.dp),
            color = ElectricSkyBlue,
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Configuration reçue !",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Type: $configType",
            fontSize = 22.sp,
            color = PremiumGold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Application en cours...",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SuccessState(playlistName: String) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier
                .size(140.dp)
                .scale(scale.value)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Configuration appliquée !",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Playlist chargée :",
            fontSize = 22.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Text(
            text = playlistName,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = PremiumGold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun OfflineState(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = WarningOrange,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Hors ligne",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connexion internet requise pour la configuration à distance",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricSkyBlue,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Réessayer",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

