package com.tvhub.skeleton.data.remote.jellyfin

import com.tvhub.skeleton.data.source.MediaSource
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class JellyfinMediaSource @Inject constructor(
    private val apiFactory: JellyfinApiFactory
) : MediaSource {

    override val id: String = "jellyfin"
    override val name: String = "Jellyfin"
    override val isConfigurable: Boolean = true

    private suspend fun api(): JellyfinApi = apiFactory.create()
    private suspend fun token(): String = apiFactory.token()
    private suspend fun serverUrl(): String = apiFactory.serverUrl()

    override suspend fun isAvailable(): Boolean {
        return try {
            serverUrl().isNotBlank() && token().isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun userId(): String? {
        return try {
            val api = api()
            val users = api.getUsers(token())
            users.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun featured(): List<MediaItem> {
        return try {
            val userId = userId() ?: return emptyList()
            val api = api()
            val response = api.getItems(token(), userId)
            mapItems(response.items)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = listOf(Category(id = "jellyfin", name = "Jellyfin", items = featured()))

    override suspend fun search(query: String): List<MediaItem> {
        return try {
            val userId = userId() ?: return emptyList()
            val api = api()
            val response = api.getItems(token(), userId)
            mapItems(response.items.filter { it.name.contains(query, ignoreCase = true) })
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? {
        return try {
            val realId = id.removePrefix("jellyfin:")
            val userId = userId() ?: return null
            val api = api()
            mapItem(api.getItem(token(), realId, userId))
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val realId = mediaItem.id.removePrefix("jellyfin:")
        val server = serverUrl().removeSuffix("/")
        val token = token()
        return "$server/Videos/$realId/stream?static=true&api_key=$token"
    }

    private suspend fun mapItems(items: List<JellyfinItem>): List<MediaItem> =
        items.map { mapItem(it) }

    private suspend fun mapItem(item: JellyfinItem): MediaItem {
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
        val streamUrl = "$server/Videos/${item.id}/stream?static=true&api_key=$token"
        return MediaItem(
            id = "jellyfin:${item.id}",
            title = item.name,
            description = item.overview ?: "",
            posterUrl = poster,
            backdropUrl = null,
            type = type,
            year = item.productionYear?.toString() ?: "",
            videoUrl = streamUrl
        )
    }
}
