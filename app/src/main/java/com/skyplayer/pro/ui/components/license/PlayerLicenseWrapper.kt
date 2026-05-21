package com.skyplayer.pro.ui.components.license

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.skyplayer.pro.data.license.LicenseSecurityManager
import com.skyplayer.pro.ui.viewmodel.LicenseViewModel
import timber.log.Timber

/**
 * Wrapper pour intégrer la protection de licence dans PlayerScreen
 * 
 * Utilisation dans PlayerScreen:
 * ```
 * PlayerLicenseWrapper(
 *     onNavigateToLicense = { navController.navigate(Routes.License.route) }
 * ) { licenseValid ->
 *     if (licenseValid) {
 *         // Afficher le lecteur vidéo
 *         VideoPlayer(...)
 *     }
 * }
 * ```
 */
@Composable
fun PlayerLicenseWrapper(
    viewModel: LicenseViewModel = hiltViewModel(),
    onNavigateToLicense: () -> Unit,
    content: @Composable (isLicenseValid: Boolean) -> Unit
) {
    var licenseError by remember { mutableStateOf<LicenseSecurityManager.InvalidReason?>(null) }
    var isLicenseChecked by remember { mutableStateOf(false) }

    LicenseGuard(
        viewModel = viewModel,
        onLicenseInvalid = { reason ->
            Timber.w("🚫 Player bloqué - Licence invalide: $reason")
            licenseError = reason
            isLicenseChecked = true
            
            // Naviguer vers l'écran de licence après un court délai
            onNavigateToLicense()
        },
        onLicenseValid = {
            Timber.i("✅ Player autorisé - Licence valide")
            isLicenseChecked = true
        }
    ) {
        // Licence valide - afficher le contenu
        content(true)
    }

    // Si erreur de licence, afficher rien (la navigation est gérée par onLicenseInvalid)
    if (licenseError != null && !isLicenseChecked) {
        content(false)
    }
}

/**
 * Extension pour afficher un message selon la raison du blocage
 */
fun LicenseSecurityManager.InvalidReason.toDisplayMessage(): String {
    return when (this) {
        LicenseSecurityManager.InvalidReason.DEACTIVATED -> 
            "Votre licence a été révoquée. Contactez votre revendeur."
        LicenseSecurityManager.InvalidReason.TRIAL_EXPIRED -> 
            "Votre période d'essai a expiré. Activez l'application sur skyplayerapp.xyz"
        LicenseSecurityManager.InvalidReason.SERVER_VALIDATION_FAILED -> 
            "Impossible de vérifier votre licence. Vérifiez votre connexion internet."
    }
}
