package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "custom_list_items",
    primaryKeys = ["profileId", "listId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomListEntity::class,
            parentColumns = ["profileId", "listId"],
            childColumns = ["profileId", "listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomListItemEntity(
    val profileId: String,
    val listId: String,
    val mediaId: String,
    val addedAt: Long = System.currentTimeMillis()
)
