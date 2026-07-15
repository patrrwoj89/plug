package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FilmwebCacheDao {

    @Query("SELECT * FROM filmweb_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): FilmwebCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FilmwebCacheEntity)

    @Query("DELETE FROM filmweb_cache WHERE updatedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
