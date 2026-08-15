package com.skyplayer.pro.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.R
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToWelcome: () -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel()
) {
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val scale = remember { Animatable(0.3f) }
    
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is SplashViewModel.SplashNavigation.ToDashboard -> {
                    onNavigateToDashboard()
                }
                is SplashViewModel.SplashNavigation.ToWelcome -> {
                    onNavigateToWelcome()
                }
            }
            viewModel.onNavigationConsumed()
        }
    }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GradientElectricStart.copy(alpha = 0.28f),
                                    PureBlack
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Sky Player Pro",
                    modifier = Modifier.size(70.dp),
                    tint = GradientElectricStart
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sky Player",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GradientElectricStart
                )
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(
                        color = PremiumGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumGold,
                        letterSpacing = 6.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chargement...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}
