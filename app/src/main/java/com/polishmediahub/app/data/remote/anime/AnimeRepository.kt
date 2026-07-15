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
 * Anime media repository with reactive Kitsu fallback.
 *
 * Tries AniList GraphQL first; if it returns an empty result (network error,
 * timeout or rate-limit), silently falls back to Kitsu JSON:API.
 * Results are filtered through [ContentFilter] using the active profile.
 */
@Singleton
class AnimeRepository @Inject constructor(
    private val aniListRepository: AniListMediaRepository,
    private val kitsuMediaSource: KitsuMediaSource,
    private val profileRepository: ProfileRepository
) : MediaRepository {

    private suspend fun profile() = profileRepository.currentProfile.first()

    override suspend fun featured(): List<MediaItem> {
        val result = aniListRepository.featured().ifEmpty { kitsuMediaSource.featured() }
        return ContentFilter.filter(result, profile())
    }

    override suspend fun categories(): List<Category> {
        val result = aniListRepository.categories().ifEmpty { kitsuMediaSource.categories() }
        return ContentFilter.filterCategories(result, profile())
    }

    override suspend fun search(query: String): List<MediaItem> {
        val result = aniListRepository.search(query).ifEmpty { kitsuMediaSource.search(query) }
        return ContentFilter.filter(result, profile())
    }

    override suspend fun byId(id: String): MediaItem? {
        val result = aniListRepository.byId(id) ?: kitsuMediaSource.byId(id)
        return result?.takeIf { ContentFilter.isAllowed(it, profile()) }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        return aniListRepository.resolve(mediaItem) ?: kitsuMediaSource.resolve(mediaItem)
    }

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        val resolved = aniListRepository.resolveItem(mediaItem)
        return if (resolved.videoUrl.isNullOrBlank() && !mediaItem.id.startsWith("anilist:")) {
            kitsuMediaSource.resolveItem(mediaItem)
        } else {
            resolved
        }
    }
}
