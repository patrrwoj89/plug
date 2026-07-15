package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.HistoryDao
import com.polishmediahub.app.data.local.WatchedEntity
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
class WatchHistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val profileRepository: ProfileRepository,
    private val mediaRepository: MediaRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHistory(): Flow<List<Pair<MediaItem, WatchedEntity>>> =
        profileRepository.currentProfile.filterNotNull().flatMapLatest { profile ->
            historyDao.observeAll(profile.id).map { list ->
                list.mapNotNull { entity ->
                    val item = mediaRepository.byId(entity.id) ?: entity.toMediaItem()
                    if (item != null) item to entity else null
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePosition(id: String): Flow<Long> =
        profileRepository.currentProfile.filterNotNull().flatMapLatest { profile ->
            historyDao.observeById(profile.id, id).map { it?.positionMs ?: 0L }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isWatched(id: String): Flow<Boolean> =
        profileRepository.currentProfile.filterNotNull().flatMapLatest { profile ->
            historyDao.observeById(profile.id, id).map { entity ->
                entity != null && (entity.positionMs > 0 || entity.durationMs > 0)
            }
        }

    suspend fun updatePosition(item: MediaItem, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.upsert(item.toWatchedEntity(profileId, positionMs, durationMs))
    }

    suspend fun updatePosition(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.upsert(
            WatchedEntity(
                profileId = profileId,
                id = id,
                positionMs = positionMs,
                durationMs = durationMs,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addOrUpdateHistory(
        item: MediaItem,
        positionMs: Long = 0,
        durationMs: Long = 0,
        watchedAt: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.upsert(item.toWatchedEntity(profileId, positionMs, durationMs, watchedAt))
    }

    suspend fun getAllForCurrentProfile(): List<WatchedEntity> = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext emptyList()
        historyDao.getAll(profileId)
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.delete(profileId, id)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.clearAll(profileId)
    }

    private fun currentProfileId(): String? = profileRepository.currentProfile.value?.id

    private fun MediaItem.toWatchedEntity(
        profileId: String,
        positionMs: Long,
        durationMs: Long,
        watchedAt: Long = System.currentTimeMillis()
    ) = WatchedEntity(
        profileId = profileId,
        id = id,
        positionMs = positionMs,
        durationMs = durationMs,
        watchedAt = watchedAt,
        title = title,
        subtitle = subtitle,
        description = description,
        posterUrl = posterUrl.orEmpty(),
        backdropUrl = backdropUrl.orEmpty(),
        year = year,
        type = type.name,
        season = season,
        episode = episode,
        tmdbId = tmdbId,
        traktId = traktId,
        imdbId = imdbId
    )

    private fun WatchedEntity.toMediaItem(): MediaItem? {
        if (title.isBlank()) return null
        return MediaItem(
            id = id,
            title = title,
            subtitle = subtitle,
            description = description,
            posterUrl = posterUrl.takeIf { it.isNotBlank() },
            backdropUrl = backdropUrl.takeIf { it.isNotBlank() },
            year = year,
            type = runCatching { MediaItem.Type.valueOf(type) }.getOrDefault(MediaItem.Type.MOVIE),
            season = season,
            episode = episode,
            tmdbId = tmdbId,
            traktId = traktId,
            imdbId = imdbId
        )
    }
}
