package com.polishmediahub.app.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class LevenshteinEngineTest {

    @Test
    fun `calculates exact distance for known typos`() {
        assertEquals(1, LevenshteinEngine.calculateDistance("Wiedźmin", "Widźmin"))
        assertEquals(1, LevenshteinEngine.calculateDistance("Filman", "Flman"))
        assertTrue(LevenshteinEngine.calculateDistance("Batman", "Spiderman") > 2)
    }

    @Test
    fun `score picks closest word inside longer title`() {
        val score = LevenshteinEngine.score("Wiedźmin", "Wiedźmin 3 Dziki Gon 1080p")
        assertEquals(0, score)
    }

    @Test
    fun `sort returns best fuzzy matches first`() {
        val candidates = listOf(
            "The Wiedźmin 2",
            "Widźmin Season 1",
            "Spiderman Far From Home",
            "Wiedźmin 3: Wild Hunt"
        )
        val sorted = LevenshteinEngine.sort("Wiedźmin", candidates) { it }
        assertEquals(
            listOf(
                "The Wiedźmin 2",
                "Wiedźmin 3: Wild Hunt",
                "Widźmin Season 1",
                "Spiderman Far From Home"
            ),
            sorted
        )
    }

    @Test
    fun `isFuzzyMatch respects threshold`() {
        assertTrue(LevenshteinEngine.isFuzzyMatch("Wiedźmin", "Widźmin"))
        assertFalse(LevenshteinEngine.isFuzzyMatch("Wiedźmin", "Batman"))
    }
}
