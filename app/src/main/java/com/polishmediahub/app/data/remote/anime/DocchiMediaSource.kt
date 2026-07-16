package com.polishmediahub.app.data.remote.anime

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private const val BASE_URL = "https://api.docchi.pl/v1"
private const val TAG = "DocchiMediaSource"

/**
 * Polish anime metadata and episode-link source backed by the official Docchi API.
 *
 * Endpoints and response schema verified live against https://dev.docchi.pl/docchiapi
 * on 2026-07-14. Series and search results are mapped to [MediaItem]; episode
 * resolution returns the publisher/player link (CDA, Google Drive, Mega, etc.)
 * that the selected translator provides.
 */
@Singleton
class DocchiMediaSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) : MediaSource {

    override val id: String = "docchi"
    override val name: String = "Docchi"
    override val isConfigurable: Boolean = false

    override suspend fun isAvailable(): Boolean = true

    override suspend fun featured(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            getList("$BASE_URL/series/list?limit=20")?.map { it.toMediaItem() } ?: emptyList()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "featured failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val all = getList("$BASE_URL/series/list?limit=50") ?: return@withContext emptyList()
            val tv = all.filter { it.seriesType.equals("TV", ignoreCase = true) }.take(20)
            val movies = all.filter { it.seriesType.equals("movie", ignoreCase = true) }.take(20)
            val specials = all.filter {
                val type = it.seriesType?.lowercase() ?: ""
                type != "tv" && type != "movie" && type != "ova"
            }.take(20)
            val ova = all.filter { it.seriesType.equals("OVA", ignoreCase = true) }.take(20)
            listOfNotNull(
                Category("docchi_tv", "Docchi TV", tv.map { it.toMediaItem() }).takeIf { tv.isNotEmpty() },
                Category("docchi_movies", "Docchi Movies", movies.map { it.toMediaItem() }).takeIf { movies.isNotEmpty() },
                Category("docchi_ova", "Docchi OVA", ova.map { it.toMediaItem() }).takeIf { ova.isNotEmpty() },
                Category("docchi_specials", "Docchi Specials", specials.map { it.toMediaItem() }).takeIf { specials.isNotEmpty() }
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "categories failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            getList("$BASE_URL/series/related/$encoded")?.map { it.toMediaItem() } ?: emptyList()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "search failed for $query: ${e.message}")
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? = withContext(Dispatchers.IO) {
        val slug = id.removePrefix("docchi:").trim().substringBefore(":")
        if (slug.isBlank()) return@withContext null
        try {
            getSingle("$BASE_URL/series/find/$slug")?.toMediaItem()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "byId failed for $id: ${e.message}")
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = withContext(Dispatchers.IO) {
        try {
            val slug = mediaItem.id.removePrefix("docchi:").trim().substringBefore(":")
            val episode = mediaItem.episode ?: 1
            getEpisodePlayers(slug, episode)
                ?.firstOrNull { it.player.startsWith("http", ignoreCase = true) }
                ?.player
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "resolve failed for ${mediaItem.id}: ${e.message}")
            null
        }
    }

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        val url = resolve(mediaItem) ?: return mediaItem
        return mediaItem.copy(
            videoUrl = url,
            headers = mapOf(
                "Referer" to "https://docchi.pl/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
        )
    }

    override suspend fun reportProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long, state: PlaybackState) {
        // Docchi does not report watch progress.
    }

    private suspend fun getList(url: String): List<DocchiSeries>? = request(url)

    private suspend fun getSingle(url: String): DocchiSeries? = request(url)

    private suspend fun getEpisodePlayers(slug: String, episode: Int): List<DocchiEpisodePlayer>? =
        request("$BASE_URL/episodes/find/$slug/$episode")

    private suspend inline fun <reified T> request(url: String): T? = try {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (BuildConfig.DEBUG) Log.w(TAG, "HTTP ${response.code} for $url")
                return@use null
            }
            val body = response.body?.string()
            if (body.isNullOrBlank()) return@use null
            try {
                json.decodeFromString<T>(body)
            } catch (e: SerializationException) {
                if (BuildConfig.DEBUG) Log.w(TAG, "parse error for $url: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w(TAG, "request error for $url: ${e.message}")
        null
    }

    private fun DocchiSeries.toMediaItem(): MediaItem {
        val primaryTitle = title.takeIf { it.isNotBlank() }
            ?: titleEn?.takeIf { it.isNotBlank() }
            ?: "Anime $slug"

        val isAdult = adultContent == "true" || genres.any { it.equals("Hentai", ignoreCase = true) }
        val year = airedFrom?.take(4)?.takeIf { it.length == 4 }
            ?: seasonYear?.toString()
            ?: ""

        val subtype = seriesType?.replaceFirstChar { it.uppercase() } ?: "Anime"
        val type = if (seriesType.equals("movie", ignoreCase = true)) MediaItem.Type.MOVIE else MediaItem.Type.SERIES

        return MediaItem(
            id = "docchi:$slug",
            title = primaryTitle,
            subtitle = buildString {
                append(subtype)
                if ((episodes ?: 0) > 0) append(" • $episodes episodes")
                if (year.isNotBlank()) append(" • $year")
            },
            description = description?.replace(Regex("<.*?>"), "")?.takeIf { it.isNotBlank() } ?: "",
            posterUrl = cover?.takeIf { it.startsWith("http") },
            backdropUrl = bg?.takeIf { it.startsWith("http") } ?: cover?.takeIf { it.startsWith("http") },
            year = year,
            rating = "",
            type = type,
            malId = malId?.toInt(),
            aniListId = aniId?.toInt(),
            ageRating = if (isAdult) "18+" else null,
            isAdult = isAdult,
            genres = genres
        )
    }
}

@kotlinx.serialization.Serializable
private data class DocchiSeries(
    val id: Long? = null,
    @SerialName("mal_id") val malId: Long? = null,
    @SerialName("ani_id") val aniId: Long? = null,
    @SerialName("adult_content") val adultContent: String? = null,
    val title: String = "",
    @SerialName("title_en") val titleEn: String? = null,
    val slug: String = "",
    val description: String? = null,
    val cover: String? = null,
    val bg: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("broadcast_day") val broadcastDay: String? = null,
    @SerialName("aired_from") val airedFrom: String? = null,
    val episodes: Int? = null,
    val season: String? = null,
    @SerialName("season_year") val seasonYear: Int? = null,
    @SerialName("series_type") val seriesType: String? = null
)

@kotlinx.serialization.Serializable
private data class DocchiEpisodePlayer(
    val id: Long = 0,
    @SerialName("anime_episode_number") val episodeNumber: Int = 0,
    val player: String = "",
    @SerialName("player_hosting") val playerHosting: String? = null,
    @SerialName("translator_title") val translatorTitle: String? = null
)
