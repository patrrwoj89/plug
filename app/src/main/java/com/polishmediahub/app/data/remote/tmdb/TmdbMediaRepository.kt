package com.polishmediahub.app.data.remote.tmdb

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TmdbMediaRepository @Inject constructor(
    private val api: TmdbApi,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    private suspend fun apiKey(): String = apiConfigRepository.tmdbApiKey.first()

    override suspend fun featured(): List<MediaItem> = try {
        api.trendingMovies(apiKey()).results.map { it.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun categories(): List<Category> = try {
        val key = apiKey()
        listOf(
            Category(id = "tmdb_movies", name = "Popular Movies", items = api.popularMovies(key).results.map { it.toMediaItem() }),
            Category(id = "tmdb_series", name = "Popular Series", items = api.popularSeries(key).results.map { it.toMediaItem() })
        )
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> = try {
        api.search(apiKey(), query).results.mapNotNull { it.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = try {
        val numericId = id.toIntOrNull() ?: return null
        api.movieDetails(numericId, apiKey()).toMediaItem()
    } catch (_: Exception) {
        null
    }

    private companion object {
        private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
        private const val BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"
    }

    private fun TmdbMovie.toMediaItem() = MediaItem(
        id = "tmdb:$id",
        title = title,
        subtitle = "Movie • ${releaseDate?.take(4).orEmpty()}",
        description = overview,
        posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
        backdropUrl = backdropPath?.let { "$BACKDROP_BASE$it" },
        year = releaseDate?.take(4).orEmpty(),
        duration = "",
        rating = voteAverage?.toString() ?: "",
        genres = emptyList(),
        type = MediaItem.Type.MOVIE
    )

    private fun TmdbSeries.toMediaItem() = MediaItem(
        id = "tmdb:$id",
        title = name,
        subtitle = "Series • ${firstAirDate?.take(4).orEmpty()}",
        description = overview,
        posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
        backdropUrl = backdropPath?.let { "$BACKDROP_BASE$it" },
        year = firstAirDate?.take(4).orEmpty(),
        duration = "",
        rating = voteAverage?.toString() ?: "",
        genres = emptyList(),
        type = MediaItem.Type.SERIES
    )

    private fun TmdbSearchResult.toMediaItem(): MediaItem? {
        val isMovie = mediaType == "movie"
        val title = title ?: name ?: return null
        return MediaItem(
            id = "tmdb:$id",
            title = title,
            subtitle = if (isMovie) "Movie" else "Series",
            description = overview.orEmpty(),
            posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
            backdropUrl = backdropPath?.let { "$BACKDROP_BASE$it" },
            year = (releaseDate ?: firstAirDate)?.take(4).orEmpty(),
            duration = "",
            rating = voteAverage?.toString() ?: "",
            genres = emptyList(),
            type = if (isMovie) MediaItem.Type.MOVIE else MediaItem.Type.SERIES
        )
    }
}
