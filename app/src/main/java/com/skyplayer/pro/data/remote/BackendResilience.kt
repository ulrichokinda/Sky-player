package com.skyplayer.pro.data.remote

import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

/**
 * Circuit Breaker + Retry avec backoff exponentiel pour les appels backend.
 *
 * - **Circuit Breaker** : après [failureThreshold] échecs consécutifs, le circuit
 *   s'ouvre pendant [resetTimeoutMs]. Tous les appels échouent immédiatement
 *   (pas de surcharge du serveur down). Après le timeout, un appel test est lancé.
 * - **Retry** : en mode CLOSED (normal), les échecs réseau sont retentés avec
 *   backoff exponentiel + jitter (1s → 2s → 4s → max 16s, max 3 tentatives).
 *
 * Thread-safe : chaque endpoint a son propre état de circuit.
 */
@Singleton
class BackendResilience @Inject constructor() {

    companion object {
        private const val FAILURE_THRESHOLD = 3        // ouvrir le circuit après 3 échecs
        private const val RESET_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 16_000L
    }

    /** État du circuit pour chaque endpoint */
    private enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

    private data class CircuitInfo(
        var state: CircuitState = CircuitState.CLOSED,
        var failureCount: Int = 0,
        var lastFailureTime: Long = 0L
    )

    private val circuits = ConcurrentHashMap<String, CircuitInfo>()

    /**
     * Exécute un appel réseau avec circuit breaker + retry.
     *
     * @param endpoint nom de l'endpoint (pour le tracking du circuit)
     * @param block le bloc à exécuter (suspend lambda)
     * @return le résultat du bloc, ou lève une [CircuitOpenException] si le circuit est ouvert
     */
    suspend fun <T> execute(endpoint: String, block: suspend () -> T): T {
        val circuit = circuits.getOrPut(endpoint) { CircuitInfo() }

        // Vérifier le circuit
        when (circuit.state) {
            CircuitState.OPEN -> {
                val elapsed = System.currentTimeMillis() - circuit.lastFailureTime
                if (elapsed >= RESET_TIMEOUT_MS) {
                    circuit.state = CircuitState.HALF_OPEN
                    Timber.i("⚡ Circuit [$endpoint]: HALF_OPEN — test en cours")
                } else {
                    val remaining = (RESET_TIMEOUT_MS - elapsed) / 1000
                    Timber.w("🚫 Circuit [$endpoint]: OPEN — réessayer dans ${remaining}s")
                    throw CircuitOpenException(endpoint, remaining)
                }
            }
            CircuitState.HALF_OPEN -> {
                // On laisse passer un seul appel test
            }
            CircuitState.CLOSED -> { /* normal */ }
        }

        // Retry avec backoff exponentiel
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                val result = block()
                // Succès → reset le circuit
                if (circuit.state != CircuitState.CLOSED) {
                    Timber.i("✅ Circuit [$endpoint]: CLOSED — serveur récupéré")
                }
                circuit.state = CircuitState.CLOSED
                circuit.failureCount = 0
                return result
            } catch (e: CircuitOpenException) {
                throw e // Ne pas retry un circuit ouvert
            } catch (e: Exception) {
                lastException = e
                circuit.failureCount++
                circuit.lastFailureTime = System.currentTimeMillis()

                if (circuit.failureCount >= FAILURE_THRESHOLD) {
                    circuit.state = CircuitState.OPEN
                    Timber.e("🔴 Circuit [$endpoint]: OPEN — serveur considéré down après ${circuit.failureCount} échecs")
                    throw CircuitOpenException(endpoint, RESET_TIMEOUT_MS / 1000)
                }

                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = calculateBackoff(attempt)
                    Timber.w("🔄 Circuit [$endpoint]: retry ${attempt + 1}/$MAX_RETRIES dans ${delayMs}ms (${e.message})")
                    delay(delayMs)
                }
            }
        }

        throw lastException ?: Exception("Unknown error")
    }

    /**
     * Backoff exponentiel avec jitter : 1s → 2s → 4s → 8s → 16s (borné).
     */
    private fun calculateBackoff(attempt: Int): Long {
        val exponential = BASE_DELAY_MS * (1L shl attempt) // 1s, 2s, 4s, 8s, 16s
        val jitter = Random.nextLong(0, exponential / 4) // ±25% jitter
        return min(exponential + jitter, MAX_DELAY_MS)
    }

    /** Réinitialise le circuit d'un endpoint (utile après un changement de réseau). */
    fun reset(endpoint: String) {
        circuits.remove(endpoint)
        Timber.d("🔄 Circuit [$endpoint]: reset manuel")
    }

    /** Réinitialise tous les circuits (utile après changement réseau). */
    fun resetAll() {
        circuits.clear()
        Timber.i("🔄 Tous les circuits réseau réinitialisés")
    }

    /** État actuel d'un circuit (pour l'UI / debugging). */
    fun getState(endpoint: String): Pair<String, Int> {
        val circuit = circuits[endpoint] ?: return "CLOSED" to 0
        return circuit.state.name to circuit.failureCount
    }
}

/**
 * Exception levée quand le circuit est ouvert (serveur considéré down).
 */
class CircuitOpenException(
    val endpoint: String,
    val retryAfterSeconds: Long
) : Exception("Circuit ouvert pour $endpoint — réessayer dans ${retryAfterSeconds}s")
