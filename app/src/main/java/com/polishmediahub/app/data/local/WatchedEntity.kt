package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "watched",
    primaryKeys = ["profileId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WatchedEntity(
    val profileId: String,
    val id: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val watchedAt: Long = System.currentTimeMillis()
)
