package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_entries WHERE channelId = :channelId AND startTime <= :time AND endTime > :time ORDER BY startTime LIMIT 1")
    suspend fun getCurrentForChannel(channelId: String, time: Long): EpgEntity?

    @Query("SELECT * FROM epg_entries WHERE channelId = :channelId AND startTime >= :from AND startTime < :to ORDER BY startTime")
    fun observeForChannel(channelId: String, from: Long, to: Long): Flow<List<EpgEntity>>

    @Query("SELECT * FROM epg_entries WHERE channelId IN (:channelIds) AND startTime >= :from AND startTime < :to AND endTime > :from ORDER BY channelId, startTime")
    fun observeForChannels(channelIds: List<String>, from: Long, to: Long): Flow<List<EpgEntity>>

    @Query("SELECT DISTINCT channelId, channelName FROM epg_entries WHERE startTime >= :from AND endTime <= :to ORDER BY channelId")
    fun observeDistinctChannels(from: Long, to: Long): Flow<List<EpgChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<EpgEntity>)

    @Query("DELETE FROM epg_entries WHERE endTime < :time")
    suspend fun deleteOlderThan(time: Long)
}

data class EpgChannel(
    val channelId: String,
    val channelName: String?
)
