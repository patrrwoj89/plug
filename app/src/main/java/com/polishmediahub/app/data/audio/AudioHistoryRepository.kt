package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.local.AudioHistoryDao
import com.polishmediahub.app.data.local.AudioHistoryEntity
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioHistoryRepository @Inject constructor(
    private val audioHistoryDao: AudioHistoryDao,
    private val profileRepository: ProfileRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHistory(): Flow<List<AudioHistoryEntity>> =
        profileRepository.currentProfile.filterNotNull().flatMapLatest { profile ->
            audioHistoryDao.observeAll(profile.id)
        }

    suspend fun getPosition(trackId: String): Long = withContext(Dispatchers.IO) {
        val profileId = profileRepository.currentProfile.value?.id ?: return@withContext 0L
        audioHistoryDao.getPosition(profileId, trackId) ?: 0L
    }

    suspend fun save(track: AudioTrack, positionMs: Long = 0) = withContext(Dispatchers.IO) {
        val profileId = profileRepository.currentProfile.value?.id ?: return@withContext
        audioHistoryDao.upsert(
            AudioHistoryEntity(
                profileId = profileId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                coverUrl = track.coverUrl,
                streamUrl = track.streamUrl,
                durationMs = track.durationMs,
                positionMs = positionMs,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun remove(trackId: String) = withContext(Dispatchers.IO) {
        val profileId = profileRepository.currentProfile.value?.id ?: return@withContext
        audioHistoryDao.delete(profileId, trackId)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        val profileId = profileRepository.currentProfile.value?.id ?: return@withContext
        audioHistoryDao.clearAll(profileId)
    }
}
