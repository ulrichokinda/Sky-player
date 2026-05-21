package com.skyplayer.pro.ui.components.license

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyplayer.pro.data.license.LicenseSecurityManager
import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import timber.log.Timber

/**
 * LicenseGuard - Composant de protection de la licence pendant la lecture
 * 
 * À placer dans l'écran de lecteur pour :
 * 1. Vérifier la licence avant de démarrer la lecture
 * 2. Surveiller en temps réel les révocations de licence
 * 3. Bloquer immédiatement si la licence devient invalide
 */
@Composable
fun LicenseGuard(
    viewModel: LicenseViewModel = hiltViewModel(),
    onLicenseInvalid: (reason: LicenseSecurityManager.InvalidReason) -> Unit,
    onLicenseValid: () -> Unit,
    content: @Composable () -> Unit
) {
    val securityManager = viewModel.licenseSecurityManager
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isLicenseValid by remember { mutableStateOf(false) }
    var hasCheckedLicense by remember { mutableStateOf(false) }

    // Configuration du callback de révocation
    DisposableEffect(securityManager) {
        securityManager.setLicenseInvalidCallback(object : LicenseSecurityManager.LicenseInvalidCallback {
            override fun onLicenseInvalid(reason: LicenseSecurityManager.InvalidReason) {
                Timber.w("🚨 LicenceGuard: Licence invalide - $reason")
                isLicenseValid = false
                onLicenseInvalid(reason)
            }
        })

        onDispose {
            securityManager.stopLicenseMonitoring()
            Timber.d("👋 LicenseGuard disposed")
        }
    }

    // Vérification initiale de la licence
    LaunchedEffect(Unit) {
        if (!hasCheckedLicense) {
            Timber.i("🔐 LicenseGuard: Vérification initiale de la licence...")
            
            // Validation côté serveur (anti-triche)
            val serverValidation = securityManager.validateAccessWithServerTime()
            
            if (serverValidation.isValid) {
                Timber.i("✅ LicenseGuard: Licence valide, démarrage surveillance...")
                isLicenseValid = true
                hasCheckedLicense = true
                
                // Démarrer la surveillance temps réel
                securityManager.startLicenseMonitoring()
                
                onLicenseValid()
            } else {
                Timber.w("🚫 LicenseGuard: Licence invalide!")
                isLicenseValid = false
                hasCheckedLicense = true
                
                val reason = when {
                    !serverValidation.isTrialValid && !serverValidation.isActivated -> 
                        LicenseSecurityManager.InvalidReason.TRIAL_EXPIRED
                    !serverValidation.isActivated -> 
                        LicenseSecurityManager.InvalidReason.DEACTIVATED
                    else -> 
                        LicenseSecurityManager.InvalidReason.SERVER_VALIDATION_FAILED
                }
                
                onLicenseInvalid(reason)
            }
        }
    }

    // Afficher le contenu uniquement si la licence est valide
    if (isLicenseValid) {
        content()
    }
}
