package com.tvhub.skeleton.data.source

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-source bridge: aggregates results from all registered [MediaSource] implementations.
 * Enables "federated" search and category browsing across Kodi, Stremio, IPTV, etc.
 */
@Singleton
class SourceRegistry @Inject constructor(
    sources: Set<@JvmSuppressWildcards MediaSource>
) {
    private val sourceMap = sources.associateBy { it.id }

    val all: List<MediaSource> get() = sourceMap.values.toList()

    fun source(id: String): MediaSource? = sourceMap[id]

    suspend fun searchAll(query: String): Map<String, List<MediaItem>> =
        all.associate { it.id to it.search(query) }

    suspend fun categoriesAll(): List<Category> =
        all.flatMap { it.categories() }
            .map { it.copy(id = "${it.id}") }

    suspend fun featuredAll(): List<MediaItem> =
        all.flatMap { it.featured() }
}
