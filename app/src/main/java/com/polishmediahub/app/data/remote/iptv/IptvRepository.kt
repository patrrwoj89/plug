package com.polishmediahub.app.data.remote.iptv

import android.util.Log
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
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
    private val apiConfigRepository: ApiConfigRepository
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
        loadChannels().filter { it.title.contains(query, ignoreCase = true) }
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
}
