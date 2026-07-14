package com.polishmediahub.app.data

import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import javax.inject.Inject

class MockMediaRepository @Inject constructor(
    private val dataSource: MockDataSource
) : MediaRepository {

    override suspend fun featured(): List<MediaItem> = dataSource.featured()

    override suspend fun categories(): List<Category> = dataSource.categories()

    override suspend fun search(query: String): List<MediaItem> = dataSource.search(query)

    override suspend fun byId(id: String): MediaItem? = dataSource.byId(id)
}
