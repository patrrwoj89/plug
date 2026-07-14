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

    suspend fun searchPaged(query: String, page: Int): List<MediaItem> = try {
        api.search(apiKey(), query, page).results.mapNotNull { it.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = try {
        val numericId = id.removePrefix("tmdb:").toIntOrNull() ?: return null
        val key = apiKey()
        val item = try {
            api.movieDetails(numericId, key).toMediaItem()
        } catch (_: Exception) {
            api.seriesDetails(numericId, key).toMediaItem()
        }
        enrichImages(item, numericId, key)
    } catch (_: Exception) {
        null
    }

    suspend fun recommendations(mediaItem: MediaItem): List<MediaItem> = try {
        val numericId = mediaItem.id.removePrefix("tmdb:").toIntOrNull() ?: return emptyList()
        val key = apiKey()
        if (mediaItem.type == MediaItem.Type.MOVIE) {
            api.movieRecommendations(numericId, key).results.map { it.toMediaItem() }
        } else {
            api.seriesRecommendations(numericId, key).results.map { it.toMediaItem() }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private suspend fun genreMap(): Map<Int, String> {
        return try {
            val key = apiKey()
            val movieGenres = api.movieGenres(key).genres
            val tvGenres = api.tvGenres(key).genres
            (movieGenres + tvGenres).associateBy({ it.id }, { it.name })
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun enrichImages(item: MediaItem, numericId: Int, key: String): MediaItem {
        return try {
            val images = if (item.type == MediaItem.Type.MOVIE) api.movieImages(numericId, key) else api.seriesImages(numericId, key)
            val bestBackdrop = images.backdrops.firstOrNull()?.filePath
            val bestPoster = images.posters.firstOrNull()?.filePath
            item.copy(
                backdropUrl = bestBackdrop?.let { "$BACKDROP_BASE$it" } ?: item.backdropUrl,
                posterUrl = bestPoster?.let { "$IMAGE_BASE$it" } ?: item.posterUrl
            )
        } catch (_: Exception) {
            item
        }
    }

    private companion object {
        private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
        private const val BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"
    }

    private suspend fun TmdbMovie.toMediaItem(): MediaItem {
        val genres = genreIds.mapNotNull { genreMap()[it] }
        return MediaItem(
            id = "tmdb:$id",
            title = title,
            subtitle = "Movie • ${releaseDate?.take(4).orEmpty()}",
            description = overview,
            posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
            backdropUrl = backdropPath?.let { "$BACKDROP_BASE$it" },
            year = releaseDate?.take(4).orEmpty(),
            duration = "",
            rating = voteAverage?.toString() ?: "",
            genres = genres,
            type = MediaItem.Type.MOVIE
        )
    }

    private suspend fun TmdbSeries.toMediaItem(): MediaItem {
        val genres = genreIds.mapNotNull { genreMap()[it] }
        return MediaItem(
            id = "tmdb:$id",
            title = name,
            subtitle = "Series • ${firstAirDate?.take(4).orEmpty()}",
            description = overview,
            posterUrl = posterPath?.let { "$IMAGE_BASE$it" },
            backdropUrl = backdropPath?.let { "$BACKDROP_BASE$it" },
            year = firstAirDate?.take(4).orEmpty(),
            duration = "",
            rating = voteAverage?.toString() ?: "",
            genres = genres,
            type = MediaItem.Type.SERIES
        )
    }

    private suspend fun TmdbSearchResult.toMediaItem(): MediaItem? {
        val isMovie = mediaType == "movie"
        val title = title ?: name ?: return null
        val genreNames = (if (isMovie) genreIds else emptyList()).mapNotNull { genreMap()[it] }
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
            genres = genreNames,
            type = if (isMovie) MediaItem.Type.MOVIE else MediaItem.Type.SERIES
        )
    }
}
