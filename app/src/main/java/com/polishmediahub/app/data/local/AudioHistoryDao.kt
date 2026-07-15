package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioHistoryDao {

    @Query("SELECT * FROM audio_history WHERE profileId = :profileId ORDER BY playedAt DESC")
    fun observeAll(profileId: String): Flow<List<AudioHistoryEntity>>

    @Query("SELECT positionMs FROM audio_history WHERE profileId = :profileId AND trackId = :trackId")
    suspend fun getPosition(profileId: String, trackId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AudioHistoryEntity)

    @Query("DELETE FROM audio_history WHERE profileId = :profileId AND trackId = :trackId")
    suspend fun delete(profileId: String, trackId: String)

    @Query("DELETE FROM audio_history WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String)
}
