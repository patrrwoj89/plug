package com.polishmediahub.app.data.source

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudstreamSource @Inject constructor(
    private val client: OkHttpClient
) : MediaSource {

    override val id: String = "cloudstream"
    override val name: String = "Cloudstream repositories"
    override val isConfigurable: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var repoUrls: List<String> = emptyList()

    fun configure(repoUrlList: String) {
        repoUrls = repoUrlList.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() && it.startsWith("http") }
    }

    override suspend fun isAvailable(): Boolean = repoUrls.isNotEmpty()

    override suspend fun featured(): List<MediaItem> = repoUrls
        .mapNotNull { loadRepoPlugins(it) }
        .flatten()
        .take(20)

    override suspend fun categories(): List<Category> = coroutineScope {
        repoUrls.map { repoUrl ->
            async(Dispatchers.IO) {
                try {
                    val plugins = loadRepoPlugins(repoUrl)
                    Category(
                        id = "cloudstream:$repoUrl",
                        name = "Cloudstream: $repoUrl",
                        items = plugins ?: emptyList()
                    )
                } catch (e: Exception) {
                    android.util.Log.w("CloudstreamSource", "categories failed for $repoUrl: ${e.message}")
                    Category(id = "cloudstream:$repoUrl", name = "Cloudstream: $repoUrl", items = emptyList())
                }
            }
        }.awaitAll()
    }

    override suspend fun search(query: String): List<MediaItem> =
        featured().filter { it.title.contains(query, ignoreCase = true) }

    override suspend fun byId(id: String): MediaItem? =
        featured().find { it.id == id }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val internalName = mediaItem.id.removePrefix("cloudstream:")
        return repoUrls.firstNotNullOfOrNull { repoUrl ->
            try {
                val body = fetch(repoUrl) ?: return@firstNotNullOfOrNull null
                val repo = json.decodeFromString(CloudstreamRepo.serializer(), body)
                repo.pluginLists?.firstNotNullOfOrNull { listUrl ->
                    try {
                        val pluginsBody = fetch(listUrl) ?: return@firstNotNullOfOrNull null
                        val plugins = json.decodeFromString(ListSerializer(CloudstreamPlugin.serializer()), pluginsBody)
                        plugins.find { it.internalName == internalName }?.url
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun loadRepoPlugins(repoUrl: String): List<MediaItem>? = coroutineScope {
        try {
            val body = fetch(repoUrl) ?: return@coroutineScope null
            val repo = json.decodeFromString(CloudstreamRepo.serializer(), body)
            repo.pluginLists?.map { listUrl ->
                async(Dispatchers.IO) {
                    try {
                        val pluginsBody = fetch(listUrl) ?: return@async emptyList()
                        val plugins = json.decodeFromString(ListSerializer(CloudstreamPlugin.serializer()), pluginsBody)
                        plugins.map { it.toMediaItem(repoUrl) }
                    } catch (e: Exception) {
                        android.util.Log.w("CloudstreamSource", "plugin list failed $listUrl: ${e.message}")
                        emptyList()
                    }
                }
            }?.awaitAll()?.flatten()
        } catch (e: Exception) {
            android.util.Log.w("CloudstreamSource", "repo failed $repoUrl: ${e.message}")
            null
        }
    }

    private suspend fun fetch(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }
        } catch (e: Exception) {
            android.util.Log.w("CloudstreamSource", "fetch failed $url: ${e.message}")
            null
        }
    }

    private fun CloudstreamPlugin.toMediaItem(repoUrl: String): MediaItem = MediaItem(
        id = "cloudstream:$internalName",
        title = name,
        description = description ?: "",
        subtitle = tvTypes?.joinToString(", ") ?: language ?: "",
        year = version.toString(),
        posterUrl = iconUrl,
        backdropUrl = iconUrl,
        videoUrl = url,
        type = MediaItem.Type.MOVIE
    )
}

@Serializable
data class CloudstreamRepo(
    val name: String? = null,
    val description: String? = null,
    @SerialName("manifestVersion") val manifestVersion: Int = 1,
    @SerialName("pluginLists") val pluginLists: List<String>? = null
)

@Serializable
data class CloudstreamPlugin(
    val url: String,
    val status: Int,
    val version: Int,
    @SerialName("apiVersion") val apiVersion: Int,
    val name: String,
    @SerialName("internalName") val internalName: String,
    val authors: List<String>? = null,
    val description: String? = null,
    @SerialName("repositoryUrl") val repositoryUrl: String? = null,
    @SerialName("tvTypes") val tvTypes: List<String>? = null,
    val language: String? = null,
    @SerialName("iconUrl") val iconUrl: String? = null,
    @SerialName("isAdult") val isAdult: Boolean? = null
)
