package com.polishmediahub.app.data.remote.trakt

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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
        fetchAllWatchedMovies(auth, clientId()).mapNotNull { it.movie?.toMediaItem() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun watchlist(): List<MediaItem> = try {
        val auth = authHeader() ?: return emptyList()
        fetchAllWatchlist(auth, clientId()).mapNotNull {
            when (it.type) {
                "movie" -> it.movie?.toMediaItem()
                "show" -> it.show?.toMediaItem()
                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Returns all watched movies and episodes together with the Unix timestamp (ms)
     * of the last watch reported by Trakt. Used by [TraktSyncWorker] for conflict
     * resolution against local Room history.
     */
    suspend fun watchedItemsWithTimestamps(): List<Pair<MediaItem, Long>> = try {
        val auth = authHeader() ?: return emptyList()
        val client = clientId()
        val movies = fetchAllWatchedMovies(auth, client).mapNotNull { watched ->
            val item = watched.movie?.toMediaItem() ?: return@mapNotNull null
            val ts = parseIsoTimestamp(watched.lastWatchedAt)
            item to ts
        }
        val shows = fetchAllWatchedShows(auth, client).flatMap { watchedShow ->
            val show = watchedShow.show
            watchedShow.seasons.flatMap { season ->
                season.episodes.map { episode ->
                    val episodeTitle = "${show.title} S${season.number}E${episode.number}"
                    val item = MediaItem(
                        id = "trakt:show:${show.ids.trakt}:s${season.number}:e${episode.number}",
                        title = episodeTitle,
                        subtitle = show.title,
                        year = show.year?.toString().orEmpty(),
                        type = MediaItem.Type.EPISODE,
                        season = season.number,
                        episode = episode.number,
                        tmdbId = show.ids.tmdb,
                        traktId = episode.ids?.trakt ?: show.ids.trakt,
                        imdbId = show.ids.imdb
                    )
                    val ts = parseIsoTimestamp(episode.lastWatchedAt)
                    item to ts
                }
            }
        }
        movies + shows
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w(TAG, "watchedItemsWithTimestamps failed: ${e.message}", e)
        emptyList()
    }

    /**
     * Returns watchlist items together with the Unix timestamp (ms) of when they
     * were added to Trakt.
     */
    suspend fun watchlistWithTimestamps(): List<Pair<MediaItem, Long>> = try {
        val auth = authHeader() ?: return emptyList()
        fetchAllWatchlist(auth, clientId()).mapNotNull { item ->
            val media = when (item.type) {
                "movie" -> item.movie?.toMediaItem()
                "show" -> item.show?.toMediaItem()?.copy(type = MediaItem.Type.SERIES)
                else -> null
            } ?: return@mapNotNull null
            media to parseIsoTimestamp(item.listedAt)
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w(TAG, "watchlistWithTimestamps failed: ${e.message}", e)
        emptyList()
    }

    /**
     * Uploads local watched items to Trakt's /sync/history.
     * Each pair contains the item and the local Unix timestamp (ms) of the watch.
     */
    suspend fun syncWatchedToTrakt(items: List<Pair<MediaItem, Long>>) {
        val auth = authHeader() ?: return
        val movies = mutableListOf<TraktSyncMovie>()
        val shows = mutableListOf<TraktSyncShow>()
        val episodes = mutableListOf<TraktSyncEpisode>()

        items.forEach { (item, watchedAtMs) ->
            val ids = item.toTraktIds() ?: return@forEach
            val stamp = formatIsoTimestamp(watchedAtMs)
            when (item.type) {
                MediaItem.Type.MOVIE -> movies.add(
                    TraktSyncMovie(title = item.title, year = item.year.toIntOrNull(), ids = ids, watchedAt = stamp)
                )
                MediaItem.Type.EPISODE -> {
                    val season = item.season ?: 1
                    val episode = item.episode ?: 1
                    episodes.add(
                        TraktSyncEpisode(
                            season = season,
                            number = episode,
                            title = item.title,
                            ids = ids,
                            watchedAt = stamp
                        )
                    )
                }
                MediaItem.Type.SERIES -> shows.add(
                    TraktSyncShow(title = item.title, year = item.year.toIntOrNull(), ids = ids, watchedAt = stamp)
                )
                else -> { /* ignored */ }
            }
        }

        if (movies.isEmpty() && shows.isEmpty() && episodes.isEmpty()) return
        api.syncHistoryAdd(auth, clientId(), TraktSyncHistoryBody(movies, shows, episodes))
    }

    /**
     * Uploads local watchlist items to Trakt's /sync/watchlist.
     */
    suspend fun syncWatchlistToTrakt(items: List<MediaItem>) {
        val auth = authHeader() ?: return
        val movies = mutableListOf<TraktSyncMovie>()
        val shows = mutableListOf<TraktSyncShow>()

        items.forEach { item ->
            val ids = item.toTraktIds() ?: return@forEach
            when (item.type) {
                MediaItem.Type.MOVIE -> movies.add(
                    TraktSyncMovie(title = item.title, year = item.year.toIntOrNull(), ids = ids)
                )
                MediaItem.Type.SERIES, MediaItem.Type.EPISODE -> shows.add(
                    TraktSyncShow(title = item.title, year = item.year.toIntOrNull(), ids = ids)
                )
                else -> { /* ignored */ }
            }
        }

        if (movies.isEmpty() && shows.isEmpty()) return
        api.syncWatchlistAdd(auth, clientId(), TraktSyncHistoryBody(movies, shows))
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

    private tailrec suspend fun fetchAllWatchedMovies(
        auth: String,
        clientId: String,
        page: Int = 1,
        accumulator: List<TraktWatchedItem> = emptyList()
    ): List<TraktWatchedItem> {
        val response = api.watchedMovies(auth, clientId, page = page, limit = 250)
        val pageCount = response.paginationPageCount()
        val body = response.body() ?: emptyList()
        val merged = accumulator + body
        return if (body.isEmpty() || page >= (pageCount ?: page)) {
            merged
        } else {
            fetchAllWatchedMovies(auth, clientId, page + 1, merged)
        }
    }

    private tailrec suspend fun fetchAllWatchedShows(
        auth: String,
        clientId: String,
        page: Int = 1,
        accumulator: List<TraktWatchedShow> = emptyList()
    ): List<TraktWatchedShow> {
        val response = api.watchedShows(auth, clientId, page = page, limit = 250)
        val pageCount = response.paginationPageCount()
        val body = response.body() ?: emptyList()
        val merged = accumulator + body
        return if (body.isEmpty() || page >= (pageCount ?: page)) {
            merged
        } else {
            fetchAllWatchedShows(auth, clientId, page + 1, merged)
        }
    }

    private tailrec suspend fun fetchAllWatchlist(
        auth: String,
        clientId: String,
        page: Int = 1,
        accumulator: List<TraktWatchlistItem> = emptyList()
    ): List<TraktWatchlistItem> {
        val response = api.watchlist(auth, clientId, page = page, limit = 250)
        val pageCount = response.paginationPageCount()
        val body = response.body() ?: emptyList()
        val merged = accumulator + body
        return if (body.isEmpty() || page >= (pageCount ?: page)) {
            merged
        } else {
            fetchAllWatchlist(auth, clientId, page + 1, merged)
        }
    }

    private fun <T> Response<List<T>>.paginationPageCount(): Int? {
        val raw = headers()["X-Pagination-Page-Count"]
            ?: headers()["x-pagination-page-count"]
        return raw?.toIntOrNull()
    }

    private fun buildScrobbleBody(mediaItem: MediaItem, progress: Float): TraktScrobbleBody {
        val year = mediaItem.year.toIntOrNull()
        val ids = mediaItem.toTraktIds() ?: TraktIds(trakt = 0)

        val movie = if (mediaItem.type == MediaItem.Type.MOVIE) {
            TraktMovieMinimal(title = mediaItem.title, year = year, ids = ids)
        } else null

        val (show, episode) = if (mediaItem.type == MediaItem.Type.EPISODE || (mediaItem.season != null && mediaItem.episode != null)) {
            val showMinimal = TraktShowMinimal(title = mediaItem.title, year = year, ids = ids)
            val episodeMinimal = TraktEpisodeMinimal(
                season = mediaItem.season ?: 1,
                number = mediaItem.episode ?: 1,
                title = mediaItem.title,
                ids = ids
            )
            showMinimal to episodeMinimal
        } else null to null

        return TraktScrobbleBody(
            progress = progress,
            movie = movie,
            show = show,
            episode = episode
        )
    }

    private fun MediaItem.toTraktIds(): TraktIds? {
        val tmdb = tmdbId
        val trakt = traktId
        val imdb = imdbId
        if (tmdb == null && trakt == null && imdb.isNullOrBlank()) return null
        return TraktIds(
            trakt = trakt ?: 0,
            tmdb = tmdb,
            imdb = imdb
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
        type = MediaItem.Type.MOVIE,
        tmdbId = ids.tmdb,
        traktId = ids.trakt,
        imdbId = ids.imdb
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
        type = MediaItem.Type.SERIES,
        tmdbId = ids.tmdb,
        traktId = ids.trakt,
        imdbId = ids.imdb
    )

    private fun TraktSearchResult.toMediaItem(): MediaItem? {
        return when (type) {
            "movie" -> movie?.toMediaItem()
            "show" -> show?.toMediaItem()
            else -> null
        }
    }

    companion object {
        private const val TAG = "TraktMediaRepository"

        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val ISO_DATE_FORMAT_NO_MS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun parseIsoTimestamp(iso: String?): Long {
            if (iso.isNullOrBlank()) return System.currentTimeMillis()
            return try {
                ISO_DATE_FORMAT.parse(iso)?.time
                    ?: ISO_DATE_FORMAT_NO_MS.parse(iso)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        fun formatIsoTimestamp(timestampMs: Long): String {
            return ISO_DATE_FORMAT.format(java.util.Date(timestampMs))
        }
    }
}
