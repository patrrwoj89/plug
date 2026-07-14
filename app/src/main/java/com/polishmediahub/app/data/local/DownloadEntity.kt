package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val downloadId: String,
    val mediaId: String,
    val title: String,
    val url: String,
    val localPath: String,
    val status: String,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0
)
