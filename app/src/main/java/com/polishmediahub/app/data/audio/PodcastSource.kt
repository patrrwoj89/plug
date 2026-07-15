package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.remote.podcast.PodcastRssParser
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class PodcastSource @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val okHttpClient: OkHttpClient
) : AudioSource {

    override val id: String = "podcast"
    override val name: String = "Podcasts"

    private val cache = mutableMapOf<String, AudioTrack>()

    private suspend fun feedUrls(): List<String> =
        apiConfigRepository.podcastFeeds.first()
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.startsWith("http") }

    override suspend fun isAvailable(): Boolean = feedUrls().isNotEmpty()

    override suspend fun browse(): List<AudioTrack> = withContext(Dispatchers.IO) {
        feedUrls().flatMap { url -> fetchAndParse(url) }
            .also { tracks -> cache.putAll(tracks.associateBy { it.id }) }
    }

    override suspend fun search(query: String): List<AudioTrack> =
        browse().filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }

    override suspend fun byId(trackId: String): AudioTrack? {
        if (!trackId.startsWith("podcast:")) return null
        cache[trackId]?.let { return it }
        val tracks = browse()
        return tracks.find { it.id == trackId }
    }

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl

    private suspend fun fetchAndParse(feedUrl: String): List<AudioTrack> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            val body = okHttpClient.newCall(request).execute().use { it.body?.string() } ?: return@withContext emptyList()
            PodcastRssParser.parse(body, id).map { it.copy(sourceId = id) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
