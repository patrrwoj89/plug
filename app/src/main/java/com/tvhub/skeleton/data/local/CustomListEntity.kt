package com.tvhub.skeleton.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey val listId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
