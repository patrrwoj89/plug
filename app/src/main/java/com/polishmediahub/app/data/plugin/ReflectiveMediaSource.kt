package com.polishmediahub.app.data.plugin

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient

class ReflectiveMediaSource(
    override val id: String,
    override val name: String,
    private val pluginInstance: Any,
    private val client: OkHttpClient
) : MediaSource {

    override val isConfigurable: Boolean = false

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun featured(): List<MediaItem> =
        invokeListMethods("featured", "getMainPage", "mainPage", "home")

    override suspend fun categories(): List<Category> =
        listOf(Category(id, name, featured()))

    override suspend fun search(query: String): List<MediaItem> =
        invokeListMethods(arrayOf<Any?>(query), "search", "find", "query")

    override suspend fun byId(id: String): MediaItem? =
        invokeOneMethod(arrayOf<Any?>(id), "byId", "load", "get", "detail")

    override suspend fun resolve(mediaItem: MediaItem): String? =
        resolveInternal(mediaItem)

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        val url = resolve(mediaItem) ?: return mediaItem
        val headers = resolveHeaders(mediaItem)
        return mediaItem.copy(videoUrl = url, headers = headers)
    }

    private fun invokeListMethods(vararg methodNames: String): List<MediaItem> =
        invokeListMethods(emptyArray<Any?>(), *methodNames)

    private fun invokeListMethods(args: Array<Any?>, vararg methodNames: String): List<MediaItem> {
        for (name in methodNames) {
            val result = invokeMethod(name, args) ?: continue
            return parseList(result)
        }
        return emptyList()
    }

    private fun invokeOneMethod(args: Array<Any?>, vararg methodNames: String): MediaItem? {
        for (name in methodNames) {
            val result = invokeMethod(name, args) ?: continue
            return parseOne(result)
        }
        return null
    }

    private fun invokeMethod(name: String, args: Array<Any?>): Any? {
        val methods = pluginInstance.javaClass.methods.filter { it.name == name && it.getParameterTypes().size == args.size }
        for (method in methods) {
            try {
                method.isAccessible = true
                return method.invoke(pluginInstance, *args)
            } catch (_: Exception) {
                // Try the next overload.
            }
        }
        return null
    }

    private fun resolveInternal(mediaItem: MediaItem): String? {
        val argsCandidates = listOf(arrayOf<Any?>(mediaItem), arrayOf<Any?>(mediaItem.id))
        for (args in argsCandidates) {
            val result = invokeMethod("resolve", args)
                ?: invokeMethod("getUrl", args)
                ?: invokeMethod("loadLink", args)
                ?: continue
            return parseUrl(result)
        }
        return null
    }

    private fun resolveHeaders(mediaItem: MediaItem): Map<String, String> {
        val argsCandidates = listOf(arrayOf<Any?>(mediaItem), arrayOf<Any?>(mediaItem.id))
        for (args in argsCandidates) {
            val result = invokeMethod("headers", args)
                ?: invokeMethod("getHeaders", args)
                ?: continue
            return parseHeaders(result)
        }
        return emptyMap()
    }

    private fun parseList(result: Any): List<MediaItem> {
        return when (result) {
            is List<*> -> result.mapNotNull { parseOne(it) }
            is Array<*> -> result.mapNotNull { parseOne(it) }
            is JsonArray -> result.mapNotNull { parseOne(it) }
            is String -> parseJsonString(result)?.let { parseList(it) } ?: emptyList()
            is JsonObject -> listOfNotNull(parseOne(result))
            is MediaItem -> listOf(result)
            else -> emptyList()
        }
    }

    private fun parseOne(result: Any?): MediaItem? {
        if (result == null) return null
        if (result is MediaItem) return result

        val jsonObject = when (result) {
            is JsonObject -> result
            is String -> parseJsonString(result)?.jsonObject
            is Map<*, *> -> mapToJsonObject(result)
            else -> reflectToJsonObject(result)
        } ?: return null

        return jsonObjectToMediaItem(jsonObject)
    }

    private fun parseUrl(result: Any): String? {
        return when (result) {
            is String -> result.ifBlank { null }
            is JsonObject -> result["url"]?.jsonPrimitive?.contentOrNull
                ?: result["link"]?.jsonPrimitive?.contentOrNull
            is Map<*, *> -> (result["url"] ?: result["link"] ?: result["stream"])?.toString()
            else -> null
        }
    }

    private fun parseHeaders(result: Any): Map<String, String> {
        return when (result) {
            is Map<*, *> -> result.mapNotNull { (k, v) ->
                k?.toString()?.let { key -> v?.toString()?.let { value -> key to value } }
            }.toMap()
            is JsonObject -> result.mapNotNull { (k, v) ->
                v.jsonPrimitive.contentOrNull?.let { k to it }
            }.toMap()
            else -> emptyMap()
        }
    }

    private fun parseJsonString(text: String): JsonElement? {
        return try {
            json.parseToJsonElement(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun mapToJsonObject(map: Map<*, *>): JsonObject {
        return JsonObject(map.mapNotNull { (k, v) ->
            k?.toString()?.let { key -> key to anyToJsonElement(v) }
        }.toMap())
    }

    private fun reflectToJsonObject(instance: Any): JsonObject? {
        return try {
            val map = mutableMapOf<String, JsonElement>()
            instance.javaClass.methods.filter { it.name.startsWith("get") && it.getParameterTypes().size == 0 && it.name != "getClass" }
                .forEach { method ->
                    val key = method.name.removePrefix("get").replaceFirstChar { it.lowercaseChar() }
                    val value = method.invoke(instance)
                    map[key] = anyToJsonElement(value)
                }
            JsonObject(map)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("ReflectiveMediaSource", "Reflection failed: ${e.message}")
            null
        }
    }

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> mapToJsonObject(value)
            is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
            is Array<*> -> JsonArray(value.map { anyToJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun jsonObjectToMediaItem(json: JsonObject): MediaItem? {
        val id = json.string("id") ?: json.string("slug") ?: json.string("url") ?: return null
        val title = json.string("title") ?: json.string("name") ?: id
        val description = json.string("description") ?: json.string("synopsis") ?: ""
        val posterUrl = json.string("posterUrl") ?: json.string("poster") ?: json.string("cover") ?: json.string("image") ?: json.string("iconUrl")
        val backdropUrl = json.string("backdropUrl") ?: posterUrl
        val videoUrl = json.string("videoUrl") ?: json.string("url") ?: json.string("stream") ?: json.string("link")
        val year = json.string("year") ?: json.int("year")?.toString() ?: ""
        val rating = json.string("rating") ?: ""
        val subtitle = json.string("subtitle") ?: (json.array("tvTypes")?.joinToString(", ") { it.jsonPrimitive.contentOrNull ?: "" } ?: "")
        val duration = json.string("duration") ?: ""
        val isLive = json.boolean("isLive") ?: false
        val typeString = json.string("type") ?: ""
        val type = try {
            MediaItem.Type.valueOf(typeString.uppercase())
        } catch (_: Exception) {
            MediaItem.Type.MOVIE
        }
        val headers = json.obj("headers")?.let { parseHeaders(it) } ?: emptyMap()

        return MediaItem(
            id = id,
            title = title,
            subtitle = subtitle,
            description = description,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            year = year,
            duration = duration,
            rating = rating,
            videoUrl = videoUrl,
            headers = headers,
            isLive = isLive,
            type = type
        )
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = this[key]?.jsonObject
    private fun JsonObject.array(key: String): JsonArray? = this[key]?.jsonArray
}
