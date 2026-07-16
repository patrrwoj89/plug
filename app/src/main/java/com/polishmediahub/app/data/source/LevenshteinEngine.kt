package com.polishmediahub.app.data.source

import kotlin.math.min

object LevenshteinEngine {

    const val MAX_DISTANCE_THRESHOLD = 2

    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespace = Regex("\\s+")

    fun calculateDistance(s1: String, s2: String): Int {
        val a = s1.lowercase().trim()
        val b = s2.lowercase().trim()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val (shorter, longer) = if (a.length < b.length) a to b else b to a
        var previous = IntArray(longer.length + 1) { it }
        var current = IntArray(longer.length + 1)

        for (i in 1..shorter.length) {
            current[0] = i
            val c1 = shorter[i - 1]
            for (j in 1..longer.length) {
                val cost = if (c1 == longer[j - 1]) 0 else 1
                current[j] = min(
                    current[j - 1] + 1,
                    min(previous[j] + 1, previous[j - 1] + cost)
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[longer.length]
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
        return input.replace(nonAlphanumeric, " ").replace(whitespace, " ").lowercase().trim()
    }
}
