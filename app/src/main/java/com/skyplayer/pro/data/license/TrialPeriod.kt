package com.skyplayer.pro.data.license

/**
 * Calcul de la période d'essai — logique pure et testable.
 *
 * Source unique de la durée d'essai (14 jours), alignée sur :
 * - `TRIAL_DAYS` dans `config.php` / `config.example.php` (backend PHP)
 * - `TRIAL_DAYS` dans `server.js` (activation-service)
 *
 * Toutes les fonctions sont pures (aucune dépendance Android), donc unit-testables.
 */
object TrialPeriod {
    /** Durée d'essai en jours — source unique pour toute l'application */
    const val TRIAL_DAYS = 14

    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    /** Fin de la période d'essai pour une date d'installation donnée */
    fun trialEndDate(installDate: Long): Long = installDate + (TRIAL_DAYS * DAY_IN_MILLIS)

    /** Vrai si [now] est strictement avant la fin de l'essai */
    fun isValid(installDate: Long, now: Long): Boolean = now < trialEndDate(installDate)

    /** Jours pleins restants (borné à 0), arrondis à l'inférieur */
    fun daysRemaining(installDate: Long, now: Long): Int {
        val end = trialEndDate(installDate)
        return ((end - now) / DAY_IN_MILLIS).coerceAtLeast(0).toInt()
    }
}
