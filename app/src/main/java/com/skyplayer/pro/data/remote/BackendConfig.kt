package com.skyplayer.pro.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.skyplayer.pro.BuildConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de configuration backend — récupère la clé API depuis
 * Firebase Remote Config au lieu de BuildConfig (embed dans l'APK).
 *
 * Flux :
 *  1. Au démarrage, lecture du cache local (Instant Display)
 *  2. Fetch async depuis Firebase (mise en cache 1h)
 *  3. Si Remote Config indisponible → fallback sur BuildConfig
 *
 * Avantages :
 *  - La clé n'est JAMAIS dans le code source (extractable via strings/apk)
 *  - Rotation de clé possible sans mise à jour de l'app
 *  - Fallback gracieux si Firebase est down
 */
@Singleton
class BackendConfig @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1h cache
            .build()

        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(settings)
            // Valeur par défaut = clé BuildConfig (fallback si Remote Config vide)
            setDefaultsAsync(
                mapOf(KEY_API_KEY to BuildConfig.LICENSE_API_KEY)
            )
        }
    }

    /**
     * Clé API active — lecture synchrone du cache (Instant Display).
     * Met à jour en arrière-plan via [fetchAndUpdate].
     */
    val apiKey: String
        get() {
            val cached = remoteConfig.getString(KEY_API_KEY)
            return if (cached.isNotBlank() && cached != DEFAULT_PLACEHOLDER) {
                cached
            } else {
                BuildConfig.LICENSE_API_KEY
            }
        }

    /**
     * Fetch async de la config depuis Firebase.
     * À appeler au démarrage de l'app (dans le ViewModel Splash ou Application).
     */
    fun fetchAndUpdate() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                val newKey = remoteConfig.getString(KEY_API_KEY)
                if (newKey.isNotBlank() && newKey != DEFAULT_PLACEHOLDER) {
                    Timber.i("🔑 BackendConfig: API key updated from Remote Config")
                } else {
                    Timber.d("🔑 BackendConfig: using BuildConfig fallback")
                }
            }
            .addOnFailureListener { e ->
                Timber.w("⚠️ BackendConfig: fetch failed, using fallback: ${e.message}")
            }
    }

    companion object {
        private const val KEY_API_KEY = "backend_api_key"
        private const val DEFAULT_PLACEHOLDER = ""
    }
}
