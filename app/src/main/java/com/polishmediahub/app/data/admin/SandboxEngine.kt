package com.polishmediahub.app.data.admin

import com.polishmediahub.app.data.plugin.QuickJsEngine
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.model.MediaItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import java.util.Base64

/**
 * Developer sandbox for the wireless admin panel.
 *
 * Supports three modes:
 * - `js`: evaluate a QuickJS snippet (with `httpFetch` global).
 * - `json`: parse and validate a JSON payload, mapping it to [MediaItem] if possible.
 * - `python`: upload a `.py` script to Kodi via JSON-RPC and execute it.
 *
 * The engine has no Android dependencies except the QuickJS wrapper, so JSON and
 * Python path orchestration can be unit-tested with mocked [KodiMediaSource].
 */
class SandboxEngine(
    private val kodiMediaSource: KodiMediaSource,
    private val okHttpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    fun testJson(code: String): SandboxResult {
        val element = try {
            json.parseToJsonElement(code)
        } catch (e: Exception) {
            return SandboxResult.error("JSON syntax error: ${e.message}")
        }

        if (element !is JsonObject) {
            return SandboxResult.error("MediaItem must be a JSON object")
        }

        val mediaItem = element.toMediaItemOrNull()
            ?: return SandboxResult.error("MediaItem validation failed: missing required fields or invalid types")

        return SandboxResult.success(
            message = "MediaItem structure valid",
            preview = mediaItem.toPreviewJson()
        )
    }

    suspend fun testJs(code: String): SandboxResult {
        val engine = QuickJsEngine(okHttpClient)
        engine.init()
        return try {
            val (result, error) = engine.evaluateWithError(code)
            if (error != null) {
                SandboxResult.error("JavaScript error: $error")
            } else {
                SandboxResult.success("JavaScript executed", output = result?.toString())
            }
        } finally {
            engine.close()
        }
    }

    suspend fun testPython(code: String, kodiUrl: String, fileName: String = "test_scraper.py"): SandboxResult {
        if (kodiUrl.isBlank()) {
            return SandboxResult.error("Kodi URL is not configured")
        }

        kodiMediaSource.configure(kodiUrl)

        val filePath = "special://home/addons/plugin.video.fanfilm/$fileName"

        val writeResponse = try {
            kodiMediaSource.writeFile(filePath, code)
        } catch (e: Exception) {
            return SandboxResult.error("Failed to write script to Kodi: ${e.message}")
        }

        if (writeResponse.isBlank()) {
            return SandboxResult.error("Kodi did not respond to Files.WriteFile")
        }

        val writeResult = parseRpcResult(writeResponse)
        if (writeResult?.containsKey("error") == true) {
            return SandboxResult.error("Kodi Files.WriteFile error: ${writeResult["error"]}")
        }

        val runResponse = try {
            kodiMediaSource.runScript(filePath)
        } catch (e: Exception) {
            return SandboxResult.error("Failed to run script on Kodi: ${e.message}")
        }

        if (runResponse.isBlank()) {
            return SandboxResult.error("Kodi did not respond to XBMC.RunScript")
        }

        val runResult = parseRpcResult(runResponse)
        if (runResult?.containsKey("error") == true) {
            return SandboxResult.error("Kodi XBMC.RunScript error: ${runResult["error"]}")
        }

        return SandboxResult.success(
            message = "Python script executed on Kodi",
            output = runResponse
        )
    }

    private fun parseRpcResult(response: String): Map<String, JsonElement>? {
        return try {
            json.parseToJsonElement(response)
                .jsonObject
                .let { it["result"]?.jsonObject ?: it }
                .toMap()
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.toMediaItemOrNull(): MediaItem? {
        val id = string("id") ?: return null
        val title = string("title") ?: return null

        return MediaItem(
            id = id,
            title = title,
            subtitle = string("subtitle").orEmpty(),
            description = string("description").orEmpty(),
            posterUrl = string("posterUrl"),
            backdropUrl = string("backdropUrl"),
            year = string("year").orEmpty(),
            duration = string("duration").orEmpty(),
            rating = string("rating").orEmpty(),
            bitrate = long("bitrate"),
            videoUrl = string("videoUrl"),
            genres = stringList("genres"),
            season = int("season"),
            episode = int("episode"),
            tvgId = string("tvgId"),
            channelNumber = string("channelNumber"),
            headers = stringMap("headers"),
            subtitleUrl = string("subtitleUrl"),
            subtitleHeaders = stringMap("subtitleHeaders"),
            subtitleLanguage = string("subtitleLanguage") ?: "pl",
            drmLicenseUrl = string("drmLicenseUrl"),
            drmScheme = string("drmScheme"),
            drmHeaders = stringMap("drmHeaders"),
            isLive = boolean("isLive") ?: false,
            type = parseType(string("type")),
            tmdbId = int("tmdbId"),
            imdbId = string("imdbId"),
            traktId = int("traktId"),
            malId = int("malId"),
            aniListId = int("aniListId"),
            ageRating = string("ageRating"),
            isAdult = boolean("isAdult") ?: false,
            introStartMs = long("introStartMs"),
            introEndMs = long("introEndMs"),
            outroStartMs = long("outroStartMs"),
            outroEndMs = long("outroEndMs"),
            filmwebRating = string("filmwebRating"),
            filmwebVoteCount = string("filmwebVoteCount"),
            filmwebUrl = string("filmwebUrl")
        )
    }

    private fun MediaItem.toPreviewJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("title", title)
        put("type", type.name)
        if (videoUrl != null) put("videoUrl", videoUrl) else put("videoUrl", JsonNull)
        if (posterUrl != null) put("posterUrl", posterUrl) else put("posterUrl", JsonNull)
        if (backdropUrl != null) put("backdropUrl", backdropUrl) else put("backdropUrl", JsonNull)
        put("year", year)
        put("genres", JsonArray(genres.map { JsonPrimitive(it) }))
    }

    private fun parseType(value: String?): MediaItem.Type {
        if (value == null) return MediaItem.Type.MOVIE
        return MediaItem.Type.entries.find { it.name.equals(value, ignoreCase = true) }
            ?: MediaItem.Type.MOVIE
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        get(key)?.jsonPrimitive?.intOrNull

    private fun JsonObject.long(key: String): Long? =
        get(key)?.jsonPrimitive?.longOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        get(key)?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.stringList(key: String): List<String> =
        get(key)?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    private fun JsonObject.stringMap(key: String): Map<String, String> =
        get(key)?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull }?.filterValues { it != null }?.mapValues { it.value!! }
            ?: emptyMap()
}
