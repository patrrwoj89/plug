package com.polishmediahub.app.data.remote.cloud

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import java.io.File

/**
 * Applies a staged profile backup before Room opens the database for the first time.
 *
 * This is invoked very early in [com.polishmediahub.app.TVHubApplication.onCreate] so that the
 * restored `media.db`, `media.db-shm` and `media.db-wal` files are in place before any DAO or
 * repository touches the database (Zasada 5, Room migration safety).
 */
object CloudProfileSyncRestore {

    fun restoreIfNeeded(context: Context) {
        val marker = File(context.filesDir, CloudProfileSyncWorker.RESTORE_PENDING_MARKER)
        if (!marker.exists()) return

        val restoreDir = File(context.filesDir, CloudProfileSyncWorker.RESTORE_DIR)
        if (!restoreDir.exists() || !restoreDir.isDirectory) {
            marker.delete()
            return
        }

        val dbFile = context.getDatabasePath(CloudProfileSyncWorker.DATABASE_NAME)
            ?: return

        try {
            // Room must not hold the file open; because this runs before any DAO access,
            // the database files can be replaced safely.
            File("${dbFile.path}-shm").delete()
            File("${dbFile.path}-wal").delete()
            dbFile.delete()

            restoreDir.listFiles()?.forEach { file ->
                when (file.name) {
                    CloudProfileSyncWorker.DATABASE_NAME -> file.copyTo(dbFile, overwrite = true)
                    "${CloudProfileSyncWorker.DATABASE_NAME}-shm" -> file.copyTo(
                        File("${dbFile.path}-shm"),
                        overwrite = true
                    )
                    "${CloudProfileSyncWorker.DATABASE_NAME}-wal" -> file.copyTo(
                        File("${dbFile.path}-wal"),
                        overwrite = true
                    )
                    else -> file.copyTo(File(dbFile.parentFile, file.name), overwrite = true)
                }
            }

            restoreDir.deleteRecursively()
            marker.delete()
            if (BuildConfig.DEBUG) Log.d(TAG, "Profile backup restored from cloud")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Profile restore failed", e)
        }
    }

    private const val TAG = "CloudProfileSyncRestore"
}
