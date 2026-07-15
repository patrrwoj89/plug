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
    val watchedAt: Long = System.currentTimeMillis(),
    // Cached metadata so Trakt-synced items can appear in Continue Watching even when
    // the original source plugin is offline or the ID is Trakt-only.
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val year: String = "",
    val type: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val tmdbId: Int? = null,
    val traktId: Int? = null,
    val imdbId: String? = null
)
