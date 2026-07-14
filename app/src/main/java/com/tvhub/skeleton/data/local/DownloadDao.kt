package com.tvhub.skeleton.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY downloadId DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes WHERE downloadId = :id")
    suspend fun updateProgress(id: String, status: String, bytesDownloaded: Long, totalBytes: Long)

    @Query("DELETE FROM downloads WHERE downloadId = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM downloads WHERE downloadId = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?
}
