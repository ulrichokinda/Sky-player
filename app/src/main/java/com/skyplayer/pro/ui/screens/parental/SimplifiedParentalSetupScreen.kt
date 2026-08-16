package com.skyplayer.pro.ui.screens.parental

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.GradientElectricEnd
import com.skyplayer.pro.ui.theme.GradientElectricStart
import com.skyplayer.pro.ui.theme.PureBlack
import com.skyplayer.pro.ui.viewmodel.ParentalViewModel

/**
 * Phase 2 — Contrôle parental simplifié en 3 étapes guidées.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedParentalSetupScreen(
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    parentalViewModel: ParentalViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("Ville de naissance ?") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val securityQuestions = listOf(
        "Ville de naissance ?",
        "Nom de votre premier animal ?",
        "Marque de votre première voiture ?",
        "Nom de votre école primaire ?"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contrôle Parental",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack
                )
            )
        },
        containerColor = PureBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = PremiumEmerald,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Étape $step sur 3",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (step) {
                1 -> ParentalIntroStep(onNext = { step = 2 })
                2 -> ParentalPinStep(
                    pin = pin,
                    confirmPin = confirmPin,
                    error = error,
                    onPinChange = { pin = it; error = null },
                    onConfirmPinChange = { confirmPin = it; error = null },
                    onNext = {
                        when {
                            pin.length != 4 -> error = "Le code doit contenir 4 chiffres"
                            pin != confirmPin -> error = "Les codes ne correspondent pas"
                            else -> {
                                error = null
                                step = 3
                            }
                        }
                    }
                )

                3 -> ParentalQuestionStep(
                    question = question,
                    answer = answer,
                    securityQuestions = securityQuestions,
                    error = error,
                    onQuestionChange = { question = it },
                    onAnswerChange = { answer = it; error = null },
                    onComplete = {
                        if (answer.isBlank()) {
                            error = "Veuillez répondre à la question"
                        } else {
                            parentalViewModel.manager.setupParentalControl(pin, question, answer)
                            onComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ParentalIntroStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PremiumEmerald.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChildCare,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = PremiumEmerald
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Protégez votre famille",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bloquez l'accès aux contenus sensibles avec un code PIN simple. Configuration en moins de 2 minutes.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        FeatureBullet(Icons.Default.Shield, "Catégories adultes verrouillées automatiquement")
        Spacer(modifier = Modifier.height(12.dp))
        FeatureBullet(Icons.Default.Lock, "Code PIN à 4 chiffres facile à retenir")
        Spacer(modifier = Modifier.height(12.dp))
        FeatureBullet(Icons.Default.QuestionAnswer, "Question de récupération en cas d'oubli")

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
        ) {
            Text(
                text = "Commencer",
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumEmerald,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ParentalPinStep(
    pin: String,
    confirmPin: String,
    error: String?,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PremiumEmerald
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Créez votre code PIN",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choisissez un code à 4 chiffres",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) onPinChange(it) },
                    label = { Text("Nouveau code PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) onConfirmPinChange(it) },
                    label = { Text("Confirmer le code PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = Color.Red, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = pin.length == 4 && confirmPin.length == 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
        ) {
            Text("Suivant", fontWeight = FontWeight.Bold, color = PureBlack)
        }
    }
}

@Composable
private fun ParentalQuestionStep(
    question: String,
    answer: String,
    securityQuestions: List<String>,
    error: String?,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QuestionAnswer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PremiumEmerald
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Question de sécurité",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pour récupérer votre code en cas d'oubli",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = PureBlack)
                    ) {
                        Text(
                            text = question,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardBlack)
                    ) {
                        securityQuestions.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q, color = Color.White) },
                                onClick = {
                                    onQuestionChange(q)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text("Votre réponse") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = Color.Red, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            enabled = answer.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
        ) {
            Text("Terminer", fontWeight = FontWeight.Bold, color = PureBlack)
        }
    }
}
