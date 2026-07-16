package com.polishmediahub.app.data.remote.mdblist

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.legal.LegalSourcesRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private const val BASE_URL = "https://api.mdblist.com"

/**
 * Federated [MediaSource] for MDBList (https://mdblist.com).
 *
 * Loads public top lists, the authenticated user's lists and supports cross-ID lookup
 * by imdb/tmdb/trakt/tvdb. Every [MediaItem] produced by this source carries the
 * original tmdb_id, imdb_id and trakt_id so other modules can match it against
 * Stremio, Plex, Jellyfin, Trakt, etc.
 */
@Singleton
class MdbListMediaSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val apiConfigRepository: ApiConfigRepository,
    private val legalSourcesRepository: LegalSourcesRepository
) : com.polishmediahub.app.data.source.MediaSource {

    override val id: String = "mdblist"
    override val name: String = "MDBList"
    override val isConfigurable: Boolean = true

    private var configuredKey: String = ""

    fun configure(apiKey: String) {
        configuredKey = apiKey.trim()
    }

    private suspend fun apiKey(): String {
        if (configuredKey.isNotBlank()) return configuredKey
        val stored = apiConfigRepository.mdbListApiKey.first().trim()
        if (stored.isNotBlank()) configuredKey = stored
        return stored
    }

    override suspend fun isAvailable(): Boolean = apiKey().isNotBlank()

    override suspend fun featured(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val key = apiKey()
            if (key.isBlank()) return@withContext emptyList()
            val starterIds = starterListIds()
            val allLists = topLists(key).take(3).map { it.id } + starterIds
            allLists.map { listId ->
                async {
                    listItems(listId, key)?.let { items ->
                        (items.movies + items.shows).map { it.toMediaItem() }
                    } ?: emptyList()
                }
            }.awaitAll().flatten().take(30)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "featured failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = coroutineScope {
        try {
            val key = apiKey()
            if (key.isBlank()) return@coroutineScope emptyList()

            val topDeferred = async(Dispatchers.IO) { topLists(key) }
            val userDeferred = async(Dispatchers.IO) { userLists(key) }
            val top = topDeferred.await().take(10)
            val user = userDeferred.await().take(10)
            val starter = legalSourcesRepository.load()?.mdbList?.starterLists ?: emptyList()

            val topAndUserCategories = (top + user).map { list ->
                async(Dispatchers.IO) {
                    val items = listItems(list.id, key)
                    Category(
                        id = "mdblist:list:${list.id}",
                        name = list.name,
                        items = items?.let { (it.movies + it.shows).map { item -> item.toMediaItem() } } ?: emptyList()
                    )
                }
            }
            val starterCategories = starter.map { entry ->
                async(Dispatchers.IO) {
                    val items = listItems(entry.id, key)
                    Category(
                        id = "mdblist:list:${entry.id}",
                        name = entry.name.takeIf { it.isNotBlank() } ?: "MDBList Starter",
                        items = items?.let { (it.movies + it.shows).map { item -> item.toMediaItem() } } ?: emptyList()
                    )
                }
            }
            (topAndUserCategories + starterCategories).awaitAll()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "categories failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val key = apiKey()
            if (key.isBlank() || query.isBlank()) return@withContext emptyList()
            val result = get<MdbListSearchResult>("/search/any", mapOf("query" to query, "apikey" to key))
            result?.search?.map { it.toMediaItem() } ?: emptyList()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "search failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val key = apiKey()
            if (key.isBlank()) return@withContext null
            val parts = id.split(":")
            if (parts.size != 4 || parts[0] != this@MdbListMediaSource.id) return@withContext null
            val provider = parts[1]
            val mediaType = parts[2]
            val mediaId = parts[3]
            if (provider == "mdblist") return@withContext null
            val info = get<MdbListMediaInfo>("/${provider}/${mediaType}/${mediaId}", mapOf("apikey" to key))
            info?.toMediaItem(id)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "byId failed for $id: ${e.message}", e)
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = null

    private suspend fun topLists(apiKey: String): List<MdbListSummary> =
        get("/lists/top", mapOf("apikey" to apiKey)) ?: emptyList()

    private suspend fun userLists(apiKey: String): List<MdbListSummary> =
        get("/lists/user", mapOf("apikey" to apiKey)) ?: emptyList()

    private suspend fun listItems(listId: Int, apiKey: String): MdbListItems? =
        get("/lists/${listId}/items", mapOf("apikey" to apiKey))

    private fun starterListIds(): List<Int> =
        legalSourcesRepository.load()?.mdbList?.starterLists?.map { it.id } ?: emptyList()

    private suspend inline fun <reified T> get(path: String, query: Map<String, String>): T? = try {
        val urlBuilder = BASE_URL.toHttpUrl().newBuilder().addPathSegments(path.removePrefix("/"))
        query.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val request = Request.Builder().url(urlBuilder.build()).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "HTTP ${response.code} for $path")
                return@use null
            }
            val body = response.body?.string()
            if (body.isNullOrBlank()) return@use null
            try {
                json.decodeFromString<T>(body)
            } catch (e: SerializationException) {
                if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "parse error for $path: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w("MdbListMediaSource", "request error for $path: ${e.message}", e)
        null
    }

    private fun MdbListItem.toMediaItem(): MediaItem {
        val (provider, providerId) = when {
            !imdbId.isNullOrBlank() -> "imdb" to imdbId
            mediatype.equals("show", ignoreCase = true) && tvdbId != null -> "tvdb" to tvdbId.toString()
            else -> "mdblist" to id.toString()
        }
        val type = if (mediatype.equals("show", ignoreCase = true)) MediaItem.Type.SERIES else MediaItem.Type.MOVIE
        return MediaItem(
            id = "mdblist:$provider:${if (type == MediaItem.Type.SERIES) "show" else "movie"}:$providerId",
            title = title,
            subtitle = if (releaseYear > 0) releaseYear.toString() else "",
            description = "",
            year = if (releaseYear > 0) releaseYear.toString() else "",
            type = type,
            imdbId = imdbId,
            tmdbId = null,
            traktId = null,
            isAdult = adult == 1
        )
    }

    private fun MdbListSearchItem.toMediaItem(): MediaItem {
        val ids = this.ids
        val (provider, providerId) = when {
            !ids.imdbid.isNullOrBlank() -> "imdb" to ids.imdbid
            ids.tmdbid != null -> "tmdb" to ids.tmdbid.toString()
            ids.traktid != null -> "trakt" to ids.traktid.toString()
            ids.tvdbId != null -> "tvdb" to ids.tvdbId.toString()
            else -> "mdblist" to title
        }
        val type = if (type.equals("show", ignoreCase = true)) MediaItem.Type.SERIES else MediaItem.Type.MOVIE
        return MediaItem(
            id = "mdblist:$provider:${if (type == MediaItem.Type.SERIES) "show" else "movie"}:$providerId",
            title = title,
            subtitle = if (year > 0) year.toString() else "",
            description = "",
            year = if (year > 0) year.toString() else "",
            type = type,
            imdbId = ids.imdbid,
            tmdbId = ids.tmdbid,
            traktId = ids.traktid
        )
    }

    private fun MdbListMediaInfo.toMediaItem(itemId: String): MediaItem {
        val type = if (type.equals("show", ignoreCase = true)) MediaItem.Type.SERIES else MediaItem.Type.MOVIE
        return MediaItem(
            id = itemId,
            title = title,
            subtitle = if (year > 0) year.toString() else "",
            description = description,
            posterUrl = poster?.takeIf { it.startsWith("http") },
            backdropUrl = backdrop?.takeIf { it.startsWith("http") },
            year = if (year > 0) year.toString() else "",
            rating = if (scoreAverage > 0) scoreAverage.toString() else if (score > 0) score.toString() else "",
            type = type,
            imdbId = ids.imdb,
            tmdbId = ids.tmdb,
            traktId = ids.trakt
        )
    }
}
