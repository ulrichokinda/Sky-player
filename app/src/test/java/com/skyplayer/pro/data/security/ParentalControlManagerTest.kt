package com.skyplayer.pro.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlManagerTest {

    // ── Catégories sensibles détectées ───────────────────────────────────────
    @Test
    fun sensitiveCategory_adultVariants() {
        assertTrue(ParentalControlManager.isSensitiveCategoryName("ADULT"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("Adult 18"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("Contenu Adulte"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("XXX"))
    }

    @Test
    fun sensitiveCategory_ageMarkersBothWays() {
        assertTrue(ParentalControlManager.isSensitiveCategoryName("18+"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("+18"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("18 ANS"))
    }

    @Test
    fun sensitiveCategory_commonAdultBrands() {
        assertTrue(ParentalControlManager.isSensitiveCategoryName("PLAYBOY TV"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("PENTHOUSE"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("DORCEL"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("SEX"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("MATURE"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("EROTIQUE"))
    }

    @Test
    fun sensitiveCategory_caseInsensitive() {
        assertTrue(ParentalControlManager.isSensitiveCategoryName("playboy"))
        assertTrue(ParentalControlManager.isSensitiveCategoryName("AdUlTe"))
    }

    // ── Catégories normales NON sensibles ────────────────────────────────────
    @Test
    fun normalCategory_notSensitive() {
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Films"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Séries"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Sports"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Actualités"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Enfants"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Musique"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Documentaires"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("Généralistes"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName("France"))
    }

    @Test
    fun similarButHarmlessNames_notSensitive() {
        // "HOT" ne doit pas faussement déclencher sur des noms sans rapport
        assertFalse(ParentalControlManager.isSensitiveCategoryName("HOT NEWS"))
        assertFalse(ParentalControlManager.isSensitiveCategoryName(""))
    }
}
