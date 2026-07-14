package com.tvhub.skeleton.data.remote.plex

import com.tvhub.skeleton.data.source.MediaSource
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import javax.inject.Inject

class PlexMediaSource @Inject constructor(
    private val apiFactory: PlexApiFactory
) : MediaSource {

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
        return try {
            val response = api().getMetadata(realId, token())
            val metadata = response.mediaContainer?.metadata?.firstOrNull() ?: return null
            val partKey = metadata.media?.firstOrNull()?.parts?.firstOrNull()?.key ?: return null
            val server = serverUrl().removeSuffix("/")
            "$server$partKey?X-Plex-Token=${token()}"
        } catch (_: Exception) {
            null
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
        return MediaItem(
            id = "plex:${metadata.ratingKey}",
            title = metadata.title,
            description = metadata.summary ?: "",
            posterUrl = poster,
            backdropUrl = null,
            type = type,
            year = metadata.year?.toString() ?: "",
            videoUrl = null
        )
    }
}
