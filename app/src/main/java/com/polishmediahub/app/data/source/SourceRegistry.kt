package com.polishmediahub.app.data.source
import com.polishmediahub.app.BuildConfig

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-source bridge: aggregates results from all registered [MediaSource] implementations.
 * Enables "federated" search and category browsing across Kodi, Stremio, IPTV, etc.
 * All network-bound calls are parallelized on [Dispatchers.IO] and isolated so one failing
 * source cannot block the UI.
 */
@Singleton
class SourceRegistry @Inject constructor(
    sources: Set<@JvmSuppressWildcards MediaSource>
) {
    private val sourceMap = sources.associateBy { it.id }

    val all: List<MediaSource> get() = sourceMap.values.toList()

    fun source(id: String): MediaSource? = sourceMap[id]

    suspend fun searchAll(query: String, dispatcher: CoroutineDispatcher = Dispatchers.IO): Map<String, List<MediaItem>> = coroutineScope {
        all
            .filter { it.isAvailable() }
            .map { source ->
                async(dispatcher) {
                    try {
                        source.id to source.search(query)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SourceRegistry", "search failed for ${source.id}: ${e.message}")
                        source.id to emptyList()
                    }
                }
            }
            .awaitAll()
            .toMap()
    }

    suspend fun categoriesAll(dispatcher: CoroutineDispatcher = Dispatchers.IO): List<Category> = coroutineScope {
        all
            .filter { it.isAvailable() }
            .map { source ->
                async(dispatcher) {
                    try {
                        source.categories()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SourceRegistry", "categories failed for ${source.id}: ${e.message}")
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
    }

    suspend fun featuredAll(dispatcher: CoroutineDispatcher = Dispatchers.IO): List<MediaItem> = coroutineScope {
        all
            .filter { it.isAvailable() }
            .map { source ->
                async(dispatcher) {
                    try {
                        source.featured()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SourceRegistry", "featured failed for ${source.id}: ${e.message}")
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
    }
}
