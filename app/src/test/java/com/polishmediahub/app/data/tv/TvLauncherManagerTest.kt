package com.polishmediahub.app.data.tv

import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.data.local.WatchedEntity
import com.polishmediahub.app.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLauncherManagerTest {

    private val adultProfile = ProfileEntity(
        id = "adult",
        name = "Adult",
        maxAgeRating = null,
        allowNsfw = true
    )

    private val childProfile = ProfileEntity(
        id = "child",
        name = "Child",
        maxAgeRating = "7",
        allowNsfw = false
    )

    @Test
    fun `isUnfinished returns false for unstarted or finished items`() {
        assertFalse(isUnfinished(positionMs = 0, durationMs = 200_000))
        assertFalse(isUnfinished(positionMs = 195_000, durationMs = 200_000))
        assertFalse(isUnfinished(positionMs = 200_000, durationMs = 200_000))
    }

    @Test
    fun `isUnfinished returns true for in-progress items`() {
        assertTrue(isUnfinished(positionMs = 100_000, durationMs = 200_000))
        assertTrue(isUnfinished(positionMs = 180_000, durationMs = 200_000))
        assertTrue(isUnfinished(positionMs = 5_000, durationMs = 0))
    }

    @Test
    fun `buildWatchNextItems keeps only unfinished and age-allowed items for child`() {
        val history = listOf(
            item(id = "g", ageRating = "G") to watched(positionMs = 100_000, durationMs = 200_000),
            item(id = "pg13", ageRating = "PG-13") to watched(positionMs = 50_000, durationMs = 200_000),
            item(id = "finished", ageRating = "G") to watched(positionMs = 195_000, durationMs = 200_000),
            item(id = "adult", ageRating = "R", isAdult = true) to watched(positionMs = 100_000, durationMs = 200_000),
            item(id = "unknown", ageRating = null) to watched(positionMs = 100_000, durationMs = 200_000)
        )

        val result = buildWatchNextItems(childProfile, history)

        assertEquals(1, result.size)
        assertEquals("g", result.first().item.id)
        assertEquals(WatchNextKind.CONTINUE, result.first().kind)
    }

    @Test
    fun `buildWatchNextItems allows all finished and unfinished items for adult profile`() {
        val history = listOf(
            item(id = "adult", ageRating = "R", isAdult = true) to watched(positionMs = 100_000, durationMs = 200_000),
            item(id = "finished", ageRating = "G") to watched(positionMs = 195_000, durationMs = 200_000)
        )

        val result = buildWatchNextItems(adultProfile, history)

        assertEquals(1, result.size)
        assertEquals("adult", result.first().item.id)
    }

    @Test
    fun `buildWatchlistItems filters adult and age-inappropriate items for child`() {
        val watchlist = listOf(
            item(id = "g", ageRating = "G"),
            item(id = "r18", ageRating = "R18", isAdult = true),
            item(id = "pg13", ageRating = "PG-13"),
            item(id = "unknown", ageRating = null)
        )

        val result = buildWatchlistItems(childProfile, watchlist)

        assertEquals(1, result.size)
        assertEquals("g", result.first().id)
    }

    @Test
    fun `buildWatchlistItems allows all items for adult profile`() {
        val watchlist = listOf(
            item(id = "g", ageRating = "G"),
            item(id = "r18", ageRating = "R18", isAdult = true)
        )

        val result = buildWatchlistItems(adultProfile, watchlist)

        assertEquals(2, result.size)
    }

    @Test
    fun `buildPreviewItems filters to movies series episodes and applies content filter`() {
        val featured = listOf(
            MediaItem(id = "movie", title = "Movie", type = MediaItem.Type.MOVIE, ageRating = "G"),
            MediaItem(id = "series", title = "Series", type = MediaItem.Type.SERIES, ageRating = "G"),
            MediaItem(id = "channel", title = "Channel", type = MediaItem.Type.CHANNEL, ageRating = "G"),
            MediaItem(id = "adult", title = "Adult", type = MediaItem.Type.MOVIE, ageRating = "R18", isAdult = true)
        )

        val result = buildPreviewItems(childProfile, featured)

        assertEquals(2, result.size)
        assertTrue(result.none { it.id == "channel" })
        assertTrue(result.none { it.id == "adult" })
        assertTrue(result.any { it.id == "movie" })
        assertTrue(result.any { it.id == "series" })
    }

    @Test
    fun `buildPreviewItems takes at most MAX_RECOMMENDATIONS`() {
        val featured = (1..30).map { index ->
            MediaItem(id = "movie$index", title = "Movie $index", type = MediaItem.Type.MOVIE, ageRating = "G")
        }

        val result = buildPreviewItems(adultProfile, featured)

        assertEquals(20, result.size)
    }

    private fun item(
        id: String,
        ageRating: String? = null,
        isAdult: Boolean = false
    ): MediaItem = MediaItem(
        id = id,
        title = "Title $id",
        ageRating = ageRating,
        isAdult = isAdult
    )

    private fun watched(
        positionMs: Long = 0,
        durationMs: Long = 0,
        watchedAt: Long = System.currentTimeMillis()
    ): WatchedEntity = WatchedEntity(
        profileId = "profile",
        id = "id",
        positionMs = positionMs,
        durationMs = durationMs,
        watchedAt = watchedAt
    )
}
