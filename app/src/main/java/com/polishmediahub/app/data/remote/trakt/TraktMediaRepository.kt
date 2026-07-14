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

    suspend fun watchedMovies(): List<MediaItem> = try {
        val auth = authHeader() ?: return emptyList()
        api.watchedMovies(auth, clientId()).mapNotNull { it.movie?.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun watchlist(): List<MediaItem> = try {
        val auth = authHeader() ?: return emptyList()
        api.watchlist(auth, clientId()).mapNotNull {
            when (it.type) {
                "movie" -> it.movie?.toMediaItem()
                "show" -> it.show?.toMediaItem()
                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun scrobbleStart(mediaItem: MediaItem, progress: Float) {
        val auth = authHeader() ?: return
        val body = buildScrobbleBody(mediaItem, progress)
        api.scrobbleStart(auth, clientId(), body)
    }

    suspend fun scrobblePause(mediaItem: MediaItem, progress: Float) {
        val auth = authHeader() ?: return
        val body = buildScrobbleBody(mediaItem, progress)
        api.scrobblePause(auth, clientId(), body)
    }

    suspend fun scrobbleStop(mediaItem: MediaItem, progress: Float) {
        val auth = authHeader() ?: return
        val body = buildScrobbleBody(mediaItem, progress)
        api.scrobbleStop(auth, clientId(), body)
    }

    private suspend fun authHeader(): String? {
        val token = apiConfigRepository.traktAccessToken.first()
        return if (token.isNotBlank()) "Bearer $token" else null
    }

    private fun buildScrobbleBody(mediaItem: MediaItem, progress: Float): TraktScrobbleBody {
        val year = mediaItem.year.toIntOrNull()
        val ids = TraktIds(trakt = 0, tmdb = mediaItem.id.removePrefix("tmdb:").toIntOrNull())
        return TraktScrobbleBody(
            progress = progress,
            movie = if (mediaItem.type == MediaItem.Type.MOVIE) {
                TraktMovieMinimal(title = mediaItem.title, year = year, ids = ids)
            } else null
        )
    }

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
