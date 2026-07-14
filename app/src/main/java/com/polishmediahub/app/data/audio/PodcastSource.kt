package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import javax.inject.Inject

class PodcastSource @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val okHttpClient: OkHttpClient
) : AudioSource {

    override val id: String = "podcast"
    override val name: String = "Podcasts"

    private suspend fun feedUrls(): List<String> =
        apiConfigRepository.podcastFeeds.first()
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.startsWith("http") }

    override suspend fun isAvailable(): Boolean = feedUrls().isNotEmpty()

    override suspend fun browse(): List<AudioTrack> = withContext(Dispatchers.IO) {
        feedUrls().flatMap { url -> fetchAndParse(url) }
    }

    override suspend fun search(query: String): List<AudioTrack> =
        browse().filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl

    private fun fetchAndParse(feedUrl: String): List<AudioTrack> {
        return try {
            val request = Request.Builder().url(feedUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            val document = Jsoup.parse(body, "", Parser.xmlParser())
            val channelTitle = document.selectFirst("channel > title")?.text() ?: "Podcast"
            document.select("item").map { item ->
                val title = item.selectFirst("title")?.text() ?: "Untitled"
                val enclosure = item.selectFirst("enclosure")?.attr("url")
                    ?: item.selectFirst("media|content")?.attr("url")
                val image = item.selectFirst("itunes|image")?.attr("href")
                    ?: document.selectFirst("channel > itunes|image")?.attr("href")
                val durationText = item.selectFirst("itunes|duration")?.text() ?: ""
                val durationMs = parseDuration(durationText)
                AudioTrack(
                    id = "podcast:${item.selectFirst("guid")?.text() ?: title}",
                    title = title,
                    artist = channelTitle,
                    album = "Podcast",
                    coverUrl = image,
                    streamUrl = enclosure,
                    durationMs = durationMs,
                    sourceId = id
                )
            }.filter { it.streamUrl != null }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDuration(duration: String): Long {
        val parts = duration.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> ((parts[0] * 3600) + (parts[1] * 60) + parts[2]) * 1000
            2 -> ((parts[0] * 60) + parts[1]) * 1000
            1 -> parts[0] * 1000
            else -> 0L
        }
    }
}
