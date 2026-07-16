package com.polishmediahub.app.data.source

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.plugin.QuickJsEngine
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WebMediaSource @Inject constructor(
    private val client: OkHttpClient,
    private val quickJsEngineProvider: Provider<QuickJsEngine>,
    private val cookieJar: MemoryCookieJar,
    private val apiConfigRepository: ApiConfigRepository
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

    override suspend fun byId(id: String): MediaItem? {
        val config = configs.find { id.startsWith("web:${it.id}:") } ?: return null
        val relativeId = id.removePrefix("web:${config.id}:")
        val url = try {
            URLDecoder.decode(relativeId, "UTF-8")
        } catch (_: Exception) {
            relativeId
        }
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return null
        }

        val html = fetch(url, config.headers) ?: return null
        val doc = Jsoup.parse(html, url)
        val title = doc.select(config.titleSelector).firstOrNull()?.text()?.takeIf { it.isNotBlank() } ?: return null
        val description = doc.select(config.descriptionSelector).firstOrNull()?.text() ?: ""
        val year = doc.select(config.yearSelector).firstOrNull()?.text() ?: ""
        val rawPoster = doc.select(config.posterSelector).firstOrNull()?.attr(config.posterAttribute)
        val poster = rawPoster?.let { resolveUrl(config.baseUrl, it) }

        val item = MediaItem(
            id = id,
            title = title,
            subtitle = year,
            description = description,
            posterUrl = poster,
            backdropUrl = poster,
            year = year,
            videoUrl = url,
            headers = config.headers,
            type = MediaItem.Type.MOVIE
        )
        val resolvedItem = resolveItem(item)
        return resolvedItem.copy(
            headers = headersWithCookies(resolvedItem.videoUrl.orEmpty(), resolvedItem.headers)
        )
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = resolveStream(mediaItem).url

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        val result = resolveStream(mediaItem)
        return if (result.url.isNotBlank() && result.url != mediaItem.videoUrl) {
            mediaItem.copy(videoUrl = result.url, headers = mediaItem.headers + result.headers)
        } else {
            mediaItem
        }
    }

    private data class StreamResult(val url: String, val headers: Map<String, String>)

    private suspend fun resolveStream(mediaItem: MediaItem): StreamResult {
        val config = configs.find { mediaItem.id.startsWith("web:${it.id}:") }
            ?: return StreamResult(mediaItem.videoUrl.orEmpty(), emptyMap())
        val relativeId = mediaItem.id.removePrefix("web:${config.id}:")
        if (config.itemUrlTemplate.isBlank()) return StreamResult(mediaItem.videoUrl.orEmpty(), emptyMap())
        val url = config.itemUrlTemplate.replace("{id}", relativeId)

        val useCloudflare = runCatching { apiConfigRepository.useCloudflareBypass.first() }.getOrDefault(false)
        val workerUrl = runCatching { apiConfigRepository.cloudflareWorkerUrl.first() }.getOrDefault("").trim()
        val authToken = runCatching { apiConfigRepository.cloudflareAuthToken.first() }.getOrDefault("").trim()

        if (useCloudflare && workerUrl.isNotBlank() && authToken.isNotBlank()) {
            val cloudResult = resolveViaCloudflare(workerUrl, authToken, url, config.headers)
            if (cloudResult != null) {
                if (BuildConfig.DEBUG) Log.d("WebMediaSource", "Resolved stream via Cloudflare worker")
                return cloudResult
            }
            if (BuildConfig.DEBUG) Log.d("WebMediaSource", "Cloudflare worker failed, falling back to local resolver")
        }

        val localUrl = resolveLocal(url, config, mediaItem)
        return StreamResult(localUrl ?: mediaItem.videoUrl.orEmpty(), emptyMap())
    }

    private suspend fun resolveViaCloudflare(
        workerUrl: String,
        authToken: String,
        targetUrl: String,
        headers: Map<String, String>
    ): StreamResult? {
        return try {
            val headersJson = json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                headers
            )
            val query = buildString {
                append("?url=")
                append(URLEncoder.encode(targetUrl, "UTF-8"))
                if (headers.isNotEmpty()) {
                    append("&headers=")
                    append(URLEncoder.encode(headersJson, "UTF-8"))
                }
            }
            val request = Request.Builder()
                .url("${workerUrl.removeSuffix("/")}/resolve$query")
                .header("X-Hub-Token", authToken)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string()
                if (body.isNullOrBlank()) return null
                val parsed = json.decodeFromString(CloudflareResponse.serializer(), body)
                if (parsed.streamUrl.isBlank()) return null
                StreamResult(parsed.streamUrl, parsed.headers)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("WebMediaSource", "Cloudflare resolve failed: ${e.message}")
            null
        }
    }

    private suspend fun resolveLocal(url: String, config: WebSourceConfig, mediaItem: MediaItem): String? {
        val html = fetch(url, config.headers) ?: return null
        val doc = Jsoup.parse(html, url)

        // CDA.pl native decoder
        if (url.contains("cda.pl", ignoreCase = true)) {
            val encoded = doc.selectFirst("[data-video-id]")?.attr("data-video-id")?.trim()
            if (!encoded.isNullOrBlank()) {
                val decoded = CdaDecoder.decode(encoded)
                if (decoded.startsWith("http", ignoreCase = true)) return decoded
            }
        }

        val direct = doc.select(config.streamSelector).firstOrNull()?.attr(config.streamAttribute)
        if (!direct.isNullOrBlank()) return resolveUrl(config.baseUrl, direct)

        // Try to unpack P.A.C.K.E.R obfuscated scripts and extract a media URL
        if (html.contains("eval(function(p,a,c,k,")) {
            val unpacked = JsUnpacker.unpack(html)
            val stream = findMediaUrl(unpacked)
            if (!stream.isNullOrBlank()) return resolveUrl(config.baseUrl, stream)
        }

        if (config.jsScript.isNotBlank()) {
            return runJsResolver(config.jsScript, url, mediaItem, config.headers)
        }

        return null
    }

    private suspend fun scrape(config: WebSourceConfig, query: String? = null): List<MediaItem> {
        val url = if (query != null && config.searchUrlTemplate.isNotBlank()) {
            config.searchUrlTemplate.replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))
        } else {
            config.catalogUrl
        }
        val html = fetch(url, config.headers) ?: return emptyList()
        val doc = Jsoup.parse(html, url)
        val elements = doc.select(config.itemSelector)
        if (elements.isEmpty()) {
            if (BuildConfig.DEBUG) Log.w("WebMediaSource", "Selektor ${config.itemSelector} zwrócił 0 wyników. Możliwa zmiana struktury HTML strony.")
        }
        return elements.mapNotNull { element ->
            try {
                val rawTitle = element.select(config.titleSelector).firstOrNull()?.text() ?: return@mapNotNull null
                val title = rawTitle.trim()
                if (title.isEmpty() || title.contains('<') || title.contains('>')) return@mapNotNull null

                val relativeHref = element.select(config.linkSelector).firstOrNull()?.attr("href") ?: ""
                val href = resolveUrl(config.baseUrl, relativeHref)
                if (!href.startsWith("http://", ignoreCase = true) && !href.startsWith("https://", ignoreCase = true)) {
                    return@mapNotNull null
                }
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
                    headers = config.headers,
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

    private suspend fun fetch(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (k, v) -> header(k, v) }
                    if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
                        header("User-Agent", "Mozilla/5.0")
                    }
                }
                .build()
            client.newCall(request).execute().body?.string()
        } catch (_: Exception) {
            null
        }
    }

    private fun runJsResolver(script: String, pageUrl: String, mediaItem: MediaItem, headers: Map<String, String>): String? {
        val engine = quickJsEngineProvider.get()
        return try {
            engine.init()
            engine.evaluate(script)
            val headersJson = toJsObjectLiteral(headers)
            val result = engine.evaluate("resolve(${toJsLiteral(pageUrl)}, ${toJsLiteral(mediaItem.id)}, $headersJson)")
            when (result) {
                is String -> result.ifBlank { null }
                is Map<*, *> -> (result["url"] as? String)?.ifBlank { null }
                else -> null
            }
        } catch (e: Exception) {
            null
        } finally {
            engine.close()
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

    private fun toJsObjectLiteral(map: Map<String, String>): String {
        val entries = map.map { (k, v) -> "${toJsLiteral(k)}:${toJsLiteral(v)}" }
        return "{" + entries.joinToString(",") + "}"
    }

    private fun headersWithCookies(url: String, baseHeaders: Map<String, String>): Map<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return baseHeaders
        val cookieHeader = cookieJar.cookieHeader(httpUrl) ?: return baseHeaders
        val existing = baseHeaders["Cookie"]
        val combined = listOfNotNull(existing, cookieHeader).joinToString("; ")
        return baseHeaders + ("Cookie" to combined)
    }

    private fun findMediaUrl(unpacked: String): String? {
        val mediaRegex = Regex(
            """(https?://[^\s"'<>]+\.(?:mp4|m3u8|mpd|webm|mkv|avi|flv|ts))(?:\?[^\s"'<>]*)?""",
            RegexOption.IGNORE_CASE
        )
        mediaRegex.find(unpacked)?.let { return it.groupValues[1] }
        return Regex("""https?://[^\s"'<>]+""").find(unpacked)?.value
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
    val streamAttribute: String = "src",
    val headers: Map<String, String> = emptyMap(),
    val jsScript: String = ""
)

@Serializable
@SerialName("CloudflareResponse")
data class CloudflareResponse(
    @SerialName("streamUrl") val streamUrl: String = "",
    @SerialName("headers") val headers: Map<String, String> = emptyMap()
)
