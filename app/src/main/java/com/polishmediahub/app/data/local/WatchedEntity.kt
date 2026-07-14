package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched")
data class WatchedEntity(
    @PrimaryKey val id: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val watchedAt: Long = System.currentTimeMillis()
)
