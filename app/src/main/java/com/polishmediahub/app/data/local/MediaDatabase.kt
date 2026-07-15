package com.polishmediahub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedMediaEntity::class,
        WatchedEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        PluginEntity::class,
        DownloadEntity::class,
        EpgEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
    abstract fun historyDao(): HistoryDao
    abstract fun pluginDao(): PluginDao
    abstract fun downloadDao(): DownloadDao
    abstract fun epgDao(): EpgDao
}
