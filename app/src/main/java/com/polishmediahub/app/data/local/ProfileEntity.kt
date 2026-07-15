package com.polishmediahub.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val isPinLocked: Boolean = false,
    val pinCode: String? = null
)
