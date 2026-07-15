package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "saved_media",
    primaryKeys = ["profileId", "id", "listType"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SavedMediaEntity(
    val profileId: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: String,
    val duration: String,
    val rating: String,
    val videoUrl: String,
    val listType: String, // "library" or "watchlist"
    val addedAt: Long = System.currentTimeMillis()
)
