package com.tvhub.skeleton.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_media")
data class SavedMediaEntity(
    @PrimaryKey val id: String,
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
