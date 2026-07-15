package com.polishmediahub.app.data.local

import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilmwebCacheRepository @Inject constructor(
    private val dao: FilmwebCacheDao
) {

    suspend fun get(title: String, year: String): MediaItem? = withContext(Dispatchers.IO) {
        dao.get(cacheKey(title, year))?.toMediaItem()
    }

    suspend fun save(item: MediaItem) = withContext(Dispatchers.IO) {
        val key = cacheKey(item.title, item.year)
        dao.insert(
            FilmwebCacheEntity(
                cacheKey = key,
                title = item.title,
                year = item.year,
                description = item.description,
                posterUrl = item.posterUrl,
                rating = item.filmwebRating,
                voteCount = item.filmwebVoteCount,
                filmwebUrl = item.filmwebUrl,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clean(maxAgeMs: Long = SEVEN_DAYS_MS) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(System.currentTimeMillis() - maxAgeMs)
    }

    private fun FilmwebCacheEntity.toMediaItem(): MediaItem = MediaItem(
        id = "filmweb:$cacheKey",
        title = title,
        description = description,
        posterUrl = posterUrl,
        year = year,
        filmwebRating = rating,
        filmwebVoteCount = voteCount,
        filmwebUrl = filmwebUrl
    )

    private fun cacheKey(title: String, year: String): String =
        "${title.lowercase().trim()}:${year.trim()}"

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
