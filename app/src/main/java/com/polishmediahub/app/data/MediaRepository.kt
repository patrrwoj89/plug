package com.polishmediahub.app.data

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem

interface MediaRepository {
    suspend fun featured(): List<MediaItem>
    suspend fun categories(): List<Category>
    suspend fun search(query: String): List<MediaItem>
    suspend fun byId(id: String): MediaItem?
}
