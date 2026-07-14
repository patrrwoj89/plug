package com.tvhub.skeleton.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {

    @Query("SELECT * FROM plugins ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE pluginId = :id LIMIT 1")
    suspend fun getById(id: String): PluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plugin: PluginEntity)

    @Query("DELETE FROM plugins WHERE pluginId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM plugins")
    suspend fun clearAll()

    @Query("UPDATE plugins SET sortOrder = :order WHERE pluginId = :id")
    suspend fun updateOrder(id: String, order: Int)
}
