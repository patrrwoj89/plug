package com.polishmediahub.app.data.remote.filmweb

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.local.FilmwebCacheRepository
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://www.filmweb.pl"
private const val API_BASE = "$BASE_URL/api/v1"
private const val IMAGE_BASE = "https://fwcdn.pl/fpo"

@Singleton
class FilmwebMediaSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val cacheRepository: FilmwebCacheRepository
) : MediaSource {

    override val id: String = "filmweb"
    override val name: String = "Filmweb.pl"
    override val isConfigurable: Boolean = false

    private val filmwebClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun isAvailable(): Boolean = true
    override suspend fun featured(): List<MediaItem> = emptyList()
    override suspend fun categories(): List<Category> = emptyList()
    override suspend fun search(query: String): List<MediaItem> = emptyList()
    override suspend fun byId(id: String): MediaItem? = null
    override suspend fun resolve(mediaItem: MediaItem): String? = null

    /**
     * Fetches Polish metadata (description, poster, rating, votes) from Filmweb.pl.
     * Caches results in Room so future detail loads are instant.
     */
    suspend fun fetchPolishMetadata(title: String, year: String?): MediaItem? = withContext(Dispatchers.IO) {
        cacheRepository.get(title, year.orEmpty())?.let { return@withContext it }

        try {
            val filmId = searchFilmId(title, year) ?: return@withContext null
            val info = fetchInfo(filmId) ?: return@withContext null
            val rating = fetchRating(filmId)
            val description = fetchDescription(filmId)?.synopsis
                ?: fetchPreview(filmId)?.plot?.synopsis
                ?: ""

            val cleanDescription = stripPersonTags(description)
            val posterUrl = buildImageUrl(info.posterPath)
            val filmYear = info.year?.toString() ?: year.orEmpty()
            val filmTitle = info.title ?: title

            val item = MediaItem(
                id = "filmweb:$filmId",
                title = filmTitle,
                description = cleanDescription,
                posterUrl = posterUrl,
                backdropUrl = posterUrl,
                year = filmYear,
                filmwebRating = rating?.rate?.let { String.format(Locale.US, "%.1f", it) },
                filmwebVoteCount = rating?.count?.toString(),
                filmwebUrl = buildFilmwebUrl(filmId, filmTitle, filmYear)
            )
            cacheRepository.save(item)
            item
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("FilmwebMediaSource", "fetchPolishMetadata failed for '$title' ($year): ${e.message}")
            null
        }
    }

    private suspend fun searchFilmId(title: String, year: String?): Int? = try {
        val response = if (!year.isNullOrBlank()) {
            val url = "$API_BASE/films/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", title)
                .addQueryParameter("startYear", year)
                .addQueryParameter("endYear", year)
                .addQueryParameter("pageSize", "12")
                .build()
            parseJson<FilmwebSearchResponse>(fetchBody(url.toString()))
        } else {
            val url = "$API_BASE/live/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", title)
                .build()
            parseJson<FilmwebSearchResponse>(fetchBody(url.toString()))
        }

        val hits = response?.searchHits
        if (hits.isNullOrEmpty()) {
            null
        } else {
            hits.firstOrNull { it.type == null || it.type == "film" }?.id
                ?: hits.firstOrNull()?.id
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w("FilmwebMediaSource", "searchFilmId failed: ${e.message}")
        null
    }

    private suspend fun fetchInfo(filmId: Int): FilmwebInfo? =
        parseJson<FilmwebInfo>(fetchBody("$API_BASE/film/$filmId/info"))
            ?: parseJson<FilmwebInfo>(fetchBody("$API_BASE/title/$filmId/info"))

    private suspend fun fetchRating(filmId: Int): FilmwebRatingResponse? =
        parseJson<FilmwebRatingResponse>(fetchBody("$API_BASE/film/$filmId/rating"))

    private suspend fun fetchDescription(filmId: Int): FilmwebDescriptionResponse? =
        parseJson<FilmwebDescriptionResponse>(fetchBody("$API_BASE/film/$filmId/description"))

    private suspend fun fetchPreview(filmId: Int): FilmwebPreviewResponse? =
        parseJson<FilmwebPreviewResponse>(fetchBody("$API_BASE/film/$filmId/preview"))

    private fun fetchBody(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .header("x-locale", "pl_PL")
            .build()

        return try {
            filmwebClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (BuildConfig.DEBUG) Log.w("FilmwebMediaSource", "HTTP ${response.code} for $url")
                    null
                } else {
                    response.body?.string()
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("FilmwebMediaSource", "fetchBody failed for $url: ${e.message}")
            null
        }
    }

    private inline fun <reified T> parseJson(body: String?): T? = try {
        if (body.isNullOrBlank()) null else json.decodeFromString<T>(body)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w("FilmwebMediaSource", "parseJson failed: ${e.message}")
        null
    }

    private fun buildImageUrl(posterPath: String?): String? {
        if (posterPath.isNullOrBlank()) return null
        val sized = posterPath.replace(".$", ".6")
        return "${IMAGE_BASE}$sized"
    }

    private fun buildFilmwebUrl(filmId: Int, title: String, year: String): String {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        return "https://www.filmweb.pl/film/$encodedTitle-$year-$filmId"
    }

    private fun stripPersonTags(text: String): String {
        return text.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
