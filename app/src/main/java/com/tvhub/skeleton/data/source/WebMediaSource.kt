package com.tvhub.skeleton.data.source

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebMediaSource @Inject constructor(
    private val client: OkHttpClient
) : MediaSource {

    override val id: String = "web"
    override val name: String = "Web sources"
    override val isConfigurable: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var configs: List<WebSourceConfig> = emptyList()

    fun configure(configJson: String) {
        configs = try {
            json.decodeFromString(ListSerializer(WebSourceConfig.serializer()), configJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun isAvailable(): Boolean = configs.isNotEmpty()

    override suspend fun featured(): List<MediaItem> =
        configs.firstOrNull()?.let { scrape(it) } ?: emptyList()

    override suspend fun categories(): List<Category> =
        configs.map { config ->
            Category("web:${config.id}", config.name, scrape(config))
        }

    override suspend fun search(query: String): List<MediaItem> =
        configs.flatMap { scrape(it, query) }

    override suspend fun byId(id: String): MediaItem? =
        search("").find { it.id == id }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val config = configs.find { mediaItem.id.startsWith("web:${it.id}:") } ?: return null
        val relativeId = mediaItem.id.removePrefix("web:${config.id}:")
        if (config.itemUrlTemplate.isBlank()) return mediaItem.videoUrl
        val url = config.itemUrlTemplate.replace("{id}", relativeId)
        val html = fetch(url) ?: return mediaItem.videoUrl
        return Jsoup.parse(html).select(config.streamSelector).firstOrNull()?.attr(config.streamAttribute)
            ?: mediaItem.videoUrl
    }

    private suspend fun scrape(config: WebSourceConfig, query: String? = null): List<MediaItem> {
        val url = if (query != null && config.searchUrlTemplate.isNotBlank()) {
            config.searchUrlTemplate.replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))
        } else {
            config.catalogUrl
        }
        val html = fetch(url) ?: return emptyList()
        val doc = Jsoup.parse(html, url)
        return doc.select(config.itemSelector).mapNotNull { element ->
            try {
                val title = element.select(config.titleSelector).firstOrNull()?.text() ?: return@mapNotNull null
                val relativeHref = element.select(config.linkSelector).firstOrNull()?.attr("href") ?: ""
                val href = if (relativeHref.startsWith("http")) relativeHref else config.baseUrl.removeSuffix("/") + "/" + relativeHref.removePrefix("/")
                val poster = element.select(config.posterSelector).firstOrNull()?.attr(config.posterAttribute)
                    ?.let { resolveUrl(config.baseUrl, it) }
                MediaItem(
                    id = "web:${config.id}:${java.net.URLEncoder.encode(href, "UTF-8")}",
                    title = title,
                    subtitle = element.select(config.yearSelector).firstOrNull()?.text() ?: "",
                    description = element.select(config.descriptionSelector).firstOrNull()?.text() ?: "",
                    posterUrl = poster,
                    backdropUrl = poster,
                    year = element.select(config.yearSelector).firstOrNull()?.text() ?: "",
                    videoUrl = href,
                    type = MediaItem.Type.MOVIE
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun resolveUrl(base: String, path: String): String = when {
        path.startsWith("http") -> path
        path.startsWith("/") -> base.removeSuffix("/") + path
        else -> base.removeSuffix("/") + "/" + path
    }

    private suspend fun fetch(url: String): String? {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().body?.string()
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
@SerialName("WebSourceConfig")
data class WebSourceConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val catalogUrl: String = "",
    val searchUrlTemplate: String = "",
    val itemUrlTemplate: String = "",
    val itemSelector: String = ".item",
    val titleSelector: String = ".title",
    val linkSelector: String = "a",
    val posterSelector: String = "img",
    val posterAttribute: String = "src",
    val yearSelector: String = ".year",
    val descriptionSelector: String = ".description",
    val streamSelector: String = "video",
    val streamAttribute: String = "src"
)
