package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "custom_lists",
    primaryKeys = ["profileId", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomListEntity(
    val profileId: String,
    val listId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
