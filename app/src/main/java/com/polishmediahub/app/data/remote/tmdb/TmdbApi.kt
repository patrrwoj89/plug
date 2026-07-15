package com.polishmediahub.app.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("3/trending/movie/week")
    suspend fun trendingMovies(@Query("api_key") apiKey: String): TmdbMoviePaginatedResponse

    @GET("3/movie/popular")
    suspend fun popularMovies(@Query("api_key") apiKey: String, @Query("page") page: Int = 1): TmdbMoviePaginatedResponse

    @GET("3/tv/popular")
    suspend fun popularSeries(@Query("api_key") apiKey: String, @Query("page") page: Int = 1): TmdbSeriesPaginatedResponse

    @GET("3/search/multi")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchPaginatedResponse

    @GET("3/movie/{id}")
    suspend fun movieDetails(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbMovie

    @GET("3/tv/{id}")
    suspend fun seriesDetails(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbSeries

    @GET("3/movie/{id}/images")
    suspend fun movieImages(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbImagesResponse

    @GET("3/tv/{id}/images")
    suspend fun seriesImages(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbImagesResponse

    @GET("3/movie/{id}/credits")
    suspend fun movieCredits(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbCreditsResponse

    @GET("3/tv/{id}/credits")
    suspend fun seriesCredits(@Path("id") id: Int, @Query("api_key") apiKey: String): TmdbCreditsResponse

    @GET("3/genre/movie/list")
    suspend fun movieGenres(@Query("api_key") apiKey: String): TmdbGenreResponse

    @GET("3/genre/tv/list")
    suspend fun tvGenres(@Query("api_key") apiKey: String): TmdbGenreResponse

    @GET("3/movie/{id}/recommendations")
    suspend fun movieRecommendations(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbMoviePaginatedResponse

    @GET("3/tv/{id}/recommendations")
    suspend fun seriesRecommendations(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbSeriesPaginatedResponse
}

@Serializable
data class TmdbPaginatedResponse<T>(
    val page: Int,
    val results: List<T>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class TmdbMoviePaginatedResponse(
    val page: Int,
    val results: List<TmdbMovie>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class TmdbSeriesPaginatedResponse(
    val page: Int,
    val results: List<TmdbSeries>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class TmdbSearchPaginatedResponse(
    val page: Int,
    val results: List<TmdbSearchResult>,
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
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

@Serializable
data class TmdbImagesResponse(
    val id: Int = 0,
    val backdrops: List<TmdbImage> = emptyList(),
    val posters: List<TmdbImage> = emptyList()
)

@Serializable
data class TmdbImage(
    @SerialName("file_path") val filePath: String,
    @SerialName("aspect_ratio") val aspectRatio: Double = 0.0,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class TmdbGenreResponse(
    val genres: List<TmdbGenre> = emptyList()
)

@Serializable
data class TmdbGenre(
    val id: Int,
    val name: String
)

@Serializable
data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember> = emptyList()
)

@Serializable
data class TmdbCastMember(
    val name: String,
    val order: Int = 0
)
