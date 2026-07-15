package com.polishmediahub.app.data.remote.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TraktApi {

    @GET("movies/popular")
    suspend fun popularMovies(
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") version: String = "2",
        @Query("limit") limit: Int = 20
    ): List<TraktMovie>

    @GET("shows/popular")
    suspend fun popularSeries(
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") version: String = "2",
        @Query("limit") limit: Int = 20
    ): List<TraktShow>

    @GET("search/movie,show")
    suspend fun search(
        @Header("trakt-api-key") clientId: String,
        @Header("trakt-api-version") version: String = "2",
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<TraktSearchResult>

    @GET("sync/watched/movies")
    suspend fun watchedMovies(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 250
    ): Response<List<TraktWatchedItem>>

    @GET("sync/watched/shows")
    suspend fun watchedShows(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 250
    ): Response<List<TraktWatchedShow>>

    @GET("sync/watchlist/movies,shows")
    suspend fun watchlist(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 250
    ): Response<List<TraktWatchlistItem>>

    @POST("sync/history")
    suspend fun syncHistoryAdd(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktSyncHistoryBody
    )

    @POST("sync/history/remove")
    suspend fun syncHistoryRemove(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktSyncHistoryBody
    )

    @POST("sync/watchlist")
    suspend fun syncWatchlistAdd(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktSyncHistoryBody
    )

    @POST("sync/watchlist/remove")
    suspend fun syncWatchlistRemove(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktSyncHistoryBody
    )

    @POST("scrobble/start")
    suspend fun scrobbleStart(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    )

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    )

    @POST("scrobble/stop")
    suspend fun scrobbleStop(
        @Header("Authorization") auth: String,
        @Header("trakt-api-key") clientId: String,
        @Body body: TraktScrobbleBody
    )

    companion object {
        const val BASE_URL = "https://api.trakt.tv/"
    }
}

@Serializable
data class TraktMovie(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktShow(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktSearchResult(
    val type: String,
    val score: Double? = null,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null
)

@Serializable
data class TraktIds(
    val trakt: Int,
    val slug: String? = null,
    val imdb: String? = null,
    val tmdb: Int? = null
)

@Serializable
data class TraktWatchedItem(
    @SerialName("last_watched_at") val lastWatchedAt: String? = null,
    val movie: TraktMovie? = null
)

@Serializable
data class TraktWatchedShow(
    val show: TraktShow,
    val seasons: List<TraktWatchedSeason> = emptyList()
)

@Serializable
data class TraktWatchedSeason(
    val number: Int,
    val episodes: List<TraktWatchedEpisode> = emptyList()
)

@Serializable
data class TraktWatchedEpisode(
    val number: Int,
    val ids: TraktIds? = null,
    @SerialName("last_watched_at") val lastWatchedAt: String? = null
)

@Serializable
data class TraktWatchlistItem(
    val type: String,
    @SerialName("listed_at") val listedAt: String? = null,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null
)

@Serializable
data class TraktScrobbleBody(
    val progress: Float,
    val movie: TraktMovieMinimal? = null,
    val show: TraktShowMinimal? = null,
    val episode: TraktEpisodeMinimal? = null,
    @SerialName("sharing") val sharing: TraktSharing = TraktSharing()
)

@Serializable
data class TraktShowMinimal(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktMovieMinimal(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds
)

@Serializable
data class TraktEpisodeMinimal(
    val season: Int,
    val number: Int,
    val title: String? = null,
    val ids: TraktIds? = null
)

@Serializable
data class TraktSharing(
    val twitter: Boolean = false,
    val tumblr: Boolean = false,
    val medium: Boolean = false
)

@Serializable
data class TraktSyncHistoryBody(
    val movies: List<TraktSyncMovie> = emptyList(),
    val shows: List<TraktSyncShow> = emptyList(),
    val episodes: List<TraktSyncEpisode> = emptyList()
)

@Serializable
data class TraktSyncMovie(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds,
    @SerialName("watched_at") val watchedAt: String? = null
)

@Serializable
data class TraktSyncShow(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds,
    val seasons: List<TraktSyncSeason> = emptyList(),
    @SerialName("watched_at") val watchedAt: String? = null
)

@Serializable
data class TraktSyncSeason(
    val number: Int,
    val episodes: List<TraktSyncEpisode> = emptyList()
)

@Serializable
data class TraktSyncEpisode(
    val season: Int,
    val number: Int,
    val title: String? = null,
    val ids: TraktIds? = null,
    @SerialName("watched_at") val watchedAt: String? = null
)
