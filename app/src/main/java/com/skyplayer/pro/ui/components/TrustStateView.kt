package com.skyplayer.pro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PureBlack

data class TrustAction(
    val label: String,
    val onClick: () -> Unit
)

/**
 * État vide ou erreur unifié — langage rassurant, action claire.
 */
@Composable
fun TrustStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White.copy(alpha = 0.35f),
    primaryAction: TrustAction? = null,
    secondaryAction: TrustAction? = null,
    footer: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.55f),
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        if (primaryAction != null || secondaryAction != null) {
            Spacer(modifier = Modifier.height(28.dp))

            if (primaryAction != null) {
                Button(
                    onClick = primaryAction.onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumEmerald,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = primaryAction.label,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (secondaryAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = secondaryAction.onClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = secondaryAction.label,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        footer?.let {
            Spacer(modifier = Modifier.height(24.dp))
            it()
        }
    }
}

@Composable
fun TrustErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Un problème est survenu",
    helpAction: TrustAction? = null
) {
    TrustStateView(
        icon = Icons.Default.Warning,
        title = title,
        message = message,
        iconTint = Color(0xFFFF6B6B),
        modifier = modifier,
        primaryAction = TrustAction(label = "Réessayer", onClick = onRetry),
        secondaryAction = helpAction
    )
}
