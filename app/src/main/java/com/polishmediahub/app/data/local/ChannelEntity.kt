package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "iptv_channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val streamUrl: String,
    val channelNumber: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
