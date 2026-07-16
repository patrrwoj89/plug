package com.polishmediahub.app.data.local

import android.util.Log
import com.polishmediahub.app.BuildConfig
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedMediaEntity::class,
        WatchedEntity::class,
        CustomListEntity::class,
        CustomListItemEntity::class,
        PluginEntity::class,
        DownloadEntity::class,
        EpgEntity::class,
        ProfileEntity::class,
        AudioHistoryEntity::class,
        ChannelEntity::class,
        FilmwebCacheEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
    abstract fun historyDao(): HistoryDao
    abstract fun pluginDao(): PluginDao
    abstract fun downloadDao(): DownloadDao
    abstract fun epgDao(): EpgDao
    abstract fun profileDao(): ProfileDao
    abstract fun audioHistoryDao(): AudioHistoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun filmwebCacheDao(): FilmwebCacheDao

    companion object {

        private const val TAG = "MediaDatabase"

        /**
         * Stable migrations from every previous schema version to the current one (9).
         * These migrations preserve user data (watch history, library, watchlist, downloads,
         * custom lists, plugins, profiles, audio history) and only rebuild the EPG cache table when needed.
         */
        val MIGRATIONS: Array<Migration> = listOf(
            migrationWithEpgRebuild(1, 7),
            migrationWithEpgRebuild(2, 7),
            migrationWithEpgRebuild(3, 7),
            migrationWithEpgRebuild(4, 7),
            migrationWithEpgRebuild(5, 7),
            migrationFromV6toV7(),
            migrationFromV7toV8(),
            migrationFromV8toV9(),
            migrationFromV9toV10(),
            migrationFromV10toV11(),
            migrationFromV11toV12(),
            migrationFromV12toV13(),
            migrationFromV13toV14()
        ).toTypedArray()

        private fun migrationWithEpgRebuild(startVersion: Int, endVersion: Int): Migration =
            object : Migration(startVersion, endVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createAllTablesIfMissing(db)
                    addMissingColumnsExceptEpg(db)
                    // EPG data is a disposable cache; recreate it so the new schema is guaranteed.
                    db.execSQL("DROP TABLE IF EXISTS epg_entries")
                    createEpgTable(db)
                }
            }

        private fun migrationFromV6toV7(): Migration =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addEpgColumnsIfMissing(db)
                    createEpgIndexIfMissing(db)
                }
            }

        private fun migrationFromV7toV8(): Migration =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    migrateToMultiProfile(db)
                }
            }

        private fun migrateToMultiProfile(db: SupportSQLiteDatabase) {
            val defaultProfileId = "default_profile"

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS profiles (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    avatarUrl TEXT,
                    isPinLocked INTEGER NOT NULL,
                    pinCode TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT OR IGNORE INTO profiles (id, name, avatarUrl, isPinLocked, pinCode) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(defaultProfileId, "Default", null, 0, null)
            )

            // watched
            db.execSQL("DROP TABLE IF EXISTS watched_new")
            db.execSQL(
                """
                CREATE TABLE watched_new (
                    profileId TEXT NOT NULL,
                    id TEXT NOT NULL,
                    positionMs INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    watchedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, id),
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            try {
                db.execSQL(
                    """
                    INSERT INTO watched_new (profileId, id, positionMs, durationMs, watchedAt)
                    SELECT ?, id, positionMs, durationMs, watchedAt FROM watched
                    """.trimIndent(),
                    arrayOf<Any?>(defaultProfileId)
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "migrate watched table failed: ${e.message}", e)
            }
            db.execSQL("DROP TABLE IF EXISTS watched")
            db.execSQL("ALTER TABLE watched_new RENAME TO watched")

            // saved_media
            db.execSQL("DROP TABLE IF EXISTS saved_media_new")
            db.execSQL(
                """
                CREATE TABLE saved_media_new (
                    profileId TEXT NOT NULL,
                    id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    subtitle TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterUrl TEXT NOT NULL,
                    backdropUrl TEXT NOT NULL,
                    year TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    rating TEXT NOT NULL,
                    videoUrl TEXT NOT NULL,
                    listType TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, id, listType),
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            try {
                db.execSQL(
                    """
                    INSERT INTO saved_media_new (profileId, id, title, subtitle, description, posterUrl, backdropUrl, year, duration, rating, videoUrl, listType, addedAt)
                    SELECT ?, id, title, subtitle, description, posterUrl, backdropUrl, year, duration, rating, videoUrl, listType, addedAt FROM saved_media
                    """.trimIndent(),
                    arrayOf<Any?>(defaultProfileId)
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "migrate saved_media table failed: ${e.message}", e)
            }
            db.execSQL("DROP TABLE IF EXISTS saved_media")
            db.execSQL("ALTER TABLE saved_media_new RENAME TO saved_media")

            // custom_lists
            db.execSQL("DROP TABLE IF EXISTS custom_lists_new")
            db.execSQL(
                """
                CREATE TABLE custom_lists_new (
                    profileId TEXT NOT NULL,
                    listId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, listId),
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            try {
                db.execSQL(
                    """
                    INSERT INTO custom_lists_new (profileId, listId, name, createdAt)
                    SELECT ?, listId, name, createdAt FROM custom_lists
                    """.trimIndent(),
                    arrayOf<Any?>(defaultProfileId)
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "migrate custom_lists table failed: ${e.message}", e)
            }
            db.execSQL("DROP TABLE IF EXISTS custom_lists")
            db.execSQL("ALTER TABLE custom_lists_new RENAME TO custom_lists")

            // custom_list_items
            db.execSQL("DROP TABLE IF EXISTS custom_list_items_new")
            db.execSQL(
                """
                CREATE TABLE custom_list_items_new (
                    profileId TEXT NOT NULL,
                    listId TEXT NOT NULL,
                    mediaId TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, listId, mediaId),
                    FOREIGN KEY(profileId, listId) REFERENCES custom_lists(profileId, listId) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            try {
                db.execSQL(
                    """
                    INSERT INTO custom_list_items_new (profileId, listId, mediaId, addedAt)
                    SELECT ?, listId, mediaId, addedAt FROM custom_list_items
                    """.trimIndent(),
                    arrayOf<Any?>(defaultProfileId)
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "migrate custom_list_items table failed: ${e.message}", e)
            }
            db.execSQL("DROP TABLE IF EXISTS custom_list_items")
            db.execSQL("ALTER TABLE custom_list_items_new RENAME TO custom_list_items")
        }

        private fun migrationFromV8toV9(): Migration =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createAudioHistoryTable(db)
                }
            }

        private fun migrationFromV9toV10(): Migration =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addColumnIfMissing(db, "audio_history", "positionMs", "INTEGER NOT NULL DEFAULT 0")
                }
            }

        private fun migrationFromV10toV11(): Migration =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createIptvChannelsTable(db)
                }
            }

        private fun migrationFromV11toV12(): Migration =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addProfileParentalControlColumns(db)
                }
            }

        private fun migrationFromV12toV13(): Migration =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    addWatchedAndSavedMediaCrossIdColumns(db)
                }
            }

        private fun addWatchedAndSavedMediaCrossIdColumns(db: SupportSQLiteDatabase) {
            // watched table metadata cache and cross-IDs for Trakt two-way sync
            addColumnIfMissing(db, "watched", "title", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "subtitle", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "description", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "posterUrl", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "backdropUrl", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "year", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "type", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "season", "INTEGER")
            addColumnIfMissing(db, "watched", "episode", "INTEGER")
            addColumnIfMissing(db, "watched", "tmdbId", "INTEGER")
            addColumnIfMissing(db, "watched", "traktId", "INTEGER")
            addColumnIfMissing(db, "watched", "imdbId", "TEXT")

            // saved_media cross-IDs for Trakt watchlist matching
            addColumnIfMissing(db, "saved_media", "tmdbId", "INTEGER")
            addColumnIfMissing(db, "saved_media", "traktId", "INTEGER")
            addColumnIfMissing(db, "saved_media", "imdbId", "TEXT")
            addColumnIfMissing(db, "saved_media", "season", "INTEGER")
            addColumnIfMissing(db, "saved_media", "episode", "INTEGER")
        }

        private fun addProfileParentalControlColumns(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "profiles", "maxAgeRating", "TEXT")
            addColumnIfMissing(db, "profiles", "allowNsfw", "INTEGER NOT NULL DEFAULT 0")
        }

        private fun createAudioHistoryTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS audio_history (
                    profileId TEXT NOT NULL,
                    trackId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT NOT NULL,
                    coverUrl TEXT,
                    streamUrl TEXT,
                    durationMs INTEGER NOT NULL,
                    positionMs INTEGER NOT NULL DEFAULT 0,
                    playedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, trackId),
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }

        private fun createAllTablesIfMissing(db: SupportSQLiteDatabase) {
            createSavedMediaTable(db)
            createWatchedTable(db)
            createCustomListsTable(db)
            createCustomListItemsTable(db)
            createPluginsTable(db)
            createDownloadsTable(db)
            createEpgTable(db)
            createIptvChannelsTable(db)
            createFilmwebCacheTable(db)
        }

        private fun createSavedMediaTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_media (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    subtitle TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterUrl TEXT NOT NULL,
                    backdropUrl TEXT NOT NULL,
                    year TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    rating TEXT NOT NULL,
                    videoUrl TEXT NOT NULL,
                    listType TEXT NOT NULL,
                    addedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createWatchedTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS watched (
                    id TEXT PRIMARY KEY NOT NULL,
                    positionMs INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    watchedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createCustomListsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS custom_lists (
                    listId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createCustomListItemsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS custom_list_items (
                    listId TEXT NOT NULL,
                    mediaId TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    PRIMARY KEY(listId, mediaId)
                )
                """.trimIndent()
            )
        }

        private fun createPluginsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS plugins (
                    pluginId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    manifestUrl TEXT NOT NULL,
                    manifestJson TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createDownloadsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS downloads (
                    downloadId TEXT PRIMARY KEY NOT NULL,
                    mediaId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL,
                    localPath TEXT NOT NULL,
                    status TEXT NOT NULL,
                    bytesDownloaded INTEGER NOT NULL,
                    totalBytes INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createEpgTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS epg_entries (
                    id TEXT PRIMARY KEY NOT NULL,
                    channelId TEXT NOT NULL,
                    channelName TEXT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    year TEXT,
                    category TEXT,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER NOT NULL,
                    iconUrl TEXT
                )
                """.trimIndent()
            )
            createEpgIndexIfMissing(db)
        }

        private fun migrationFromV13toV14(): Migration =
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createFilmwebCacheTable(db)
                }
            }

        private fun createFilmwebCacheTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS filmweb_cache (
                    cacheKey TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    year TEXT NOT NULL,
                    description TEXT NOT NULL,
                    posterUrl TEXT,
                    rating TEXT,
                    voteCount TEXT,
                    filmwebUrl TEXT,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createIptvChannelsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS iptv_channels (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    logoUrl TEXT,
                    groupTitle TEXT,
                    streamUrl TEXT NOT NULL,
                    channelNumber TEXT,
                    tvgId TEXT,
                    tvgName TEXT,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun createEpgIndexIfMissing(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_epg_entries_channelId_startTime ON epg_entries(channelId, startTime)"
            )
        }

        private fun addMissingColumnsExceptEpg(db: SupportSQLiteDatabase) {
            // These tables have been stable for a long time; if any of them are missing columns
            // in an old pre-release schema, add them defensively.
            addColumnIfMissing(db, "saved_media", "addedAt", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "custom_lists", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "custom_list_items", "addedAt", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "plugins", "enabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "plugins", "sortOrder", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "downloads", "bytesDownloaded", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "downloads", "totalBytes", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "profiles", "maxAgeRating", "TEXT")
            addColumnIfMissing(db, "profiles", "allowNsfw", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "epg_entries", "channelId", "TEXT")

            // Trakt sync metadata / cross-IDs (also added in v12->v13 migration)
            addColumnIfMissing(db, "watched", "title", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "subtitle", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "description", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "posterUrl", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "backdropUrl", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "year", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "type", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "watched", "season", "INTEGER")
            addColumnIfMissing(db, "watched", "episode", "INTEGER")
            addColumnIfMissing(db, "watched", "tmdbId", "INTEGER")
            addColumnIfMissing(db, "watched", "traktId", "INTEGER")
            addColumnIfMissing(db, "watched", "imdbId", "TEXT")
            addColumnIfMissing(db, "saved_media", "tmdbId", "INTEGER")
            addColumnIfMissing(db, "saved_media", "traktId", "INTEGER")
            addColumnIfMissing(db, "saved_media", "imdbId", "TEXT")
            addColumnIfMissing(db, "saved_media", "season", "INTEGER")
            addColumnIfMissing(db, "saved_media", "episode", "INTEGER")
        }

        private fun addEpgColumnsIfMissing(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "epg_entries", "channelName", "TEXT")
            addColumnIfMissing(db, "epg_entries", "year", "TEXT")
            addColumnIfMissing(db, "epg_entries", "category", "TEXT")
            createEpgIndexIfMissing(db)
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            type: String
        ) {
            try {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "addColumnIfMissing failed for $table.$column: ${e.message}", e)
            }
        }
    }
}
