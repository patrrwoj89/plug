package com.polishmediahub.app.data.source

import java.text.Normalizer
import kotlin.math.min

object LevenshteinEngine {

    const val MAX_DISTANCE_THRESHOLD = 2

    private val combiningDiacritics = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespace = Regex("\\s+")

    fun calculateDistance(s1: String, s2: String): Int {
        val a = normalize(s1)
        val b = normalize(s2)
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            val c1 = a[i - 1]
            for (j in 1..b.length) {
                val cost = if (c1 == b[j - 1]) 0 else 1
                current[j] = min(
                    current[j - 1] + 1,
                    min(previous[j] + 1, previous[j - 1] + cost)
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    fun score(query: String, candidate: String): Int {
        val normalizedQuery = normalize(query)
        val normalizedCandidate = normalize(candidate)
        if (normalizedQuery.isEmpty() || normalizedCandidate.isEmpty()) {
            return calculateDistance(normalizedQuery, normalizedCandidate)
        }
        val words = normalizedCandidate.split(whitespace).filter { it.isNotEmpty() }
        var best = calculateDistance(normalizedQuery, normalizedCandidate)
        for (word in words) {
            if (word.length >= normalizedQuery.length / 2) {
                best = min(best, calculateDistance(normalizedQuery, word))
            }
        }
        return best
    }

    fun isFuzzyMatch(query: String, candidate: String, threshold: Int = MAX_DISTANCE_THRESHOLD): Boolean {
        return score(query, candidate) <= threshold
    }

    fun <T> sort(
        query: String,
        candidates: List<T>,
        threshold: Int = MAX_DISTANCE_THRESHOLD,
        selector: (T) -> String
    ): List<T> {
        if (query.isBlank()) return candidates
        val normalizedQuery = normalize(query)
        return candidates
            .mapIndexed { index, item ->
                val text = normalize(selector(item))
                val distance = score(normalizedQuery, text)
                Triple(index, item, distance)
            }
            .filter { it.third <= threshold.coerceAtLeast(normalizedQuery.length) }
            .sortedWith(compareBy({ it.third }, { it.first }))
            .map { it.second }
    }

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(combiningDiacritics, "")
        return decomposed.replace(nonAlphanumeric, " ").replace(whitespace, " ").lowercase().trim()
    }
}
