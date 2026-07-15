package com.polishmediahub.app.data.remote.health

import com.polishmediahub.app.data.source.WebSourceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthCheckEngine @Inject constructor(
    private val json: Json
) {

    suspend fun runChecks(client: OkHttpClient, config: HealthConfig): HealthStatus = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val results = mutableListOf<SourceHealth>()

        results.add(checkKodi(client, config.kodiUrl, timestamp))
        results.add(checkGeneric(client, "epg", "EPG", config.epgUrl, timestamp))
        results.add(checkJellyfin(client, config.jellyfinUrl, config.jellyfinToken, timestamp))
        results.add(checkEmby(client, config.embyUrl, config.embyToken, timestamp))
        results.add(checkPlex(client, config.plexUrl, config.plexToken, timestamp))
        results.add(checkSubsonic(client, config.subsonicUrl, config.subsonicUser, config.subsonicPassword, timestamp))
        results.add(checkGeneric(client, "deezer", "Deezer Proxy", config.deezerProxyUrl, timestamp))

        config.iptvUrls.forEachIndexed { index, url ->
            results.add(checkGeneric(client, "iptv_$index", "IPTV ${index + 1}", url, timestamp))
        }
        config.stremioAddons.forEachIndexed { index, url ->
            results.add(checkGeneric(client, "stremio_$index", "Stremio addon ${index + 1}", url, timestamp))
        }
        config.cloudstreamRepos.forEachIndexed { index, url ->
            results.add(checkGeneric(client, "cloudstream_$index", "Cloudstream repo ${index + 1}", url, timestamp))
        }
        config.podcastFeeds.forEachIndexed { index, url ->
            results.add(checkGeneric(client, "podcast_$index", "Podcast feed ${index + 1}", url, timestamp))
        }

        parseWebSourceUrls(config.webSourceConfig).forEachIndexed { index, entry ->
            results.add(checkGeneric(client, "web_$index", entry.label, entry.url, timestamp))
        }

        HealthStatus(lastCheckAt = timestamp, sources = results)
    }

    private fun checkKodi(client: OkHttpClient, url: String, timestamp: Long): SourceHealth {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return unconfigured("kodi", "Kodi")
        val body = """{"jsonrpc":"2.0","method":"JSONRPC.Version","id":1}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(trimmed).post(body).build()
        return executeCheck(client, "kodi", "Kodi", trimmed, request, timestamp)
    }

    private fun checkJellyfin(client: OkHttpClient, baseUrl: String, token: String, timestamp: Long): SourceHealth {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return unconfigured("jellyfin", "Jellyfin")
        val url = trimmed.removeSuffix("/") + "/System/Info/Public"
        val requestBuilder = Request.Builder().url(url)
        if (token.isNotBlank()) requestBuilder.header("X-Emby-Token", token)
        return executeCheck(client, "jellyfin", "Jellyfin", trimmed, requestBuilder.build(), timestamp)
    }

    private fun checkEmby(client: OkHttpClient, baseUrl: String, token: String, timestamp: Long): SourceHealth {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return unconfigured("emby", "Emby")
        val normalized = trimmed.removeSuffix("/")
        val url = if (normalized.endsWith("/emby", ignoreCase = true)) {
            "$normalized/System/Info/Public"
        } else {
            "$normalized/emby/System/Info/Public"
        }
        val requestBuilder = Request.Builder().url(url)
        if (token.isNotBlank()) requestBuilder.header("X-Emby-Token", token)
        return executeCheck(client, "emby", "Emby", trimmed, requestBuilder.build(), timestamp)
    }

    private fun checkPlex(client: OkHttpClient, baseUrl: String, token: String, timestamp: Long): SourceHealth {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return unconfigured("plex", "Plex")
        val url = trimmed.removeSuffix("/") + "/identity"
        val requestBuilder = Request.Builder().url(url)
        if (token.isNotBlank()) requestBuilder.header("X-Plex-Token", token)
        return executeCheck(client, "plex", "Plex", trimmed, requestBuilder.build(), timestamp)
    }

    private fun checkSubsonic(
        client: OkHttpClient,
        baseUrl: String,
        user: String,
        password: String,
        timestamp: Long
    ): SourceHealth {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return unconfigured("subsonic", "Subsonic")
        val httpUrl = trimmed.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment("rest")
            ?.addPathSegment("ping.view")
            ?.addQueryParameter("u", user)
            ?.addQueryParameter("p", password)
            ?.addQueryParameter("v", "1.16.1")
            ?.addQueryParameter("c", "PolishMediaHub")
            ?.addQueryParameter("f", "json")
            ?.build()
        if (httpUrl == null) {
            return SourceHealth("subsonic", "Subsonic", trimmed, SourceHealth.OFFLINE, timestamp, "Invalid URL")
        }
        val request = Request.Builder().url(httpUrl).build()
        return executeCheck(client, "subsonic", "Subsonic", trimmed, request, timestamp) { body ->
            body.contains("\"status\":\"ok\"") || body.contains("\"status\": \"ok\"")
        }
    }

    private fun checkGeneric(
        client: OkHttpClient,
        id: String,
        label: String,
        url: String,
        timestamp: Long
    ): SourceHealth {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return unconfigured(id, label)
        val request = Request.Builder().url(trimmed).get().build()
        return executeCheck(client, id, label, trimmed, request, timestamp)
    }

    private fun executeCheck(
        client: OkHttpClient,
        id: String,
        label: String,
        url: String,
        request: Request,
        timestamp: Long,
        isHealthy: (String) -> Boolean = { true }
    ): SourceHealth {
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (isHealthy(body)) {
                        SourceHealth(id, label, url, SourceHealth.ONLINE, timestamp)
                    } else {
                        SourceHealth(id, label, url, SourceHealth.OFFLINE, timestamp, "Unhealthy response body")
                    }
                } else {
                    SourceHealth(id, label, url, SourceHealth.OFFLINE, timestamp, "HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            SourceHealth(id, label, url, SourceHealth.OFFLINE, timestamp, e.javaClass.simpleName)
        } catch (e: Exception) {
            SourceHealth(id, label, url, SourceHealth.OFFLINE, timestamp, e.javaClass.simpleName)
        }
    }

    private fun unconfigured(id: String, label: String) = SourceHealth(
        id = id,
        label = label,
        url = "",
        status = SourceHealth.UNCONFIGURED,
        checkedAt = 0L
    )

    private fun parseWebSourceUrls(configJson: String): List<WebSourceEntry> {
        if (configJson.isBlank()) return emptyList()
        return try {
            val configs = json.decodeFromString(ListSerializer(WebSourceConfig.serializer()), configJson)
            configs.mapNotNull { entry ->
                val base = entry.baseUrl.trim()
                if (base.isNotBlank()) WebSourceEntry(entry.name.ifBlank { "Web source" }, base) else null
            }
        } catch (e: Exception) {
            // Fallback: try to extract any bare "baseUrl" strings from a loose JSON object/array.
            try {
                val element = json.parseToJsonElement(configJson)
                val array = element as? JsonArray ?: JsonArray(listOf(element))
                array.mapNotNull { item ->
                    val obj = item.jsonObject
                    val base = obj["baseUrl"]?.jsonPrimitive?.content?.trim()
                    val name = obj["name"]?.jsonPrimitive?.content?.trim()
                    if (!base.isNullOrBlank()) WebSourceEntry(name?.ifBlank { "Web source" } ?: "Web source", base) else null
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private data class WebSourceEntry(val label: String, val url: String)
}
