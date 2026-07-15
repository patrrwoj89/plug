package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_entries",
    indices = [Index(value = ["channelId", "startTime"])]
)
data class EpgEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val channelName: String? = null,
    val title: String,
    val description: String = "",
    val year: String? = null,
    val category: String? = null,
    val startTime: Long,
    val endTime: Long,
    val iconUrl: String? = null
)
