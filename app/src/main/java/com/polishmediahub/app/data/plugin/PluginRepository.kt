package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.local.PluginDao
import com.polishmediahub.app.data.local.PluginEntity
import com.polishmediahub.app.data.source.CloudstreamSource
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.data.source.WebMediaSource
import javax.inject.Provider
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginRepository @Inject constructor(
    private val pluginDao: PluginDao,
    private val client: OkHttpClient,
    private val kodiMediaSource: KodiMediaSource,
    private val webMediaSource: WebMediaSource,
    private val cloudstreamSource: CloudstreamSource,
    private val quickJsMediaSourceProvider: Provider<QuickJsMediaSource>
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val plugins: Flow<List<PluginEntity>> = pluginDao.observeAll()

    suspend fun addPluginFromUrl(url: String) {
        val body = fetch(url) ?: throw IllegalStateException("Could not fetch plugin manifest")
        val manifest = json.decodeFromString(PluginManifest.serializer(), body)
        val entity = PluginEntity(
            pluginId = manifest.id,
            name = manifest.name,
            manifestUrl = url,
            manifestJson = body,
            enabled = true,
            sortOrder = 0
        )
        pluginDao.upsert(entity)
        applyPlugin(manifest)
    }

    suspend fun addPluginFromJson(jsonString: String) {
        val manifest = json.decodeFromString(PluginManifest.serializer(), jsonString)
        val entity = PluginEntity(
            pluginId = manifest.id,
            name = manifest.name,
            manifestUrl = "",
            manifestJson = jsonString,
            enabled = true,
            sortOrder = 0
        )
        pluginDao.upsert(entity)
        applyPlugin(manifest)
    }

    suspend fun removePlugin(id: String) {
        pluginDao.delete(id)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        pluginDao.getById(id)?.let {
            pluginDao.upsert(it.copy(enabled = enabled))
        }
    }

    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            pluginDao.updateOrder(id, index)
        }
    }

    suspend fun checkUpdates(): Int {
        val plugins = pluginDao.observeAll().first()
        var updated = 0
        plugins.forEach { entity ->
            val url = entity.manifestUrl
            if (url.isBlank()) return@forEach
            val body = fetch(url) ?: return@forEach
            if (body != entity.manifestJson) {
                try {
                    val manifest = json.decodeFromString(PluginManifest.serializer(), body)
                    pluginDao.upsert(
                        entity.copy(
                            name = manifest.name,
                            manifestJson = body
                        )
                    )
                    updated++
                } catch (_: Exception) {
                }
            }
        }
        return updated
    }

    suspend fun loadAll(): List<MediaSource> {
        val entities = pluginDao.observeAll().first().filter { it.enabled }
        return entities.flatMap { entity ->
            try {
                val manifest = json.decodeFromString(PluginManifest.serializer(), entity.manifestJson)
                applyPlugin(manifest)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun applyPlugin(manifest: PluginManifest): List<MediaSource> {
        return manifest.sources.filter { it.enabled }.mapNotNull { source ->
            when (source.type) {
                "kodi" -> {
                    source.config["url"]?.let { kodiMediaSource.configure(it) }
                    kodiMediaSource
                }
                "web" -> {
                    source.config["json"]?.let { webMediaSource.configure(it) }
                    webMediaSource
                }
                "cloudstream" -> {
                    source.config["repos"]?.let { cloudstreamSource.configure(it) }
                    cloudstreamSource
                }
                "stremio" -> {
                    // Applied via ApiConfigRepository in current flow
                    null
                }
                "iptv" -> {
                    // Applied via ApiConfigRepository
                    null
                }
                "quickjs" -> {
                    val quickSource = quickJsMediaSourceProvider.get()
                    quickSource.configure(source.config["script"] ?: "")
                    quickSource
                }
                else -> null
            }
        }.distinct()
    }

    private suspend fun fetch(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().body?.string()
        } catch (_: Exception) {
            null
        }
    }
}

@Singleton
class PluginMediaSource @Inject constructor(
    private val repository: PluginRepository
) : MediaSource {

    override val id: String = "plugin"
    override val name: String = "Plugins"
    override val isConfigurable: Boolean = true

    override suspend fun isAvailable(): Boolean = repository.loadAll().isNotEmpty()

    override suspend fun featured(): List<MediaItem> =
        repository.loadAll().flatMap { it.featured() }

    override suspend fun categories(): List<Category> =
        repository.loadAll().flatMap { it.categories() }

    override suspend fun search(query: String): List<MediaItem> =
        repository.loadAll().flatMap { it.search(query) }

    override suspend fun byId(id: String): MediaItem? =
        repository.loadAll().firstNotNullOfOrNull { it.byId(id) }

    override suspend fun resolve(mediaItem: MediaItem): String? =
        repository.loadAll().firstNotNullOfOrNull { it.resolve(mediaItem) }
}
