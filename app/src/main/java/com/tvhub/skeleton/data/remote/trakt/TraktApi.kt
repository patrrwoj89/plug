package com.tvhub.skeleton.data.remote.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
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
