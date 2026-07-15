package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filmweb_cache")
data class FilmwebCacheEntity(
    @PrimaryKey val cacheKey: String,
    val title: String,
    val year: String,
    val description: String,
    val posterUrl: String?,
    val rating: String?,
    val voteCount: String?,
    val filmwebUrl: String?,
    val updatedAt: Long = System.currentTimeMillis()
)
