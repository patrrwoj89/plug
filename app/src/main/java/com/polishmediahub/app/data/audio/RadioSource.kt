package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RadioSource @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : AudioSource {

    override val id: String = "radio"
    override val name: String = "Internet radio"

    private suspend fun urls(): List<String> {
        return apiConfigRepository.iptvSourceUrls.first()
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() && (it.endsWith(".m3u", ignoreCase = true) || it.endsWith(".pls", ignoreCase = true) || it.startsWith("http")) }
    }

    override suspend fun isAvailable(): Boolean = urls().isNotEmpty()

    override suspend fun browse(): List<AudioTrack> {
        return urls().flatMap { parsePlaylist(it) }
    }

    override suspend fun search(query: String): List<AudioTrack> =
        browse().filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl

    private suspend fun parsePlaylist(url: String): List<AudioTrack> {
        return try {
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            parseM3u(body, url)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseM3u(body: String, baseUrl: String): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        var pendingTitle = ""
        body.lineSequence().forEach { line ->
            when {
                line.startsWith("#EXTINF:") -> {
                    pendingTitle = line.substringAfter(",", "").trim()
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    val streamUrl = line.trim()
                    tracks += AudioTrack(
                        id = "radio:${streamUrl}",
                        title = pendingTitle.ifBlank { streamUrl },
                        artist = "",
                        album = "",
                        streamUrl = streamUrl,
                        sourceId = id
                    )
                    pendingTitle = ""
                }
            }
        }
        return tracks
    }
}
