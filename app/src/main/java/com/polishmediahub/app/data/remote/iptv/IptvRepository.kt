package com.polishmediahub.app.data.remote.iptv

import android.util.Log
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.local.ChannelDao
import com.polishmediahub.app.data.local.ChannelEntity
import com.polishmediahub.app.data.local.EpgDao
import com.polishmediahub.app.data.local.EpgEntity
import com.polishmediahub.app.data.source.LevenshteinEngine
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

private const val TAG = "IptvRepository"

class IptvRepository @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository,
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao
) : MediaRepository {

    override suspend fun featured(): List<MediaItem> = try {
        loadChannels().take(10)
    } catch (e: Exception) {
        Log.w(TAG, "featured() failed: ${e.message}", e)
        emptyList()
    }

    override suspend fun categories(): List<Category> = try {
        val channels = loadChannels()
        channels.groupBy { it.genres.firstOrNull() ?: "Uncategorized" }
            .map { (group, items) -> Category(id = "iptv_$group", name = group, items = items) }
    } catch (e: Exception) {
        Log.w(TAG, "categories() failed: ${e.message}", e)
        emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> = try {
        val pattern = "%$query%"
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val from = now - day
        val to = now + day

        val localChannels = channelDao.search(pattern, limit = 100).map { it.toMediaItem() }
        val epgPrograms = epgDao.search(pattern, from, to, limit = 100).map { it.toMediaItem() }
        val remoteChannels = loadChannels().filter { it.title.contains(query, ignoreCase = true) }

        val all = (localChannels + epgPrograms + remoteChannels).distinctBy { it.id }
        LevenshteinEngine.sort(query, all, LevenshteinEngine.MAX_DISTANCE_THRESHOLD) { it.title }
    } catch (e: Exception) {
        Log.w(TAG, "search($query) failed: ${e.message}", e)
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = try {
        loadChannels().find { it.id == id }
    } catch (e: Exception) {
        Log.w(TAG, "byId($id) failed: ${e.message}", e)
        null
    }

    internal suspend fun loadChannels(): List<MediaItem> = withContext(Dispatchers.IO) {
        val urls = apiConfigRepository.iptvSourceUrls.first()
        if (urls.isBlank()) return@withContext emptyList()

        urls.split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { url -> fetchPlaylist(url) }
            .distinctBy { it.id }
    }

    private fun fetchPlaylist(url: String): List<MediaItem> {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Playlist fetch failed for $url: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body for $url")
            return M3UParser.parse(body)
        }
    }

    private fun ChannelEntity.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = name,
        subtitle = groupTitle ?: "",
        description = "IPTV channel",
        posterUrl = logoUrl,
        videoUrl = streamUrl,
        tvgId = tvgId,
        channelNumber = channelNumber,
        genres = groupTitle?.let { listOf(it) } ?: emptyList(),
        isLive = true,
        type = MediaItem.Type.CHANNEL
    )

    private fun EpgEntity.toMediaItem(): MediaItem = MediaItem(
        id = "epg:$id",
        title = title,
        subtitle = channelName ?: "",
        description = description,
        posterUrl = iconUrl,
        tvgId = channelId,
        isLive = true,
        type = MediaItem.Type.CHANNEL
    )
}
