package com.skyplayer.pro

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application principale de Sky Player Pro
 * Configure Hilt pour l'injection de dépendances et Coil pour le cache d'images
 */
@HiltAndroidApp
class SkyPlayerApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        
        // Initialisation du logger en debug
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialisation Firebase (OBLIGATOIRE pour le système de licence)
        try {
            FirebaseApp.initializeApp(this)
            // La persistence est maintenant configurée dans AppModule
            Timber.i("🔥 Firebase initialisé avec succès")
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur initialisation Firebase: ${e.message}")
            Timber.w("⚠️ L'application fonctionnera en mode hors-ligne limité")
        }
        
        Timber.d("📱 Sky Player Pro démarré - Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    /**
     * Configuration personnalisée de Coil pour optimiser le cache des logos
     * Essentiel pour économiser la data sur les connexions limitées
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% de la mémoire disponible
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("channel_logos"))
                    .maxSizeBytes(250 * 1024 * 1024) // Augmenté à 250MB pour stabilité optimale
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .respectCacheHeaders(true) // Respecte les headers de cache du serveur
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
