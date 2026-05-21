package com.skyplayer.pro.di

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import timber.log.Timber
import com.google.firebase.database.FirebaseDatabase
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.firebase.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Module Hilt principal pour l'injection de dépendances
 * Configure ExoPlayer avec buffering agressif pour réseaux instables
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fournit le LoadControl personnalisé avec buffering agressif anti-coupure
     * Optimisé pour les connexions mobiles instables en Afrique (Edge/3G/4G)
     *
     * - minBufferMs: 30s  → buffer minimum avant démarrage (évite lecture hachée)
     * - maxBufferMs: 60s  → buffer max en arrière-plan pour résister aux coupures
     * - bufferForPlaybackMs: 4s → attente au 1er démarrage pour sécuriser le flux
     * - bufferForPlaybackAfterRebufferMs: 6s → reprise stable après interruption
     */
    @Provides
    @Singleton
    fun provideLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,   // minBufferMs: 30s
                60_000,   // maxBufferMs: 60s
                4_000,    // bufferForPlaybackMs: 4s
                6_000     // bufferForPlaybackAfterRebufferMs: 6s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .build()
    }

    /**
     * Fournit le TrackSelector adaptatif avec support 4K
     * Ajuste automatiquement la qualité vidéo (SD, 720p, 1080p, 4K) selon le débit réseau
     */
    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        // Configuration AdaptiveTrackSelection pour transitions fluides de qualité
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        
        val trackSelector = DefaultTrackSelector(context, adaptiveTrackSelection)
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setMinVideoBitrate(100_000) // 100kbps minimum (240p)
            .setMaxVideoBitrate(20_000_000) // 20Mbps maximum pour support 4K
            .build()
        return trackSelector
    }

    /**
     * Fournit le cache pour le buffering vidéo
     * 100MB de cache local pour réduire la consommation data
     */
    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val evictor = LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024) // 250MB - cache élargi pour zapping rapide
        return SimpleCache(cacheDir, evictor)
    }

    /**
     * Fournit Firebase Realtime Database
     * Pour la gestion des licences et activation à distance
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().apply {
            // Activer la persistence pour fonctionnement hors-ligne
            setPersistenceEnabled(true)
        }
    }

    /**
     * Fournit une RenderersFactory optimisée pour le décodage matériel (Suggestion 4)
     * Favorise la fluidité et le support 4K/UHD
     */
    @Provides
    @Singleton
    fun provideRenderersFactory(@ApplicationContext context: Context): RenderersFactory {
        return DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true) // Fallback software si HW échoue
            .setMediaCodecSelector(androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT)
    }

    /**
     * Fournit le ExoPlayer configuré pour réseaux instables
     * Note: Non-singleton pour éviter les conflits de cycle de vie (release/reuse)
     */
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        loadControl: LoadControl,
        renderersFactory: RenderersFactory,
        trackSelector: DefaultTrackSelector,
        cache: SimpleCache
    ): ExoPlayer {
        // HttpDataSource avec timeouts 15s pour connexions lentes
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)

        // DataSource avec cache alimenté par le HttpDataSource optimisé
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                playWhenReady = false
                repeatMode = ExoPlayer.REPEAT_MODE_OFF

                // Listener de reconnexion infinie en cas de coupure réseau
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                Timber.d("⏳ ExoPlayer: buffering en cours...")
                            }
                            Player.STATE_READY -> {
                                Timber.d("▶️ ExoPlayer: prêt à lire")
                            }
                            Player.STATE_ENDED -> {
                                Timber.d("⏹️ ExoPlayer: lecture terminée")
                            }
                            Player.STATE_IDLE -> {
                                Timber.d("💤 ExoPlayer: idle")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Timber.w("⚠️ ExoPlayer erreur: ${error.message} — tentative de reconnexion...")
                        // Tentative de reconnexion automatique sans couper le flux
                        prepare()
                        playWhenReady = true
                    }
                })
            }
    }

    /**
     * Fournit RemoteConfigManager pour configuration à distance via QR Code
     * Écoute Firebase pending_configs avec reconnexion automatique
     */
    @Provides
    @Singleton
    fun provideRemoteConfigManager(
        firebaseDatabase: FirebaseDatabase,
        @ApplicationContext context: Context
    ): RemoteConfigManager {
        return RemoteConfigManager(firebaseDatabase, context)
    }

    /**
     * Fournit EncryptedPrefs pour stockage sécurisé des credentials
     */
    @Provides
    @Singleton
    fun provideEncryptedPrefs(
        @ApplicationContext context: Context
    ): EncryptedPrefs {
        return EncryptedPrefs(context)
    }
}
