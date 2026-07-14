package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.HistoryDao
import com.polishmediahub.app.data.local.WatchedEntity
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val mediaRepository: MediaRepository
) {

    fun observeHistory(): Flow<List<Pair<MediaItem, WatchedEntity>>> =
        historyDao.observeAll().map { list ->
            list.mapNotNull { entity ->
                mediaRepository.byId(entity.id)?.let { it to entity }
            }
        }

    fun observePosition(id: String): Flow<Long> =
        historyDao.observeById(id).map { it?.positionMs ?: 0L }

    suspend fun updatePosition(id: String, positionMs: Long, durationMs: Long) {
        historyDao.upsert(
            WatchedEntity(
                id = id,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
    }

    suspend fun remove(id: String) = historyDao.delete(id)
    suspend fun clear() = historyDao.clearAll()
}
