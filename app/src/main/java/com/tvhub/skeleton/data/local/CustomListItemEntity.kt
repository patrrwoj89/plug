package com.tvhub.skeleton.data.local

import androidx.room.Entity

@Entity(
    tableName = "custom_list_items",
    primaryKeys = ["listId", "mediaId"]
)
data class CustomListItemEntity(
    val listId: String,
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)
