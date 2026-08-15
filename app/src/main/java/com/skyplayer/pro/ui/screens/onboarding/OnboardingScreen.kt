package com.skyplayer.pro.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.ui.screens.welcome.WelcomeViewModel
import com.skyplayer.pro.ui.theme.ElectricSkyBlue
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PremiumGold
import com.skyplayer.pro.ui.theme.PureBlack
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val highlights: List<String> = emptyList()
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Default.PlayArrow,
        title = "Bienvenue sur Sky Player Pro",
        subtitle = "Votre lecteur IPTV premium, optimisé pour l'Afrique",
        highlights = listOf(
            "Lecture fluide même en 3G/4G",
            "Interface moderne et intuitive",
            "Compatible Mobile, Tablette et TV"
        )
    ),
    OnboardingSlide(
        icon = Icons.Default.Speed,
        title = "Performance optimale",
        subtitle = "Conçu pour les réseaux africains",
        highlights = listOf(
            "Buffering intelligent 90-120s",
            "Reconnexion automatique",
            "Multi-vue : 2 à 4 chaînes simultanées"
        )
    ),
    OnboardingSlide(
        icon = Icons.Default.Tv,
        title = "Tout votre contenu",
        subtitle = "Live TV, Films, Séries et Favoris",
        highlights = listOf(
            "Navigation par swipe entre sections",
            "Cartes immersives pour VOD & Séries",
            "Contrôle parental simplifié"
        )
    ),
    OnboardingSlide(
        icon = Icons.Default.AddLink,
        title = "Ajoutez votre playlist",
        subtitle = "Connectez votre abonnement IPTV en quelques secondes",
        highlights = listOf(
            "URL M3U ou Xtream Codes",
            "Configuration par QR Code (TV)",
            "Prêt à regarder immédiatement"
        )
    )
)

/**
 * Phase 2 — Onboarding en 4 slides avec option de passer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onAddPlaylist: () -> Unit,
    onRemoteConfig: () -> Unit = {},
    onComplete: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.lastIndex

    val finishOnboarding: () -> Unit = {
        viewModel.completeOnboarding()
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PureBlack, Color(0xFF0A1628), PureBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = finishOnboarding) {
                    Text(
                        text = "Passer",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingSlideContent(slide = slides[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(slides.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    ElectricSkyBlue
                                else
                                    Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            if (isLastPage) {
                Button(
                    onClick = onAddPlaylist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricSkyBlue)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajouter une playlist",
                        fontWeight = FontWeight.SemiBold,
                        color = PureBlack
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRemoteConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Configurer par QR Code (TV)", color = ElectricSkyBlue)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = finishOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Explorer sans playlist", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricSkyBlue)
                ) {
                    Text("Suivant", fontWeight = FontWeight.Bold, color = PureBlack)
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GradientElectricStart.copy(alpha = 0.8f),
                            GradientElectricEnd.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                brush = Brush.horizontalGradient(
                    colors = listOf(GradientElectricStart, GradientElectricEnd)
                )
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = slide.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        if (slide.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))

            slide.highlights.forEach { highlight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
