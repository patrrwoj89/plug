package com.tvhub.skeleton.data.source

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem

/**
 * Unified interface for any content source: local backend, Stremio addon,
 * Kodi JSON-RPC, IPTV, legal web extractors, or third-party plugin repositories.
 */
interface MediaSource {
    val id: String
    val name: String
    val isConfigurable: Boolean

    suspend fun isAvailable(): Boolean
    suspend fun featured(): List<MediaItem>
    suspend fun categories(): List<Category>
    suspend fun search(query: String): List<MediaItem>
    suspend fun byId(id: String): MediaItem?
    suspend fun resolve(mediaItem: MediaItem): String?
}
