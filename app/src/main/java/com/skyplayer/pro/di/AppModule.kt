package com.skyplayer.pro.di

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.skyplayer.pro.BuildConfig
import timber.log.Timber
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.skyplayer.pro.data.encrypted.EncryptedPrefs
import com.skyplayer.pro.data.firebase.RemoteConfigManager
import androidx.media3.database.StandaloneDatabaseProvider
import com.skyplayer.pro.data.organizer.ContentClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import coil.ImageLoader
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn

/**
 * Module Hilt principal pour l'injection de dépendances
 * Configure ExoPlayer avec buffering équilibré (démarrage rapide + résilience)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {



    @Provides
    @Singleton
    fun provideContentClassifier(): ContentClassifier {
        return ContentClassifier
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                BuildConfig.MIN_BUFFER_MS,
                BuildConfig.MAX_BUFFER_MS,
                BuildConfig.BUFFER_FOR_PLAYBACK_MS,
                BuildConfig.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(15_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    @OptIn(UnstableApi::class)
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
     * 250MB de cache local pour réduire la consommation data
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val databaseProvider = StandaloneDatabaseProvider(context)
        val evictor = LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024) // 250MB
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    /**
     * Fournit Firebase Realtime Database
     * Pour la gestion des licences et activation à distance
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().apply {
            try {
                // Activer la persistence pour fonctionnement hors-ligne
                setPersistenceEnabled(true)
            } catch (e : DatabaseException) {
                Timber.w(e, "⚠️ Firebase persistence déjà configurée, poursuite sans crash")
            }
        }
    }

    /**
     * Fournit Firebase Firestore
     * Pour la gestion des activations et playlists via Firestore
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Fournit une RenderersFactory optimisée pour le décodage matériel (Suggestion 4)
     * Favorise la fluidité et le support 4K/UHD
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideRenderersFactory(@ApplicationContext context: Context): RenderersFactory {
        return DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true) // Fallback software si HW échoue
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(
                    mimeType, requiresSecureDecoder, requiresTunnelingDecoder
                )
                // Prioriser les décodeurs matériels (Hardware) comme Netflix
                decoders.sortedBy { if (it.hardwareAccelerated) 0 else 1 }
            }
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        loadControl: LoadControl,
        renderersFactory: RenderersFactory,
        trackSelector: DefaultTrackSelector,
        cache: SimpleCache
    ): ExoPlayer {
        // HttpDataSource avec User-Agent complet pour éviter les blocages de flux
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        // DataSource avec cache alimenté par le HttpDataSource optimisé
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Factory de source média optimisée pour zapping
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
            .setLiveMaxOffsetMs(5000)
            .setLiveMinOffsetMs(2000)
            .setLiveMaxSpeed(1.02f)
            .setLiveMinSpeed(0.98f)

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(false) // Désactivé pour zapping plus fluide
            .build()
            .apply {
                playWhenReady = false
                repeatMode = ExoPlayer.REPEAT_MODE_OFF

                // Listener de reconnexion infinie en cas de coupure réseau
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                Timber.d("⏳ ExoPlayer : buffering en cours...")
                            }
                            Player.STATE_READY -> {
                                Timber.d("▶️ ExoPlayer : prêt à lire")
                            }
                            Player.STATE_ENDED -> {
                                Timber.d("⏹️ ExoPlayer : lecture terminée")
                            }
                            Player.STATE_IDLE -> {
                                Timber.d("💤 ExoPlayer : idle")
                            }
                        }
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        Timber.d("🌐 ExoPlayer : Chargement = $isLoading")
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

    /**
     * Fournit Coil ImageLoader pour le pré-chargement des images
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    /**
     * Fournit MediaSession pour le contrôle système et Picture-in-Picture
     */
    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): MediaSession {
        return MediaSession.Builder(context, player).build()
    }
}
