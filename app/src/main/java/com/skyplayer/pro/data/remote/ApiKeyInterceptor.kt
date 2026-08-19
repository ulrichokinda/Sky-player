package com.skyplayer.pro.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor OkHttp qui injecte la clé API sur chaque requête backend.
 *
 * Utilise [BackendConfig] (Firebase Remote Config) au lieu de BuildConfig,
 * ce qui permet la rotation de clé sans mise à jour de l'app.
 *
 * Le header X-Activation-API-Key est ajouté UNIQUEMENT si absent de la requête
 * (permet de l'overrider par endpoint si nécessaire).
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val backendConfig: BackendConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Ne pas ajouter la clé si déjà présente (override par endpoint)
        if (original.header(HEADER_API_KEY) != null) {
            return chain.proceed(original)
        }

        val request = original.newBuilder()
            .header(HEADER_API_KEY, backendConfig.apiKey)
            .build()

        return chain.proceed(request)
    }

    companion object {
        const val HEADER_API_KEY = "X-Activation-API-Key"
    }
}
