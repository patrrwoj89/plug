package com.tvhub.skeleton.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMediaDao {

    @Query("SELECT * FROM saved_media WHERE listType = :listType ORDER BY addedAt DESC")
    fun observeByType(listType: String): Flow<List<SavedMediaEntity>>

    @Query("SELECT * FROM saved_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedMediaEntity)

    @Delete
    suspend fun delete(entity: SavedMediaEntity)

    @Query("DELETE FROM saved_media WHERE id = :id AND listType = :listType")
    suspend fun deleteByIdAndType(id: String, listType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_media WHERE id = :id AND listType = :listType)")
    fun isSaved(id: String, listType: String): Flow<Boolean>
}
