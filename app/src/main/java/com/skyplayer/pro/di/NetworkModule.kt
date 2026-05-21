package com.skyplayer.pro.di

import com.skyplayer.pro.BuildConfig
import com.skyplayer.pro.data.parser.M3UParser
import com.skyplayer.pro.data.remote.LicenseApiService
import com.skyplayer.pro.data.remote.XtreamCodesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Module Hilt pour la configuration réseau
 * Optimisé pour les connexions instables avec timeouts élevés
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Fournit le client HTTP OkHttp avec configuration optimisée
     * Timeouts élevés pour réseaux lents (Edge/3G/4G instable)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        
        return OkHttpClient.Builder()
            // Timeouts élevés pour réseaux africains
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Retry automatique
            .retryOnConnectionFailure(true)
            // Intercepteurs
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * Fournit le Retrofit pour le backend skyplayerapp.xyz
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(LicenseApiService.BASE_URL)
            .build()
    }

    /**
     * Fournit le service API pour la gestion des licences
     */
    @Provides
    @Singleton
    fun provideLicenseApiService(retrofit: Retrofit): LicenseApiService {
        return retrofit.create(LicenseApiService::class.java)
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
