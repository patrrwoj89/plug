package com.tvhub.skeleton.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedMediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
}
