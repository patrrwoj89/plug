package com.polishmediahub.app.data.source

import android.util.Base64
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
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
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : MediaSource {

    override val id: String = "kodi"
    override val name: String = "Kodi"
    override val isConfigurable: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var configuredBaseUrl: String = ""

    fun configure(url: String) {
        configuredBaseUrl = url.removeSuffix("/")
    }

    private suspend fun baseUrl(): String {
        if (configuredBaseUrl.isNotBlank()) return configuredBaseUrl
        val stored = apiConfigRepository.kodiUrl.first().removeSuffix("/")
        if (stored.isNotBlank()) configuredBaseUrl = stored
        return configuredBaseUrl
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
        return when {
            id.startsWith("kodi:plugin:") -> {
                val file = decodePluginId(id)
                getFileDetails(file)
            }
            else -> (getMovies() + getTvShows()).find { it.id == id }
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = resolveItem(mediaItem).videoUrl

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        val file = pluginFileFromMediaItem(mediaItem) ?: mediaItem.videoUrl ?: return mediaItem
        if (file.isBlank()) return mediaItem
        return try {
            val (url, drm) = prepareDownload(file)
            if (url.isNullOrBlank()) return mediaItem
            mediaItem.copy(
                videoUrl = url,
                drmLicenseUrl = drm?.licenseUrl ?: mediaItem.drmLicenseUrl,
                drmScheme = drm?.scheme ?: mediaItem.drmScheme,
                drmHeaders = drm?.headers ?: mediaItem.drmHeaders
            )
        } catch (_: Exception) {
            mediaItem
        }
    }

    suspend fun getPluginDirectory(directoryPath: String): List<MediaItem> = try {
        val body = rpc(
            "Files.GetDirectory",
            buildJsonObject {
                put("directory", directoryPath)
                put("media", "video")
                putJsonArray("properties") {
                    add("title")
                    add("file")
                    add("thumbnail")
                    add("fanart")
                    add("genre")
                    add("plot")
                    add("year")
                    add("rating")
                    add("duration")
                    add("mimetype")
                    add("inputstream.adaptive.license_key")
                    add("inputstream.adaptive.license_type")
                }
            }
        )
        parseDirectory(body, directoryPath)
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getFileDetails(file: String): MediaItem? = try {
        val body = rpc(
            "Files.GetFileDetails",
            buildJsonObject {
                put("file", file)
                putJsonArray("properties") {
                    add("title")
                    add("file")
                    add("thumbnail")
                    add("fanart")
                    add("genre")
                    add("plot")
                    add("year")
                    add("rating")
                    add("duration")
                    add("mimetype")
                    add("inputstream.adaptive.license_key")
                    add("inputstream.adaptive.license_type")
                }
            }
        )
        parseFileDetails(body, file)
    } catch (_: Exception) {
        null
    }

    suspend fun setAddonSetting(addonId: String, settingId: String, value: String): Boolean = try {
        val response = rpc(
            "Settings.SetSettingValue",
            buildJsonObject {
                put("setting", "addon_${addonId}_$settingId")
                put("value", value)
            }
        )
        json.parseToJsonElement(response)
            .jsonObject["result"]
            ?.jsonPrimitive
            ?.booleanOrNull == true
    } catch (_: Exception) {
        false
    }

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
        val base = baseUrl().ifBlank { throw IllegalStateException("Kodi URL not configured") }
        val payload = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", 1)
        }.toString()
        val request = Request.Builder()
            .url("$base/jsonrpc")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { it.body?.string().orEmpty() }
        }
    }

    private suspend fun prepareDownload(file: String): Pair<String?, DrmInfo?> {
        val body = rpc(
            "Files.PrepareDownload",
            buildJsonObject { put("path", file) }
        )
        val jsonElement = json.parseToJsonElement(body)
        val result = jsonElement.jsonObject["result"]?.jsonObject ?: return null to null
        val details = result["details"]?.jsonObject
        val url = details?.get("path")?.jsonPrimitive?.contentOrNull
            ?: result["path"]?.jsonPrimitive?.contentOrNull
        val drm = parseDrm(details ?: result)
        return url to drm
    }

    private fun parseDirectory(body: String, directoryPath: String): List<MediaItem> = try {
        json.parseToJsonElement(body)
            .jsonObject["result"]
            ?.jsonObject?.get("files")
            ?.jsonArray
            ?.mapIndexedNotNull { index, element ->
                element.jsonObject.toPluginMediaItem(directoryPath, index)
            } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseFileDetails(body: String, file: String): MediaItem? = try {
        json.parseToJsonElement(body)
            .jsonObject["result"]
            ?.jsonObject?.get("filedetails")
            ?.jsonObject
            ?.toPluginMediaItem(parent = "", index = 0, fallbackFile = file)
    } catch (_: Exception) {
        null
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

    private fun JsonObject.toPluginMediaItem(parent: String, index: Int, fallbackFile: String? = null): MediaItem? {
        val label = string("label").orEmpty().ifBlank { string("title").orEmpty() }
        val file = string("file") ?: fallbackFile ?: return null
        val fileType = string("filetype") ?: "file"
        val isDirectory = fileType.equals("directory", ignoreCase = true)
        val fanart = toKodiImageUrl(string("fanart"))
        val thumb = toKodiImageUrl(string("thumbnail"))
        val drm = parseDrm(this)
        return MediaItem(
            id = encodePluginId(parent, file, index),
            title = label,
            subtitle = if (isDirectory) "Folder" else string("mimetype").orEmpty(),
            description = string("plot").orEmpty(),
            posterUrl = thumb,
            backdropUrl = fanart ?: thumb,
            year = int("year")?.toString() ?: "",
            rating = double("rating")?.toString() ?: "",
            genres = stringList("genre"),
            videoUrl = file,
            duration = int("duration")?.toString() ?: "",
            drmLicenseUrl = drm?.licenseUrl,
            drmScheme = drm?.scheme,
            drmHeaders = drm?.headers ?: emptyMap(),
            type = if (isDirectory) MediaItem.Type.SERIES else MediaItem.Type.MOVIE
        )
    }

    private fun parseDrm(json: JsonObject): DrmInfo? {
        val licenseKey = json.string("inputstream.adaptive.license_key")
        val licenseType = json.string("inputstream.adaptive.license_type")
        val licenseUrl = when {
            licenseKey?.startsWith("http", ignoreCase = true) == true -> licenseKey
            licenseKey?.isNotBlank() == true && licenseKey.contains("http") -> licenseKey
            else -> null
        } ?: return null
        val scheme = when (licenseType?.lowercase()) {
            "com.widevine.alpha", "widevine" -> "widevine"
            "com.microsoft.playready", "playready" -> "playready"
            "org.w3.clearkey", "clearkey" -> "clearkey"
            else -> "widevine"
        }
        val headers = mutableMapOf<String, String>()
        licenseKey.orEmpty().substringAfter("|", "")
            .split("&")
            .forEach { pair ->
                val eq = pair.indexOf('=')
                if (eq > 0) headers[pair.substring(0, eq)] = pair.substring(eq + 1)
            }
        return DrmInfo(licenseUrl, scheme, headers)
    }

    private fun toKodiImageUrl(path: String?): String? {
        if (path == null) return null
        if (!path.startsWith("image://", ignoreCase = true)) return path
        val inner = path.removePrefix("image://").removeSuffix("/")
        val base = configuredBaseUrl
        val encoded = URLEncoder.encode("image://$inner", "UTF-8")
        return "$base/image/$encoded"
    }

    private fun encodePluginId(directory: String, file: String, index: Int): String {
        val raw = "$directory\u0000$file\u0000$index"
        val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return "kodi:plugin:$encoded"
    }

    private fun decodePluginId(id: String): String {
        val encoded = id.removePrefix("kodi:plugin:")
        val decoded = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8)
        return decoded.substringAfter("\u0000").substringBeforeLast("\u0000")
    }

    private fun pluginFileFromMediaItem(mediaItem: MediaItem): String? {
        return if (mediaItem.id.startsWith("kodi:plugin:")) {
            decodePluginId(mediaItem.id)
        } else {
            mediaItem.videoUrl
        }
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        get(key)?.jsonPrimitive?.intOrNull

    private fun JsonObject.double(key: String): Double? =
        get(key)?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

    private fun JsonObject.stringList(key: String): List<String> =
        get(key)?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    private data class DrmInfo(
        val licenseUrl: String,
        val scheme: String,
        val headers: Map<String, String>
    )
}
