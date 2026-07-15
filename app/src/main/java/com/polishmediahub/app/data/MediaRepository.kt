package com.polishmediahub.app.data

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState

interface MediaRepository {
    suspend fun featured(): List<MediaItem>
    suspend fun categories(): List<Category>
    suspend fun search(query: String): List<MediaItem>
    suspend fun byId(id: String): MediaItem?
    suspend fun resolve(mediaItem: MediaItem): String? = mediaItem.videoUrl
    suspend fun resolveItem(mediaItem: MediaItem): MediaItem = mediaItem
    suspend fun reportProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long, state: PlaybackState) {}
}
