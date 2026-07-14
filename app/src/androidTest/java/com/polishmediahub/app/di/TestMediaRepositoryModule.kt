package com.polishmediahub.app.di

import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [MediaRepositoryModule::class])
object TestMediaRepositoryModule {

    @Provides
    @Singleton
    fun provideFakeMediaRepository(): MediaRepository = object : MediaRepository {
        override suspend fun featured(): List<MediaItem> = listOf(
            MediaItem(
                id = "test:movie",
                title = "Test Movie",
                subtitle = "Test",
                description = "Test description",
                type = MediaItem.Type.MOVIE
            )
        )

        override suspend fun categories(): List<Category> = listOf(
            Category(
                id = "test:category",
                name = "Test Category",
                items = listOf(
                    MediaItem(
                        id = "test:item",
                        title = "Test Category Item",
                        subtitle = "Test",
                        description = "Test description",
                        type = MediaItem.Type.MOVIE
                    )
                )
            )
        )

        override suspend fun search(query: String): List<MediaItem> = featured().filter { it.title.contains(query, ignoreCase = true) }

        override suspend fun byId(id: String): MediaItem? = (featured() + categories().flatMap { it.items }).find { it.id == id }
    }
}
