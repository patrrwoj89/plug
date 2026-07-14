package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickJsMediaSource @Inject constructor(
    private val quickJsEngine: QuickJsEngine
) : MediaSource {

    override val id: String = "quickjs"
    override val name: String = "QuickJS Plugin"
    override val isConfigurable: Boolean = true

    private var script: String = ""

    fun configure(script: String) {
        this.script = script
        quickJsEngine.close()
        quickJsEngine.init()
        quickJsEngine.evaluate(script)
    }

    override suspend fun isAvailable(): Boolean = script.isNotBlank()

    override suspend fun featured(): List<MediaItem> =
        callList("featured") ?: emptyList()

    override suspend fun categories(): List<Category> =
        emptyList()

    override suspend fun search(query: String): List<MediaItem> =
        callList("search", query) ?: emptyList()

    override suspend fun byId(id: String): MediaItem? =
        callMap("byId", id)?.let { mapToMediaItem(it) }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val url = call("resolve", mediaItem.id) as? String
        return url?.ifBlank { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun callList(functionName: String, vararg args: Any?): List<MediaItem>? {
        val result = call(functionName, *args) ?: return null
        return when (result) {
            is List<*> -> result.filterIsInstance<Map<*, *>>().mapNotNull { mapToMediaItem(it) }
            is Array<*> -> result.filterIsInstance<Map<*, *>>().mapNotNull { mapToMediaItem(it) }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun callMap(functionName: String, vararg args: Any?): Map<String, Any?>? {
        val result = call(functionName, *args) ?: return null
        return result as? Map<String, Any?>
    }

    private fun call(functionName: String, vararg args: Any?): Any? {
        return try {
            quickJsEngine.evaluate("$functionName(${args.joinToString(",") { toJsLiteral(it) }})")
        } catch (_: Exception) {
            null
        }
    }

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
            year = (map["year"] as? Number)?.toInt()?.toString() ?: (map["year"] as? String) ?: ""
        )
    }
}
