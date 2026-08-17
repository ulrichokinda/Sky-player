package com.skyplayer.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.skyplayer.pro.ui.theme.CardBlack
import com.skyplayer.pro.ui.theme.PremiumEmerald
import com.skyplayer.pro.ui.theme.PureBlack

/**
 * Dialogue de saisie du code PIN pour le contrôle parental
 */
@Composable
fun PinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onForgotPassword: () -> Unit = {},
    error: String? = null
) {
    var pin by remember { mutableStateOf("") }

    // Focus initial sur le champ PIN pour la télécommande TV (saisie par touches chiffres)
    val pinFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        pinFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PremiumEmerald,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CONTRÔLE PARENTAL",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Veuillez saisir votre code PIN pour accéder à ce contenu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) pin = it },
                    modifier = Modifier
                        .width(160.dp)
                        .focusRequester(pinFocusRequester),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        color = PremiumEmerald
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = PureBlack,
                        unfocusedContainerColor = PureBlack,
                        focusedIndicatorColor = PremiumEmerald
                    )
                )
                
                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onForgotPassword) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, modifier = Modifier.size(16.dp), tint = PremiumEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Code oublié ?", color = PremiumEmerald, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
            ) {
                Text("DÉVERROUILLER", fontWeight = FontWeight.Bold, color = PureBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULER", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

/**
 * Dialogue de configuration initiale du code parental
 */
@Composable
fun SetupPinDialog(
    onSetupComplete: (pin: String, question: String, answer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("Ville de naissance ?") }
    var answer by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1: PIN, 2: Security Question
    var error by remember { mutableStateOf<String?>(null) }

    // Focus initial sur le premier champ pour la télécommande TV
    val pinFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        pinFocusRequester.requestFocus()
    }

    val securityQuestions = listOf(
        "Ville de naissance ?",
        "Nom de votre premier animal ?",
        "Marque de votre première voiture ?",
        "Nom de votre école primaire ?"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (step == 1) "CRÉER VOTRE CODE PIN" else "SÉCURITÉ RÉCUPÉRATION",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (step == 1) {
                    Text(
                        text = "Définissez un code à 4 chiffres pour protéger les catégories sensibles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) pin = it },
                        label = { Text("Nouveau code PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pinFocusRequester),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) confirmPin = it },
                        label = { Text("Confirmer le code PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                    )
                } else {
                    Text(
                        text = "Cette question servira à réinitialiser votre code en cas d'oubli.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
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
                                        question = q
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Votre réponse") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length == 4 && pin == confirmPin) {
                            step = 2
                            error = null
                        } else {
                            error = "Les codes ne correspondent pas ou sont incomplets"
                        }
                    } else {
                        if (answer.isNotBlank()) {
                            onSetupComplete(pin, question, answer)
                        } else {
                            error = "Veuillez répondre à la question"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
            ) {
                Text(if (step == 1) "SUIVANT" else "TERMINER", fontWeight = FontWeight.Bold, color = PureBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULER", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

/**
 * Dialogue de récupération du code PIN
 */
@Composable
fun RecoveryPinDialog(
    question: String,
    onVerify: (answer: String) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null
) {
    var answer by remember { mutableStateOf("") }

    // Focus initial sur le champ réponse pour la télécommande TV
    val answerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        answerFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "RÉCUPÉRATION DU CODE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Répondez à votre question de sécurité pour définir un nouveau code PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    color = PremiumEmerald,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Votre réponse") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(answerFocusRequester),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = PremiumEmerald)
                )
                
                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(answer) },
                enabled = answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald)
            ) {
                Text("VÉRIFIER", fontWeight = FontWeight.Bold, color = PureBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULER", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}
