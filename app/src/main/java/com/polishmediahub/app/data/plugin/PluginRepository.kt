package com.polishmediahub.app.data.plugin

import com.polishmediahub.app.data.local.PluginDao
import com.polishmediahub.app.data.local.PluginEntity
import com.polishmediahub.app.data.source.CloudstreamSource
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.data.source.WebMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Provider
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _activeSources = MutableStateFlow<List<MediaSource>>(emptyList())
    val activeSources: StateFlow<List<MediaSource>> = _activeSources
    private var previousSources: List<MediaSource> = emptyList()

    val plugins: Flow<List<PluginEntity>> = pluginDao.observeAll()

    init {
        scope.launch {
            pluginDao.observeAll().collect { entities ->
                refreshSources(entities.filter { it.enabled })
            }
        }
    }

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

    fun loadAll(): List<MediaSource> = _activeSources.value

    private suspend fun refreshSources(entities: List<PluginEntity>) {
        val sources = entities.flatMap { entity ->
            try {
                val manifest = json.decodeFromString(PluginManifest.serializer(), entity.manifestJson)
                applyPlugin(manifest)
            } catch (e: Exception) {
                android.util.Log.w("PluginRepository", "Failed to apply plugin ${entity.pluginId}: ${e.message}")
                emptyList()
            }
        }
        val uniqueSources = sources.distinct()
        val oldSources = mutex.withLock {
            val previous = previousSources
            previousSources = uniqueSources
            _activeSources.value = uniqueSources
            previous
        }
        oldSources.filterIsInstance<QuickJsMediaSource>().forEach { old ->
            if (old !in uniqueSources) old.dispose()
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
