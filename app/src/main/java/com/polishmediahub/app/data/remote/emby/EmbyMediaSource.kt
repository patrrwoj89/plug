package com.polishmediahub.app.data.remote.emby

import android.util.Log
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class EmbyMediaSource @Inject constructor(
    private val apiFactory: EmbyApiFactory
) : MediaSource {

    private companion object {
        const val REPORT_INTERVAL_MS = 15_000L
        const val TICKS_PER_MS = 10_000L
    }

    private val lastReportedAt = AtomicLong(0L)
    private val lastReportedPosition = AtomicLong(-1L)

    override val id: String = "emby"
    override val name: String = "Emby"
    override val isConfigurable: Boolean = true

    private suspend fun api(): EmbyApi = apiFactory.create()
    private suspend fun token(): String = apiFactory.token()
    private suspend fun serverUrl(): String = apiFactory.serverUrl()

    override suspend fun isAvailable(): Boolean =
        serverUrl().isNotBlank() && token().isNotBlank()

    private suspend fun userId(): String? {
        return try {
            api().getUsers(token()).firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun featured(): List<MediaItem> {
        return try {
            val userId = userId() ?: return emptyList()
            val response = api().getItems(token(), userId)
            mapItems(response.items)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = listOf(Category(id = "emby", name = "Emby", items = featured()))

    override suspend fun search(query: String): List<MediaItem> {
        return try {
            val userId = userId() ?: return emptyList()
            val response = api().getItems(token(), userId)
            mapItems(response.items.filter { it.name.contains(query, ignoreCase = true) })
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? {
        return try {
            val realId = id.removePrefix("emby:")
            val userId = userId() ?: return null
            mapItem(api().getItem(token(), realId, userId))
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val realId = mediaItem.id.removePrefix("emby:")
        val server = serverUrl().removeSuffix("/")
        val token = token()
        val forceTranscode = apiFactory.forceTranscode()
        val maxBitrate = apiFactory.maxDirectPlayBitrate().toLongOrNull() ?: Long.MAX_VALUE
        val shouldTranscode = forceTranscode ||
            (mediaItem.bitrate != null && mediaItem.bitrate > maxBitrate)

        return if (shouldTranscode) {
            val maxStreamingBitrate = mediaItem.bitrate?.coerceAtMost(maxBitrate) ?: maxBitrate
            "$server/Videos/$realId/main.m3u8" +
                "?static=false" +
                "&VideoCodec=h264" +
                "&AudioCodec=aac" +
                "&MaxStreamingBitrate=$maxStreamingBitrate" +
                "&api_key=$token"
        } else {
            "$server/Videos/$realId/stream?static=true&api_key=$token"
        }
    }

    override suspend fun reportProgress(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        state: PlaybackState
    ) {
        if (!mediaItem.id.startsWith("emby:")) return
        val now = System.currentTimeMillis()
        if (state == PlaybackState.PLAYING) {
            val last = lastReportedAt.get()
            if (now - last < REPORT_INTERVAL_MS && positionMs == lastReportedPosition.get()) return
        }
        lastReportedAt.set(now)
        lastReportedPosition.set(positionMs)

        try {
            val realId = mediaItem.id.removePrefix("emby:")
            api().reportProgress(
                token = token(),
                info = PlaybackProgressInfo(
                    itemId = realId,
                    positionTicks = positionMs * TICKS_PER_MS,
                    isPaused = state == PlaybackState.PAUSED,
                    isStopped = state == PlaybackState.STOPPED
                )
            )
        } catch (e: Exception) {
            Log.w("EmbyMediaSource", "report progress failed: ${e.message}")
        }
    }

    private suspend fun mapItems(items: List<EmbyItem>): List<MediaItem> =
        items.map { mapItem(it) }

    private suspend fun mapItem(item: EmbyItem): MediaItem {
        val type = when (item.type) {
            "Episode" -> MediaItem.Type.SERIES
            "Series" -> MediaItem.Type.SERIES
            else -> MediaItem.Type.MOVIE
        }
        val server = serverUrl().removeSuffix("/")
        val token = token()
        val poster = item.primaryImageTag?.let { tag ->
            "$server/Items/${item.id}/Images/Primary?tag=$tag&api_key=$token"
        }
        val backdrop = item.backdropImageTags?.firstOrNull()?.let { tag ->
            "$server/Items/${item.id}/Images/Backdrop/0?tag=$tag&api_key=$token"
        }
        val streamUrl = "$server/Videos/${item.id}/stream?static=true&api_key=$token"
        return MediaItem(
            id = "emby:${item.id}",
            title = item.name,
            description = item.overview ?: "",
            posterUrl = poster,
            backdropUrl = backdrop,
            type = type,
            year = item.productionYear?.toString() ?: "",
            bitrate = item.bitrate,
            videoUrl = streamUrl
        )
    }
}
