package com.skyplayer.pro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.viewmodel.AppNetworkStatus
import com.skyplayer.pro.ui.viewmodel.AppStatusUiState
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.theme.SuccessGreen
import com.skyplayer.pro.ui.theme.WarningOrange

/**
 * Bandeau de statut compact — réseau, licence, contenu.
 * Un tap ouvre l'écran Ma Ligne pour plus de détails.
 */
@Composable
fun TrustStatusBanner(
    state: AppStatusUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerColor by animateColorAsState(
        targetValue = when {
            state.networkStatus == AppNetworkStatus.Offline -> Color(0xFF3D1515)
            state.licenseWarning -> Color(0xFF2A2210)
            state.networkStatus == AppNetworkStatus.Cellular -> Color(0xFF1A1A10)
            else -> CardBlack
        },
        label = "bannerColor"
    )

    val accentColor = when {
        state.networkStatus == AppNetworkStatus.Offline -> Color(0xFFFF6B6B)
        state.licenseWarning -> WarningOrange
        state.networkStatus == AppNetworkStatus.Cellular -> WarningOrange
        else -> SuccessGreen
    }

    ColumnBannerContent(
        state = state,
        bannerColor = bannerColor,
        accentColor = accentColor,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ColumnBannerContent(
    state: AppStatusUiState,
    bannerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bannerColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = networkIcon(state.networkStatus),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildBannerText(state),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (state.isSyncing) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = ElectricSkyBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.status_banner_details),
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (state.isSyncing && state.syncProgress in 1..99) {
            LinearProgressIndicator(
                progress = { state.syncProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ElectricSkyBlue,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun buildBannerText(state: AppStatusUiState): String {
    val network = when (state.networkStatus) {
        AppNetworkStatus.Checking -> stringResource(R.string.dash_checking)
        AppNetworkStatus.Wifi -> stringResource(R.string.dash_signal_wifi)
        AppNetworkStatus.Cellular -> stringResource(R.string.dash_signal_data)
        AppNetworkStatus.Connected -> stringResource(R.string.dash_signal_connected)
        AppNetworkStatus.Offline -> stringResource(R.string.dash_offline)
    }

    val parts = buildList {
        add(network)
        if (state.licenseLabel.isNotBlank()) add(state.licenseLabel)
        if (state.channelCount > 0) {
            add(stringResource(R.string.status_channels_loaded, state.channelCount))
        } else if (state.playlistName.isNotBlank()) {
            add(state.playlistName)
        }
    }
    return parts.joinToString(" · ")
}

private fun networkIcon(status: AppNetworkStatus): ImageVector = when (status) {
    AppNetworkStatus.Wifi -> Icons.Default.Wifi
    AppNetworkStatus.Offline -> Icons.Default.CloudOff
    else -> Icons.Default.SignalCellularAlt
}
