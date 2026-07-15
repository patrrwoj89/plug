package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.ContentFilter
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginMediaSource @Inject constructor(
    private val repository: PluginRepository,
    private val profileRepository: ProfileRepository
) : MediaSource {

    override val id: String = "plugin"
    override val name: String = "Plugins"
    override val isConfigurable: Boolean = true

    override suspend fun isAvailable(): Boolean = repository.loadAll().isNotEmpty()

    private suspend fun profile() = profileRepository.currentProfile.first()

    override suspend fun featured(): List<MediaItem> =
        ContentFilter.filter(parallelMap { it.featured() }, profile())

    override suspend fun categories(): List<Category> =
        ContentFilter.filterCategories(parallelMap { it.categories() }, profile())

    override suspend fun search(query: String): List<MediaItem> =
        ContentFilter.filter(parallelMap { it.search(query) }, profile())

    override suspend fun byId(id: String): MediaItem? {
        for (source in repository.loadAll().filter { it.isAvailable() }) {
            try {
                source.byId(id)
                    ?.takeIf { ContentFilter.isAllowed(it, profile()) }
                    ?.let { return it }
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
