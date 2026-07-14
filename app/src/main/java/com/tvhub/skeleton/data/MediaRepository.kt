package com.tvhub.skeleton.data

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem

interface MediaRepository {
    suspend fun featured(): List<MediaItem>
    suspend fun categories(): List<Category>
    suspend fun search(query: String): List<MediaItem>
    suspend fun byId(id: String): MediaItem?
}
