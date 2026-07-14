package com.polishmediahub.app.data.remote.trakt

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TraktMediaRepository @Inject constructor(
    private val api: TraktApi,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    private suspend fun clientId(): String = apiConfigRepository.traktClientId.first()

    override suspend fun featured(): List<MediaItem> = try {
        api.popularMovies(clientId()).map { it.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun categories(): List<Category> = try {
        val id = clientId()
        listOf(
            Category(id = "trakt_movies", name = "Trakt Movies", items = api.popularMovies(id).map { it.toMediaItem() }),
            Category(id = "trakt_series", name = "Trakt Shows", items = api.popularSeries(id).map { it.toMediaItem() })
        )
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> = try {
        api.search(clientId(), query = query).mapNotNull { it.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = null

    private fun TraktMovie.toMediaItem() = MediaItem(
        id = "trakt:${ids.trakt}",
        title = title,
        subtitle = "Movie • ${year ?: ""}",
        description = "",
        year = year?.toString().orEmpty(),
        duration = "",
        rating = "",
        genres = emptyList(),
        type = MediaItem.Type.MOVIE
    )

    private fun TraktShow.toMediaItem() = MediaItem(
        id = "trakt:${ids.trakt}",
        title = title,
        subtitle = "Series • ${year ?: ""}",
        description = "",
        year = year?.toString().orEmpty(),
        duration = "",
        rating = "",
        genres = emptyList(),
        type = MediaItem.Type.SERIES
    )

    private fun TraktSearchResult.toMediaItem(): MediaItem? {
        return when (type) {
            "movie" -> movie?.toMediaItem()
            "show" -> show?.toMediaItem()
            else -> null
        }
    }
}
