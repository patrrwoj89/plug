package com.polishmediahub.app.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("3/trending/movie/week")
    suspend fun trendingMovies(@Query("api_key") apiKey: String): TmdbPaginatedResponse<TmdbMovie>

    @GET("3/movie/popular")
    suspend fun popularMovies(@Query("api_key") apiKey: String, @Query("page") page: Int = 1): TmdbPaginatedResponse<TmdbMovie>

    @GET("3/tv/popular")
    suspend fun popularSeries(@Query("api_key") apiKey: String, @Query("page") page: Int = 1): TmdbPaginatedResponse<TmdbSeries>

    @GET("3/search/multi")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<TmdbSearchResult>

    @GET("3/movie/{id}")
    suspend fun movieDetails(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbMovie

    @GET("3/tv/{id}")
    suspend fun seriesDetails(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbSeries
}

@Serializable
data class TmdbPaginatedResponse<T>(
    val page: Int,
    val results: List<T>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

@Serializable
data class TmdbSeries(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

@Serializable
data class TmdbSearchResult(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null
)
