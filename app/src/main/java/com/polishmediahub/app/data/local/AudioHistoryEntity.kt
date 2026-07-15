package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "audio_history",
    primaryKeys = ["profileId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AudioHistoryEntity(
    val profileId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val streamUrl: String?,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val playedAt: Long = System.currentTimeMillis()
)
