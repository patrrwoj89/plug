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
    /** Salted PBKDF2-HMAC-SHA256 hash of the profile PIN (see PinSecurity); never stored in clear text. */
    val pinCode: String? = null,
    val maxAgeRating: String? = null,
    val allowNsfw: Boolean = false
)
