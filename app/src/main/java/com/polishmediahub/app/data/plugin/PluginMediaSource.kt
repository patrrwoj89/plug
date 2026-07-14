package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginMediaSource @Inject constructor(
    private val repository: PluginRepository
) : MediaSource {

    override val id: String = "plugin"
    override val name: String = "Plugins"
    override val isConfigurable: Boolean = true

    override suspend fun isAvailable(): Boolean = repository.loadAll().isNotEmpty()

    override suspend fun featured(): List<MediaItem> =
        parallelMap { it.featured() }

    override suspend fun categories(): List<Category> =
        parallelMap { it.categories() }

    override suspend fun search(query: String): List<MediaItem> =
        parallelMap { it.search(query) }

    override suspend fun byId(id: String): MediaItem? {
        for (source in repository.loadAll().filter { it.isAvailable() }) {
            try {
                source.byId(id)?.let { return it }
            } catch (e: Exception) {
                android.util.Log.w("PluginMediaSource", "byId failed for ${source.id}: ${e.message}")
            }
        }
        return null
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        for (source in repository.loadAll().filter { it.isAvailable() }) {
            try {
                source.resolve(mediaItem)?.let { return it }
            } catch (e: Exception) {
                android.util.Log.w("PluginMediaSource", "resolve failed for ${source.id}: ${e.message}")
            }
        }
        return null
    }

    private suspend inline fun <T> parallelMap(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        crossinline block: suspend (MediaSource) -> List<T>
    ): List<T> = coroutineScope {
        repository.loadAll()
            .filter { it.isAvailable() }
            .map { source ->
                async(dispatcher) {
                    try {
                        block(source)
                    } catch (e: Exception) {
                        android.util.Log.w("PluginMediaSource", "call failed for ${source.id}: ${e.message}")
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
    }
}
