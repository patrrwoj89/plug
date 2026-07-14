package com.polishmediahub.app.data.source

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KodiMediaSource @Inject constructor(
    private val client: OkHttpClient
) : MediaSource {

    override val id: String = "kodi"
    override val name: String = "Kodi"
    override val isConfigurable: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var baseUrl: String = "http://localhost:8080"

    fun configure(url: String) {
        baseUrl = url.removeSuffix("/")
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val response = rpc("JSONRPC.Ping", buildJsonObject { })
            response.contains("\"result\":\"pong\"")
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun featured(): List<MediaItem> = coroutineScope {
        val movies = async(Dispatchers.IO) { getMovies().take(10) }
        val tv = async(Dispatchers.IO) { getTvShows().take(10) }
        movies.await() + tv.await()
    }

    override suspend fun categories(): List<Category> = coroutineScope {
        val movies = async(Dispatchers.IO) { getMovies() }
        val tv = async(Dispatchers.IO) { getTvShows() }
        listOf(
            Category("kodi:movies", "Kodi Movies", movies.await()),
            Category("kodi:tv", "Kodi TV Shows", tv.await())
        )
    }

    override suspend fun search(query: String): List<MediaItem> = coroutineScope {
        val movies = async(Dispatchers.IO) { getMovies() }
        val tv = async(Dispatchers.IO) { getTvShows() }
        (movies.await() + tv.await()).filter { it.title.contains(query, ignoreCase = true) }
    }

    override suspend fun byId(id: String): MediaItem? {
        return (getMovies() + getTvShows()).find { it.id == id }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = mediaItem.videoUrl

    private suspend fun getMovies(): List<MediaItem> {
        val body = rpc(
            "VideoLibrary.GetMovies",
            buildJsonObject {
                putJsonObject("limits") {
                    put("start", 0)
                    put("end", 100)
                }
                putJsonArray("properties") {
                    add("title")
                    add("year")
                    add("plot")
                    add("thumbnail")
                    add("fanart")
                    add("file")
                    add("genre")
                    add("rating")
                }
            }
        )
        return parseMovies(body)
    }

    private suspend fun getTvShows(): List<MediaItem> {
        val body = rpc(
            "VideoLibrary.GetTVShows",
            buildJsonObject {
                putJsonObject("limits") {
                    put("start", 0)
                    put("end", 100)
                }
                putJsonArray("properties") {
                    add("title")
                    add("year")
                    add("plot")
                    add("thumbnail")
                    add("fanart")
                    add("file")
                    add("genre")
                    add("rating")
                }
            }
        )
        return parseTvShows(body)
    }

    private suspend fun rpc(method: String, params: JsonObject): String {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", 1)
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl/jsonrpc")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return with(Dispatchers.IO) {
            client.newCall(request).execute().use { it.body?.string().orEmpty() }
        }
    }

    private fun parseMovies(body: String): List<MediaItem> = try {
        json.parseToJsonElement(body)
            .jsonObject["result"]
            ?.jsonObject?.get("movies")
            ?.jsonArray
            ?.map { it.jsonObject.toMovie() }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseTvShows(body: String): List<MediaItem> = try {
        json.parseToJsonElement(body)
            .jsonObject["result"]
            ?.jsonObject?.get("tvshows")
            ?.jsonArray
            ?.map { it.jsonObject.toTvShow() }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun toKodiImageUrl(path: String?): String? {
        if (path == null) return null
        if (!path.startsWith("image://", ignoreCase = true)) return path
        val inner = path.removePrefix("image://").removeSuffix("/")
        val encoded = URLEncoder.encode("image://$inner", "UTF-8")
        return "$baseUrl/image/$encoded"
    }

    private fun JsonObject.toMovie(): MediaItem {
        val fanart = toKodiImageUrl(string("fanart"))
        val thumb = toKodiImageUrl(string("thumbnail"))
        return MediaItem(
            id = "kodi:movie:${int("movieid")}",
            title = string("title").orEmpty(),
            subtitle = int("year")?.toString() ?: "",
            description = string("plot").orEmpty(),
            posterUrl = thumb,
            backdropUrl = fanart ?: thumb,
            year = int("year")?.toString() ?: "",
            rating = double("rating")?.toString() ?: "",
            genres = stringList("genre"),
            videoUrl = string("file"),
            type = MediaItem.Type.MOVIE
        )
    }

    private fun JsonObject.toTvShow(): MediaItem {
        val fanart = toKodiImageUrl(string("fanart"))
        val thumb = toKodiImageUrl(string("thumbnail"))
        return MediaItem(
            id = "kodi:tv:${int("tvshowid")}",
            title = string("title").orEmpty(),
            subtitle = int("year")?.toString() ?: "",
            description = string("plot").orEmpty(),
            posterUrl = thumb,
            backdropUrl = fanart ?: thumb,
            year = int("year")?.toString() ?: "",
            rating = double("rating")?.toString() ?: "",
            genres = stringList("genre"),
            videoUrl = string("file"),
            type = MediaItem.Type.SERIES
        )
    }
}

private fun JsonObject.string(key: String): String? =
    get(key)?.jsonPrimitive?.content

private fun JsonObject.int(key: String): Int? =
    get(key)?.jsonPrimitive?.int

private fun JsonObject.double(key: String): Double? =
    get(key)?.jsonPrimitive?.content?.toDoubleOrNull()

private fun JsonObject.stringList(key: String): List<String> =
    get(key)?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
