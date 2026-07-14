package com.tvhub.skeleton.data.source

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
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

    override suspend fun featured(): List<MediaItem> = emptyList()

    override suspend fun categories(): List<Category> {
        return repoUrls.flatMap { repoUrl ->
            try {
                val body = fetch(repoUrl) ?: return@flatMap emptyList()
                val repo = json.decodeFromString(CloudstreamRepo.serializer(), body)
                repo.pluginLists?.flatMap { pluginListUrl ->
                    try {
                        val pluginsBody = fetch(pluginListUrl) ?: return@flatMap emptyList()
                        val plugins = json.decodeFromString(ListSerializer(CloudstreamPlugin.serializer()), pluginsBody)
                        plugins.map { plugin ->
                            Category(
                                id = "cloudstream:${plugin.internalName}",
                                name = plugin.name,
                                items = emptyList()
                            )
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun search(query: String): List<MediaItem> = emptyList()

    override suspend fun byId(id: String): MediaItem? = null

    override suspend fun resolve(mediaItem: MediaItem): String? = null

    private suspend fun fetch(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().body?.string()
        } catch (_: Exception) {
            null
        }
    }
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
