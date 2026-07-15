package com.polishmediahub.app.data.remote.plex

import android.util.Log
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class PlexMediaSource @Inject constructor(
    private val apiFactory: PlexApiFactory
) : MediaSource {

    private companion object {
        const val REPORT_INTERVAL_MS = 15_000L
    }

    private val lastReportedAt = AtomicLong(0L)
    private val lastReportedPosition = AtomicLong(-1L)

    override val id: String = "plex"
    override val name: String = "Plex"
    override val isConfigurable: Boolean = true

    private suspend fun api(): PlexApi = apiFactory.create()
    private suspend fun token(): String = apiFactory.token()
    private suspend fun serverUrl(): String = apiFactory.serverUrl()

    override suspend fun isAvailable(): Boolean =
        serverUrl().isNotBlank() && token().isNotBlank()

    override suspend fun featured(): List<MediaItem> {
        return try {
            val response = api().getLibrarySections(token())
            val directories = response.mediaContainer?.directories ?: return emptyList()
            val items = mutableListOf<MediaItem>()
            directories.forEach { dir ->
                val sectionKey = dir.key
                val sectionItems = api().getSectionItems(sectionKey, token())
                items += sectionItems.mediaContainer?.metadata?.map { mapMetadata(it) } ?: emptyList()
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = listOf(Category(id = "plex", name = "Plex", items = featured()))

    override suspend fun search(query: String): List<MediaItem> {
        return featured().filter { it.title.contains(query, ignoreCase = true) }
    }

    override suspend fun byId(id: String): MediaItem? {
        return try {
            val realId = id.removePrefix("plex:")
            val response = api().getMetadata(realId, token())
            response.mediaContainer?.metadata?.firstOrNull()?.let { mapMetadata(it) }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val realId = mediaItem.id.removePrefix("plex:")
        val server = serverUrl().removeSuffix("/")
        val token = token()
        val forceTranscode = apiFactory.forceTranscode()
        val maxBitrate = apiFactory.maxDirectPlayBitrate().toLongOrNull() ?: Long.MAX_VALUE
        val shouldTranscode = forceTranscode ||
            (mediaItem.bitrate != null && mediaItem.bitrate > maxBitrate)

        if (shouldTranscode) {
            return "$server/video/:/transcode/universal/start.m3u8" +
                "?path=/library/metadata/$realId" +
                "&mediaIndex=0&partIndex=0&protocol=hls&fastSeek=1" +
                "&directPlay=0&directStream=0&location=lan" +
                "&subtitleSize=100&audioBoost=100" +
                "&videoCodec=h264&audioCodec=aac" +
                "&mediaBufferSize=102400" +
                "&X-Plex-Token=$token"
        }

        return try {
            val response = api().getMetadata(realId, token)
            val metadata = response.mediaContainer?.metadata?.firstOrNull() ?: return null
            val partKey = metadata.media?.firstOrNull()?.parts?.firstOrNull()?.key ?: return null
            "$server$partKey?X-Plex-Token=$token"
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun reportProgress(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        state: PlaybackState
    ) {
        if (!mediaItem.id.startsWith("plex:")) return
        val now = System.currentTimeMillis()
        if (state == PlaybackState.PLAYING) {
            val last = lastReportedAt.get()
            if (now - last < REPORT_INTERVAL_MS && positionMs == lastReportedPosition.get()) return
        }
        lastReportedAt.set(now)
        lastReportedPosition.set(positionMs)

        try {
            val realId = mediaItem.id.removePrefix("plex:")
            val stateStr = when (state) {
                PlaybackState.PLAYING -> "playing"
                PlaybackState.PAUSED -> "paused"
                PlaybackState.STOPPED -> "stopped"
            }
            api().reportTimeline(
                ratingKey = realId,
                key = "/library/metadata/$realId",
                state = stateStr,
                time = positionMs,
                duration = durationMs,
                token = token()
            )
        } catch (e: Exception) {
            Log.w("PlexMediaSource", "report progress failed: ${e.message}")
        }
    }

    private suspend fun mapMetadata(metadata: PlexMetadata): MediaItem {
        val type = when (metadata.type) {
            "show", "episode" -> MediaItem.Type.SERIES
            else -> MediaItem.Type.MOVIE
        }
        val server = serverUrl().removeSuffix("/")
        val token = token()
        val poster = metadata.thumb?.let { "$server$it?X-Plex-Token=$token" }
        val backdrop = metadata.art?.let { "$server$it?X-Plex-Token=$token" }
        return MediaItem(
            id = "plex:${metadata.ratingKey}",
            title = metadata.title,
            description = metadata.summary ?: "",
            posterUrl = poster,
            backdropUrl = backdrop,
            type = type,
            year = metadata.year?.toString() ?: "",
            bitrate = metadata.bitrate?.times(1000)?.toLong(),
            videoUrl = null
        )
    }
}
