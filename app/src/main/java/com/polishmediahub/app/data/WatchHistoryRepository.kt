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
                    mediaRepository.byId(entity.id)?.let { it to entity }
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

    suspend fun updatePosition(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.upsert(
            WatchedEntity(
                profileId = profileId,
                id = id,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
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
}
