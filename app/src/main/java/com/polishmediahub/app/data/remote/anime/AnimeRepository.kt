package com.polishmediahub.app.data.remote.anime

import com.polishmediahub.app.data.ContentFilter
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.remote.anilist.AniListMediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Anime media repository with Polish Docchi priority.
 *
 * Docchi (https://docchi.pl) exposes Polish anime releases, metadata and
 * episode players. It is queried first; if it returns no results (network
 * error, timeout or empty response), the repository falls back to AniList
 * GraphQL and then Kitsu JSON:API.
 * Results are filtered through [ContentFilter] using the active profile.
 */
@Singleton
class AnimeRepository @Inject constructor(
    private val docchiMediaSource: DocchiMediaSource,
    private val aniListRepository: AniListMediaRepository,
    private val kitsuMediaSource: KitsuMediaSource,
    private val profileRepository: ProfileRepository
) : MediaRepository {

    private suspend fun profile() = profileRepository.currentProfile.first()

    override suspend fun featured(): List<MediaItem> {
        val result = docchiMediaSource.featured()
            .ifEmpty { aniListRepository.featured() }
            .ifEmpty { kitsuMediaSource.featured() }
        return ContentFilter.filter(result, profile())
    }

    override suspend fun categories(): List<Category> {
        val docchiCategories = docchiMediaSource.categories()
        val result = if (docchiCategories.any { it.items.isNotEmpty() }) {
            docchiCategories
        } else {
            val aniListCategories = aniListRepository.categories()
            if (aniListCategories.any { it.items.isNotEmpty() }) {
                aniListCategories
            } else {
                kitsuMediaSource.categories()
            }
        }
        return ContentFilter.filterCategories(result, profile())
    }

    override suspend fun search(query: String): List<MediaItem> {
        val result = docchiMediaSource.search(query)
            .ifEmpty { aniListRepository.search(query) }
            .ifEmpty { kitsuMediaSource.search(query) }
        return ContentFilter.filter(result, profile())
    }

    override suspend fun byId(id: String): MediaItem? {
        val result = when {
            id.startsWith("docchi:") -> docchiMediaSource.byId(id)
            else -> aniListRepository.byId(id) ?: kitsuMediaSource.byId(id)
        }
        return result?.takeIf { ContentFilter.isAllowed(it, profile()) }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        return when {
            mediaItem.id.startsWith("docchi:") -> docchiMediaSource.resolve(mediaItem)
            else -> aniListRepository.resolve(mediaItem) ?: kitsuMediaSource.resolve(mediaItem)
        }
    }

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        if (mediaItem.id.startsWith("docchi:")) {
            return docchiMediaSource.resolveItem(mediaItem)
        }
        val resolved = aniListRepository.resolveItem(mediaItem)
        return if (resolved.videoUrl.isNullOrBlank() && !mediaItem.id.startsWith("anilist:")) {
            kitsuMediaSource.resolveItem(mediaItem)
        } else {
            resolved
        }
    }
}
