package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.SavedMediaDao
import com.polishmediahub.app.data.local.SavedMediaEntity
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedMediaRepository @Inject constructor(
    private val dao: SavedMediaDao,
    private val profileRepository: ProfileRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLibrary(): Flow<List<MediaItem>> =
        currentProfileIdFlow().flatMapLatest { dao.observeByType(it, TYPE_LIBRARY) }
            .map { it.map(::toModel) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWatchlist(): Flow<List<MediaItem>> =
        currentProfileIdFlow().flatMapLatest { dao.observeByType(it, TYPE_WATCHLIST) }
            .map { it.map(::toModel) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isInLibrary(id: String): Flow<Boolean> =
        currentProfileIdFlow().flatMapLatest { dao.isSaved(it, id, TYPE_LIBRARY) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isInWatchlist(id: String): Flow<Boolean> =
        currentProfileIdFlow().flatMapLatest { dao.isSaved(it, id, TYPE_WATCHLIST) }

    suspend fun addToLibrary(item: MediaItem, addedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        dao.insert(item.toEntity(profileId, TYPE_LIBRARY, addedAt))
    }

    suspend fun addToWatchlist(item: MediaItem, addedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        dao.insert(item.toEntity(profileId, TYPE_WATCHLIST, addedAt))
    }

    suspend fun removeFromLibrary(id: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        dao.deleteByIdAndType(profileId, id, TYPE_LIBRARY)
    }

    suspend fun removeFromWatchlist(id: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        dao.deleteByIdAndType(profileId, id, TYPE_WATCHLIST)
    }

    suspend fun getWatchlist(): List<SavedMediaEntity> = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext emptyList()
        dao.getByType(profileId, TYPE_WATCHLIST)
    }

    suspend fun addToWatchlist(entity: SavedMediaEntity) = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    private fun currentProfileIdFlow() = profileRepository.currentProfile.filterNotNull().map { it.id }

    private fun currentProfileId(): String? = profileRepository.currentProfile.value?.id

    private fun MediaItem.toEntity(profileId: String, listType: String, addedAt: Long) = SavedMediaEntity(
        profileId = profileId,
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        posterUrl = posterUrl.orEmpty(),
        backdropUrl = backdropUrl.orEmpty(),
        year = year,
        duration = duration,
        rating = rating,
        videoUrl = videoUrl.orEmpty(),
        listType = listType,
        addedAt = addedAt,
        tmdbId = tmdbId,
        traktId = traktId,
        imdbId = imdbId,
        season = season,
        episode = episode
    )

    private fun toModel(entity: SavedMediaEntity) = MediaItem(
        id = entity.id,
        title = entity.title,
        subtitle = entity.subtitle,
        description = entity.description,
        posterUrl = entity.posterUrl,
        backdropUrl = entity.backdropUrl,
        year = entity.year,
        duration = entity.duration,
        rating = entity.rating,
        videoUrl = entity.videoUrl.takeIf { it.isNotBlank() },
        tmdbId = entity.tmdbId,
        traktId = entity.traktId,
        imdbId = entity.imdbId,
        season = entity.season,
        episode = entity.episode
    )

    companion object {
        const val TYPE_LIBRARY = "library"
        const val TYPE_WATCHLIST = "watchlist"
    }
}
