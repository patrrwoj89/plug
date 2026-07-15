package com.polishmediahub.app.data.plugin

import android.content.Context
import android.util.Log
import com.polishmediahub.app.data.local.PluginDao
import com.polishmediahub.app.data.local.PluginEntity
import com.polishmediahub.app.data.plugin.models.AniyomiExtension
import com.polishmediahub.app.data.plugin.models.CloudstreamPluginMetadata
import com.polishmediahub.app.data.plugin.models.InstallablePlugin
import com.polishmediahub.app.data.source.CloudstreamSource
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.data.source.WebMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PluginRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pluginDao: PluginDao,
    private val client: OkHttpClient,
    private val dynamicPluginLoader: DynamicPluginLoader,
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

    private val _availablePlugins = MutableStateFlow<List<InstallablePlugin>>(emptyList())
    val availablePlugins: StateFlow<List<InstallablePlugin>> = _availablePlugins

    private val loadedDynamicSources = mutableMapOf<String, Pair<MediaSource, File>>()

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
        upsertManifest(manifest, url)
    }

    suspend fun addPluginFromJson(jsonString: String) {
        val manifest = json.decodeFromString(PluginManifest.serializer(), jsonString)
        upsertManifest(manifest, "")
    }

    private suspend fun upsertManifest(manifest: PluginManifest, url: String) {
        val entity = PluginEntity(
            pluginId = manifest.id,
            name = manifest.name,
            manifestUrl = url,
            manifestJson = json.encodeToString(PluginManifest.serializer(), manifest),
            enabled = true,
            sortOrder = 0
        )
        pluginDao.upsert(entity)
    }

    suspend fun removePlugin(id: String) {
        unloadDynamicPlugin(id)
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

    /**
     * Fetches all repository indexes defined in enabled plugin manifests and exposes them as
     * installable plugin tiles via [availablePlugins].
     */
    suspend fun syncIndexes() = withContext(Dispatchers.IO) {
        val plugins = pluginDao.observeAll().first().filter { it.enabled }
        val all = mutableListOf<InstallablePlugin>()
        plugins.forEach { entity ->
            try {
                val manifest = json.decodeFromString(PluginManifest.serializer(), entity.manifestJson)
                manifest.sources.filter { it.enabled }.forEach { source ->
                    when (source.type) {
                        "cloudstream_repo" -> {
                            val repoUrl = source.config["url"] ?: return@forEach
                            all += fetchCloudstreamPlugins(repoUrl).map { it.toInstallable() }
                        }
                        "aniyomi_repo" -> {
                            val repoUrl = source.config["url"] ?: return@forEach
                            all += fetchAniyomiExtensions(repoUrl).map { it.toInstallable() }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "syncIndexes failed for ${entity.pluginId}: ${e.message}")
            }
        }
        _availablePlugins.value = all.distinctBy { it.id }
    }

    suspend fun fetchCloudstreamPlugins(repoUrl: String): List<CloudstreamPluginMetadata> = withContext(Dispatchers.IO) {
        try {
            val body = fetch(repoUrl) ?: return@withContext emptyList()
            val repo = json.decodeFromString(CloudstreamRepoIndex.serializer(), body)
            repo.pluginLists.flatMap { listUrl ->
                try {
                    val pluginsBody = fetch(listUrl) ?: return@flatMap emptyList()
                    json.decodeFromString(ListSerializer(CloudstreamPluginMetadata.serializer()), pluginsBody)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch plugin list $listUrl: ${e.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Cloudstream repo $repoUrl: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAniyomiExtensions(repoUrl: String): List<AniyomiExtension> = withContext(Dispatchers.IO) {
        try {
            val indexUrl = if (repoUrl.endsWith("/")) "${repoUrl}index.min.json" else "$repoUrl/index.min.json"
            val body = fetch(indexUrl) ?: return@withContext emptyList()
            json.decodeFromString(ListSerializer(AniyomiExtension.serializer()), body)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Aniyomi repo $repoUrl: ${e.message}")
            emptyList()
        }
    }

    /**
     * Downloads a binary plugin from [url] and persists it as an enabled plugin manifest.
     * [type] should be `cloudstream_binary` or `aniyomi_apk`.
     */
    suspend fun installBinaryPlugin(
        type: String,
        url: String,
        mainClass: String,
        name: String,
        iconUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        val pluginId = "${type}_${url.hashCode()}"
        val file = downloadPluginFile(url, pluginId)

        val manifest = PluginManifest(
            id = pluginId,
            name = name,
            version = "1.0",
            description = "Dynamically loaded $type plugin",
            sources = listOf(
                PluginSource(
                    type = type,
                    id = pluginId,
                    name = name,
                    enabled = true,
                    config = mapOf(
                        "url" to url,
                        "mainClass" to mainClass,
                        "iconUrl" to (iconUrl ?: ""),
                        "localFile" to file.absolutePath
                    )
                )
            )
        )
        upsertManifest(manifest, "")
    }

    fun loadAll(): List<MediaSource> = _activeSources.value

    private suspend fun refreshSources(entities: List<PluginEntity>) {
        val newDynamicKeys = mutableSetOf<String>()
        val sources = mutableListOf<MediaSource>()
        entities.forEach { entity ->
            try {
                val manifest = json.decodeFromString(PluginManifest.serializer(), entity.manifestJson)
                sources += applyPlugin(manifest, newDynamicKeys)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply plugin ${entity.pluginId}: ${e.message}")
            }
        }
        val uniqueSources = sources.distinct()
        val oldSources = mutex.withLock {
            val previous = previousSources
            previousSources = uniqueSources
            _activeSources.value = uniqueSources
            previous
        }

        // Dispose sources that are no longer active.
        oldSources.filterIsInstance<QuickJsMediaSource>().forEach { old ->
            if (old !in uniqueSources) old.dispose()
        }

        // Unload binary plugins that disappeared.
        val keysToRemove = loadedDynamicSources.keys - newDynamicKeys
        keysToRemove.forEach { unloadDynamicSource(it) }
    }

    private suspend fun applyPlugin(manifest: PluginManifest, newDynamicKeys: MutableSet<String>): List<MediaSource> {
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
                "cloudstream_repo", "aniyomi_repo" -> {
                    // Repository indexes are handled by syncIndexes() and surfaced as installable plugins.
                    null
                }
                "cloudstream_binary", "aniyomi_apk" -> {
                    loadDynamicSource(source, newDynamicKeys)
                }
                "stremio", "iptv" -> null
                "quickjs" -> {
                    val quickSource = quickJsMediaSourceProvider.get()
                    quickSource.configure(source.config["script"] ?: "")
                    quickSource
                }
                else -> null
            }
        }.distinct()
    }

    private suspend fun loadDynamicSource(source: PluginSource, newDynamicKeys: MutableSet<String>): MediaSource? = withContext(Dispatchers.IO) {
        val pluginUrl = source.config["url"] ?: return@withContext null
        val mainClass = source.config["mainClass"] ?: return@withContext null
        val pluginFile = if (source.config["localFile"].isNullOrBlank()) {
            downloadPluginFile(pluginUrl, source.id)
        } else {
            File(source.config["localFile"]!!)
        }

        val cacheKey = "${source.id}:${pluginFile.absolutePath}:$mainClass"
        newDynamicKeys.add(cacheKey)

        val sourceInstance = dynamicPluginLoader.loadPlugin(pluginFile, mainClass)
        if (sourceInstance != null) {
            loadedDynamicSources[cacheKey] = sourceInstance to pluginFile
        }
        sourceInstance
    }

    private suspend fun downloadPluginFile(url: String, fileName: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "plugins").apply { mkdirs() }
        val file = File(dir, "$fileName.apk")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Download failed: ${response.code}")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Empty response body")
        }
        file
    }

    private suspend fun unloadDynamicPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        loadedDynamicSources.keys.filter { it.startsWith("$pluginId:") }.forEach { unloadDynamicSource(it) }
    }

    private fun unloadDynamicSource(key: String) {
        val (_, file) = loadedDynamicSources.remove(key) ?: return
        dynamicPluginLoader.unloadPlugin(file)
    }

    private suspend fun fetch(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().body?.string()
        } catch (_: Exception) {
            null
        }
    }

    private fun CloudstreamPluginMetadata.toInstallable(): InstallablePlugin.Cloudstream =
        InstallablePlugin.Cloudstream(
            id = internalName ?: (name + version),
            name = name,
            url = url,
            iconUrl = iconUrl,
            description = description,
            version = version.toString(),
            fileSize = fileSize,
            language = language,
            tvTypes = tvTypes.orEmpty(),
            authors = authors.orEmpty()
        )

    private fun AniyomiExtension.toInstallable(): InstallablePlugin.Aniyomi =
        InstallablePlugin.Aniyomi(
            id = pkg,
            name = name,
            url = apk,
            iconUrl = null,
            description = null,
            version = version,
            fileSize = null,
            pkg = pkg,
            lang = lang,
            nsfw = nsfw
        )

    @kotlinx.serialization.Serializable
    private data class CloudstreamRepoIndex(
        val name: String? = null,
        val description: String? = null,
        @kotlinx.serialization.SerialName("manifestVersion") val manifestVersion: Int = 1,
        @kotlinx.serialization.SerialName("pluginLists") val pluginLists: List<String> = emptyList()
    )

    private companion object {
        private const val TAG = "PluginRepository"
    }
}
