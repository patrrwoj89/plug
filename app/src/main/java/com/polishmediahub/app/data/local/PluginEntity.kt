package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val pluginId: String,
    val name: String,
    val manifestUrl: String,
    val manifestJson: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0
)
