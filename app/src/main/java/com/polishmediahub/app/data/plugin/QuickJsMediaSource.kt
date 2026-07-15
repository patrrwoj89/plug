package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QuickJsMediaSource @Inject constructor(
    private val engine: QuickJsEngine
) : MediaSource {

    override val id: String = "quickjs"
    override val name: String = "QuickJS Plugin"
    override val isConfigurable: Boolean = true

    private var script: String = ""

    fun configure(script: String) {
        if (this.script != script) {
            this.script = script
            engine.close()
            engine.init()
            withContextOrEmpty { engine.evaluate(script) }
        }
    }

    fun dispose() {
        script = ""
        engine.close()
    }

    override suspend fun isAvailable(): Boolean = script.isNotBlank()

    override suspend fun featured(): List<MediaItem> =
        withContextIO { callList("featured") } ?: emptyList()

    override suspend fun categories(): List<Category> =
        withContextIO {
            callList("categories")?.let { items ->
                items.groupBy { it.type }
                    .map { (type, group) ->
                        Category(
                            id = "quickjs:${type.name.lowercase()}",
                            name = type.name,
                            items = group
                        )
                    }
            }
        } ?: emptyList()

    override suspend fun search(query: String): List<MediaItem> =
        withContextIO { callList("search", query) } ?: emptyList()

    override suspend fun byId(id: String): MediaItem? =
        withContextIO { callMap("byId", id)?.let { mapToMediaItem(it) } }

    override suspend fun resolve(mediaItem: MediaItem): String? =
        withContextIO {
            val result = call("resolve", mediaItem.id)
            when (result) {
                is String -> result.ifBlank { null }
                is Map<*, *> -> result["url"] as? String ?: result["videoUrl"] as? String
                is JSObject -> result.getStringProperty("url") ?: result.getStringProperty("videoUrl")
                else -> null
            }?.ifBlank { null }
        }

    @Suppress("UNCHECKED_CAST")
    private suspend fun callList(functionName: String, vararg args: Any?): List<MediaItem>? = withContextIO {
        val result = call(functionName, *args) ?: return@withContextIO null
        val rawItems: List<Any?> = when (result) {
            is JSArray -> result.toArray()
            is Array<*> -> result.toList()
            is List<*> -> result
            is JSObject -> listOf(result)
            else -> return@withContextIO null
        }
        rawItems.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> mapToMediaItem(item)
                is JSObject -> mapToMediaItem(item.toMap())
                else -> null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun callMap(functionName: String, vararg args: Any?): Map<String, Any?>? = withContextIO {
        val result = call(functionName, *args) ?: return@withContextIO null
        when (result) {
            is Map<*, *> -> result as? Map<String, Any?>
            is JSObject -> result.toMap() as? Map<String, Any?>
            else -> null
        }
    }

    private suspend fun call(functionName: String, vararg args: Any?): Any? = withContextIO {
        try {
            engine.evaluate("$functionName(${args.joinToString(",") { toJsLiteral(it) }})")
        } catch (_: Exception) {
            null
        }
    }

    private fun withContextOrEmpty(block: () -> Any?) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    private suspend fun <T> withContextIO(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }

    private fun toJsLiteral(value: Any?): String {
        val quote = Char(34).toString()
        val escapedQuote = "\\" + quote
        val escapedBackslash = "\\\\"
        return when (value) {
            null -> "null"
            is String -> quote + value.replace("\\", escapedBackslash).replace(quote, escapedQuote) + quote
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> quote + value.toString().replace("\\", escapedBackslash).replace(quote, escapedQuote) + quote
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToMediaItem(map: Map<*, *>): MediaItem? {
        if (map["id"] == null) return null
        val headersMap = map["headers"]
        val headers = when (headersMap) {
            is Map<*, *> -> headersMap.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: v?.toString() ?: return@mapNotNull null
                key to value
            }.toMap()
            is JSObject -> headersMap.toMap().mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: v?.toString() ?: return@mapNotNull null
                key to value
            }.toMap()
            else -> emptyMap()
        }
        val subtitleHeadersMap = map["subtitleHeaders"]
        val subtitleHeaders = when (subtitleHeadersMap) {
            is Map<*, *> -> subtitleHeadersMap.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: v?.toString() ?: return@mapNotNull null
                key to value
            }.toMap()
            is JSObject -> subtitleHeadersMap.toMap().mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: v?.toString() ?: return@mapNotNull null
                key to value
            }.toMap()
            else -> emptyMap()
        }
        return MediaItem(
            id = map["id"] as String,
            title = map["title"] as? String ?: "",
            type = (map["type"] as? String)?.let {
                try {
                    MediaItem.Type.valueOf(it.uppercase())
                } catch (_: Exception) {
                    MediaItem.Type.MOVIE
                }
            } ?: MediaItem.Type.MOVIE,
            posterUrl = map["posterUrl"] as? String,
            backdropUrl = map["backdropUrl"] as? String,
            description = map["description"] as? String ?: "",
            year = (map["year"] as? Number)?.toInt()?.toString() ?: (map["year"] as? String) ?: "",
            season = (map["season"] as? Number)?.toInt(),
            episode = (map["episode"] as? Number)?.toInt(),
            videoUrl = map["videoUrl"] as? String ?: map["url"] as? String,
            headers = headers,
            subtitleUrl = map["subtitleUrl"] as? String,
            subtitleHeaders = subtitleHeaders,
            subtitleLanguage = (map["subtitleLanguage"] as? String) ?: "pl"
        )
    }
}
