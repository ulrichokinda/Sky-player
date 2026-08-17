package com.skyplayer.pro.di

import android.content.Context
import com.skyplayer.pro.BuildConfig
import com.skyplayer.pro.data.parser.M3UParser
import com.skyplayer.pro.data.remote.SkyPlayerBackendApi
import com.skyplayer.pro.data.remote.XtreamCodesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Module Dagger Hilt pour la gestion des dépendances réseau
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").v(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        // Cache disque HTTP : les playlists M3U/EPG ne sont re-téléchargées en entier
        // que si le serveur le demande (ETag/Last-Modified → réponse 304). Inerte pour
        // les réponses sans en-têtes de validation (jamais servies depuis le cache).
        val httpCache = Cache(
            File(context.cacheDir, "http_cache"),
            50L * 1024 * 1024 // 50 Mo
        )

        return OkHttpClient.Builder()
            // Timeouts ajustés pour stabilité (évite chargements infinis)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            // Pool élargi pour IPTV (zapping fréquent sur différents serveurs)
            .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
            .pingInterval(20, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    // User-Agent standard requis par la plupart des serveurs IPTV
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                    .method(original.method, original.body)
                    .build()

                Timber.tag("OkHttp").d("Request: ${request.method} ${request.url}")
                val response = chain.proceed(request)
                Timber.tag("OkHttp").d("Response: ${response.code} for ${request.url}")
                response
            }
            .cache(httpCache)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Fournit le Retrofit pour le backend skyplayerapp.xyz
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val baseUrl = SkyPlayerBackendApi.BASE_URL
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl.ifEmpty { "https://localhost/" })
            .build()
    }

    /**
     * Fournit le client unique du backend Sky-player (devices/check, playlist, mac/check)
     */
    @Provides
    @Singleton
    fun provideSkyPlayerBackendApi(retrofit: Retrofit): SkyPlayerBackendApi {
        return retrofit.create(SkyPlayerBackendApi::class.java)
    }

    /**
     * Fournit l'API Xtream Codes
     * Utilise un Retrofit séparé car la base URL est dynamique (passée via @Url)
     */
    @Provides
    @Singleton
    fun provideXtreamCodesApi(okHttpClient: OkHttpClient): XtreamCodesApi {
        // Base URL factice car on utilise @Url pour des URLs dynamiques
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://xtream-codes.api/")
            .build()
            .create(XtreamCodesApi::class.java)
    }

    /**
     * Fournit le parser M3U avec OkHttpClient injecté
     */
    @Provides
    @Singleton
    fun provideM3UParser(okHttpClient: OkHttpClient): M3UParser {
        return M3UParser(okHttpClient)
    }
}
