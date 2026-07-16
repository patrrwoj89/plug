package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM iptv_channels ORDER BY COALESCE(channelNumber, '999999'), name")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM iptv_channels WHERE name LIKE :pattern OR groupTitle LIKE :pattern OR tvgName LIKE :pattern OR tvgId LIKE :pattern ORDER BY name COLLATE NOCASE LIMIT :limit")
    suspend fun search(pattern: String, limit: Int = 100): List<ChannelEntity>

    @Query("SELECT * FROM iptv_channels WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM iptv_channels")
    suspend fun deleteAll()

    @Query("DELETE FROM iptv_channels WHERE updatedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
