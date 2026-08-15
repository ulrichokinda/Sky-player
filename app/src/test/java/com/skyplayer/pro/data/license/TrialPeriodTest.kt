package com.skyplayer.pro.data.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialPeriodTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_700_000_000_000L // horodatage fixe pour des tests déterministes

    @Test
    fun isValid_returnsTrueWithinTrial() {
        val installDate = now - 10 * day
        assertTrue(TrialPeriod.isValid(installDate, now))
    }

    @Test
    fun isValid_returnsFalseAfterTrial() {
        val installDate = now - 15 * day
        assertFalse(TrialPeriod.isValid(installDate, now))
    }

    @Test
    fun isValid_boundaryAtExactlyTrialDaysIsExpired() {
        val installDate = now - TrialPeriod.TRIAL_DAYS * day
        assertFalse(TrialPeriod.isValid(installDate, now))
    }

    @Test
    fun daysRemaining_countsFullDays() {
        val installDate = now - 3 * day
        assertEquals(TrialPeriod.TRIAL_DAYS - 3, TrialPeriod.daysRemaining(installDate, now))
    }

    @Test
    fun daysRemaining_partialDayRoundsDown() {
        // 3,5 jours écoulés → 10,5 jours restants → arrondi à 10
        val installDate = now - (3 * day + 12 * 60 * 60 * 1000L)
        assertEquals(TrialPeriod.TRIAL_DAYS - 4, TrialPeriod.daysRemaining(installDate, now))
    }

    @Test
    fun daysRemaining_isNeverNegative() {
        val installDate = now - 30 * day
        assertEquals(0, TrialPeriod.daysRemaining(installDate, now))
    }

    @Test
    fun trialEndDate_usesTrialDaysConstant() {
        val installDate = now - day
        assertEquals(installDate + TrialPeriod.TRIAL_DAYS * day, TrialPeriod.trialEndDate(installDate))
    }

    @Test
    fun trialDays_matchesLicenseManager() {
        assertEquals(LicenseManager.TRIAL_DAYS, TrialPeriod.TRIAL_DAYS)
    }
}
