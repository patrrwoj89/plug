package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RadioRepository @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : AudioSource {

    override val id: String = "radio"
    override val name: String = "Internet radio"

    private val cache = mutableMapOf<String, AudioTrack>()

    private suspend fun urls(): List<String> {
        return apiConfigRepository.iptvSourceUrls.first()
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() && (it.endsWith(".m3u", ignoreCase = true) || it.endsWith(".m3u8", ignoreCase = true) || it.endsWith(".pls", ignoreCase = true) || it.startsWith("http")) }
    }

    override suspend fun isAvailable(): Boolean = urls().isNotEmpty()

    override suspend fun browse(): List<AudioTrack> = withContext(Dispatchers.IO) {
        urls().flatMap { parsePlaylist(it) }
            .also { tracks -> cache.putAll(tracks.associateBy { it.id }) }
    }

    override suspend fun search(query: String): List<AudioTrack> =
        browse().filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    override suspend fun byId(trackId: String): AudioTrack? {
        if (!trackId.startsWith("radio:")) return null
        cache[trackId]?.let { return it }
        val tracks = browse()
        return tracks.find { it.id == trackId }
    }

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl

    private suspend fun parsePlaylist(url: String): List<AudioTrack> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            when {
                url.endsWith(".pls", ignoreCase = true) || body.trimStart().startsWith("[playlist]", ignoreCase = true) -> parsePls(body, url)
                else -> parseM3u(body, url)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseM3u(body: String, baseUrl: String): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val lines = body.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.iterator()
        var pendingTitle = ""
        var pendingLogo = ""
        var pendingGroup = ""

        while (lines.hasNext()) {
            val line = lines.next()
            when {
                line.startsWith("#EXTINF:") -> {
                    val meta = line.removePrefix("#EXTINF:").trim()
                    val commaIdx = meta.indexOf(',')
                    val left = if (commaIdx >= 0) meta.substring(0, commaIdx) else meta
                    pendingTitle = if (commaIdx >= 0) meta.substring(commaIdx + 1).trim() else ""
                    val attrString = left.substringAfter(" ", "")
                    val attrs = parseM3uAttributes(attrString)
                    pendingLogo = attrs["tvg-logo"] ?: attrs["logo"] ?: ""
                    pendingGroup = attrs["group-title"] ?: ""
                    if (pendingTitle.isBlank()) {
                        pendingTitle = left.substringBefore(" ", "")
                    }
                }
                line.startsWith("#EXTVLCOPT:") -> {
                    // ignore vlc options
                }
                !line.startsWith("#") -> {
                    val streamUrl = resolveUrl(line, baseUrl)
                    val trackId = "radio:${streamUrl.hashCode()}"
                    tracks += AudioTrack(
                        id = trackId,
                        title = pendingTitle.ifBlank { streamUrl },
                        artist = pendingGroup.ifBlank { "Radio" },
                        album = "Internet radio",
                        coverUrl = pendingLogo,
                        streamUrl = streamUrl,
                        durationMs = 0,
                        isLive = true,
                        sourceId = id
                    )
                    pendingTitle = ""
                    pendingLogo = ""
                    pendingGroup = ""
                }
            }
        }
        return tracks
    }

    private fun parsePls(body: String, baseUrl: String): List<AudioTrack> {
        val entries = mutableMapOf<Int, MutableMap<String, String>>()
        body.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("File", ignoreCase = true) || line.startsWith("Title", ignoreCase = true) || line.startsWith("Length", ignoreCase = true)) {
                val eq = line.indexOf('=')
                if (eq > 0) {
                    val keyWithIndex = line.substring(0, eq).trim()
                    val value = line.substring(eq + 1).trim()
                    val number = keyWithIndex.takeLastWhile { it.isDigit() }.toIntOrNull() ?: 1
                    val key = keyWithIndex.dropLastWhile { it.isDigit() }.lowercase()
                    val map = entries.getOrPut(number) { mutableMapOf() }
                    map[key] = value
                }
            }
        }
        return entries.values.mapNotNull { entry ->
            val streamUrl = entry["file"] ?: return@mapNotNull null
            val title = entry["title"] ?: streamUrl
            val resolvedUrl = resolveUrl(streamUrl, baseUrl)
            AudioTrack(
                id = "radio:${resolvedUrl.hashCode()}",
                title = title,
                artist = "Radio",
                album = "Internet radio",
                streamUrl = resolvedUrl,
                durationMs = 0,
                isLive = true,
                sourceId = id
            )
        }
    }

    private fun parseM3uAttributes(attrString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = """([\w\-]+)=["']([^"']+)["']""".toRegex()
        regex.findAll(attrString).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }

    private fun resolveUrl(url: String, baseUrl: String): String {
        return when {
            url.startsWith("http", ignoreCase = true) -> url
            url.startsWith("/") -> baseUrl.removeSuffix("/").substringBeforeLast("/") + url
            else -> baseUrl.substringBeforeLast("/") + "/" + url
        }
    }
}
