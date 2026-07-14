package com.polishmediahub.app.data.remote.iptv

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class IptvRepository @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    override suspend fun featured(): List<MediaItem> = try {
        fetchChannels().take(10)
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun categories(): List<Category> = try {
        val channels = fetchChannels()
        channels.groupBy { it.genres.firstOrNull() ?: "Uncategorized" }
            .map { (group, items) -> Category(id = "iptv_$group", name = group, items = items) }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> = try {
        fetchChannels().filter { it.title.contains(query, ignoreCase = true) }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = try {
        fetchChannels().find { it.id == id }
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchChannels(): List<MediaItem> = withContext(Dispatchers.IO) {
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
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()
        return M3UParser.parse(body)
    }
}
