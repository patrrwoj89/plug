package com.tvhub.skeleton.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedMediaEntity::class,
        WatchedEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        PluginEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
    abstract fun historyDao(): HistoryDao
    abstract fun pluginDao(): PluginDao
}
