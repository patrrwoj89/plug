package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem

/**
 * Parental control filter applied to all federated and plugin content.
 *
 * Respects per-profile `maxAgeRating` and `allowNsfw` flags. Unknown ratings
 * are allowed through when no explicit age cap is set; when a cap is set,
 * items with a recognized rating above the cap are removed and adult-flagged
 * items are removed unless the profile explicitly allows NSFW.
 */
object ContentFilter {

    private val RATING_LEVELS = mapOf(
        "G" to 0, "TV-G" to 0, "TV-Y" to 0, "0" to 0, "BO" to 0,
        "PG" to 10, "TV-PG" to 10, "6" to 6, "7" to 7,
        "PG-13" to 13, "TV-14" to 13, "12" to 12,
        "R" to 17, "TV-MA" to 17, "16" to 16,
        "NC-17" to 18, "R18" to 18, "R18+" to 18, "18" to 18,
        "AO" to 18, "X" to 18, "MATURE" to 18, "ADULT" to 18
    )

    private val ADULT_KEYWORDS = setOf("R18", "R18+", "NC-17", "AO", "X", "MATURE", "ADULT", "TV-MA", "18+")

    fun filter(items: List<MediaItem>, profile: ProfileEntity?): List<MediaItem> =
        items.filter { isAllowed(it, profile) }

    fun filterCategories(categories: List<Category>, profile: ProfileEntity?): List<Category> =
        categories.mapNotNull { category ->
            val filtered = category.items.filter { isAllowed(it, profile) }
            if (filtered.isEmpty()) null else category.copy(items = filtered)
        }

    fun isAllowed(item: MediaItem, profile: ProfileEntity?): Boolean {
        if (profile == null) return true

        if (!profile.allowNsfw && item.isAdult) return false

        val ageRating = item.ageRating?.trim()?.uppercase()
        if (!ageRating.isNullOrBlank()) {
            if (!profile.allowNsfw && isAdultRating(ageRating)) return false
            val maxLevel = level(profile.maxAgeRating)
            if (maxLevel != null) {
                val itemLevel = level(ageRating)
                if (itemLevel != null && itemLevel > maxLevel) return false
                if (itemLevel == null && item.isAdult && maxLevel < 18) return false
            }
        } else if (!profile.allowNsfw && item.isAdult) {
            return false
        }

        return true
    }

    private fun isAdultRating(rating: String): Boolean {
        val upper = rating.uppercase()
        if (ADULT_KEYWORDS.any { upper.contains(it) }) return true
        return when (upper) {
            "R", "NC-17", "R18", "R18+", "AO", "X", "MATURE", "ADULT", "TV-MA", "18", "18+" -> true
            else -> false
        }
    }

    private fun level(rating: String?): Int? {
        val normalized = rating?.trim()?.uppercase() ?: return null
        RATING_LEVELS[normalized]?.let { return it }
        // Try to parse a leading age number, e.g. "13+", "16+"
        val match = Regex("""^(\d+)(\+?)""").find(normalized)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
