package com.tvhub.skeleton.data.remote.stremio

import com.tvhub.skeleton.data.ApiConfigRepository
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioRepository @Inject constructor(
    private val api: StremioApi,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    private suspend fun addonUrls(): List<String> =
        apiConfigRepository.stremioAddons.first()
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.startsWith("http") }

    override suspend fun featured(): List<MediaItem> {
        val urls = addonUrls()
        if (urls.isEmpty()) return emptyList()
        return try {
            urls.flatMap { baseUrl ->
                val manifest = api.getManifest("$baseUrl/manifest.json")
                manifest.catalogs
                    ?.filter { it.type == "movie" || it.type == "series" }
                    ?.take(1)
                    ?.flatMap { catalog ->
                        val catalogUrl = "$baseUrl/catalog/${catalog.type}/${catalog.id}.json"
                        api.getCatalog(catalogUrl).metas.map { it.toMediaItem(baseUrl) }
                    } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> {
        val urls = addonUrls()
        if (urls.isEmpty()) return emptyList()
        return try {
            urls.flatMap { baseUrl ->
                val manifest = api.getManifest("$baseUrl/manifest.json")
                manifest.catalogs?.map { catalog ->
                    val catalogUrl = "$baseUrl/catalog/${catalog.type}/${catalog.id}.json"
                    val items = try {
                        api.getCatalog(catalogUrl).metas.map { it.toMediaItem(baseUrl) }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    Category(
                        id = "${manifest.id}:${catalog.id}",
                        name = catalog.name ?: "${manifest.name} - ${catalog.id}",
                        items = items
                    )
                } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String): List<MediaItem> {
        val urls = addonUrls()
        if (urls.isEmpty()) return emptyList()
        return try {
            urls.flatMap { baseUrl ->
                val manifest = api.getManifest("$baseUrl/manifest.json")
                manifest.catalogs
                    ?.filter { catalog ->
                        catalog.extraParams?.any { it.name == "search" } == true
                    }
                    ?.flatMap { catalog ->
                        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                        val catalogUrl = "$baseUrl/catalog/${catalog.type}/${catalog.id}/search=$encoded.json"
                        try {
                            api.getCatalog(catalogUrl).metas.map { it.toMediaItem(baseUrl) }
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? {
        val urls = addonUrls()
        if (urls.isEmpty()) return null
        return urls.firstNotNullOfOrNull { baseUrl ->
            try {
                if (!id.startsWith(baseUrl)) return@firstNotNullOfOrNull null
                val parts = id.removePrefix(baseUrl).removePrefix("/").split(":")
                if (parts.size < 2) return@firstNotNullOfOrNull null
                val type = parts[0]
                val itemId = parts[1]
                val metaUrl = "$baseUrl/meta/$type/$itemId.json"
                api.getMeta(metaUrl).meta?.toMediaItem(baseUrl)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun resolveStream(baseUrl: String, type: String, id: String): List<StremioStream> {
        return try {
            api.getStreams("$baseUrl/stream/$type/$id.json").streams
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun StremioMetaPreview.toMediaItem(baseUrl: String): MediaItem = MediaItem(
    id = "$baseUrl:$type:$id",
    title = name,
    subtitle = year ?: imdbRating ?: type,
    description = "",
    posterUrl = poster,
    backdropUrl = poster,
    type = mapStremioType(type),
    year = year ?: "",
    rating = imdbRating ?: "",
    genres = genres ?: emptyList(),
    duration = "",
    videoUrl = null
)

private fun StremioMeta.toMediaItem(baseUrl: String): MediaItem = MediaItem(
    id = "$baseUrl:$type:$id",
    title = name,
    subtitle = year ?: releaseInfo ?: type,
    description = description.orEmpty(),
    posterUrl = poster,
    backdropUrl = background ?: poster,
    type = mapStremioType(type),
    year = year ?: "",
    rating = imdbRating ?: "",
    genres = genres ?: emptyList(),
    duration = runtime ?: "",
    videoUrl = null
)

private fun mapStremioType(type: String): MediaItem.Type = when (type.lowercase()) {
    "series", "show", "tv" -> MediaItem.Type.SERIES
    "episode" -> MediaItem.Type.EPISODE
    "channel", "tv_channel" -> MediaItem.Type.CHANNEL
    else -> MediaItem.Type.MOVIE
}
