package com.polishmediahub.app.data.remote.cloud

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.concurrent.TimeUnit

/**
 * Background worker that uploads the Room database files to the Cloudflare Worker endpoint
 * [POST /api/sync-profiles] every 12 hours, or downloads them [GET /api/sync-profiles] when
 * triggered with [OPERATION_DOWNLOAD]. The restored files are staged for safe replacement before
 * the next app cold start (see [CloudProfileSyncRestore]).
 */
@HiltWorker
class CloudProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiConfigRepository: ApiConfigRepository,
    private val cloudProfileSyncClient: CloudProfileSyncClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val workerUrl = apiConfigRepository.cloudflareWorkerUrl.first()
            val token = apiConfigRepository.cloudflareAuthToken.first()
            if (workerUrl.isBlank() || token.isBlank()) {
                apiConfigRepository.setLastProfileSync(
                    System.currentTimeMillis(),
                    "error",
                    "Worker URL or token not configured"
                )
                return@withContext Result.failure()
            }

            val operation = inputData.getString(KEY_OPERATION) ?: OPERATION_UPLOAD
            val result = when (operation) {
                OPERATION_DOWNLOAD -> downloadAndStage(workerUrl, token)
                else -> uploadBackup(workerUrl, token)
            }
            result
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Profile sync failed", e)
            apiConfigRepository.setLastProfileSync(
                System.currentTimeMillis(),
                "error",
                e.message
            )
            Result.retry()
        }
    }

    private suspend fun uploadBackup(workerUrl: String, token: String): Result {
        val dbFile = applicationContext.getDatabasePath(DATABASE_NAME)
            ?: return Result.failure()

        val files = listOfNotNull(
            dbFile,
            File("${dbFile.path}-shm").takeIf { it.exists() },
            File("${dbFile.path}-wal").takeIf { it.exists() }
        )

        val zipBytes = zipFiles(files)
        if (zipBytes.isEmpty()) {
            apiConfigRepository.setLastProfileSync(
                System.currentTimeMillis(),
                "error",
                "Database archive is empty"
            )
            return Result.failure()
        }

        val uploadResult = cloudProfileSyncClient.uploadProfileBackup(zipBytes, workerUrl, token)
        return if (uploadResult.isSuccess) {
            apiConfigRepository.setLastProfileSync(System.currentTimeMillis(), "success", null)
            Result.success()
        } else {
            apiConfigRepository.setLastProfileSync(
                System.currentTimeMillis(),
                "error",
                uploadResult.exceptionOrNull()?.message
            )
            Result.retry()
        }
    }

    private suspend fun downloadAndStage(workerUrl: String, token: String): Result {
        val downloadResult = cloudProfileSyncClient.downloadProfileBackup(workerUrl, token)
        val bytes = downloadResult.getOrElse { error ->
            apiConfigRepository.setLastProfileSync(
                System.currentTimeMillis(),
                "error",
                error.message
            )
            return Result.retry()
        }

        val restoreDir = File(applicationContext.filesDir, RESTORE_DIR)
        restoreDir.deleteRecursively()
        restoreDir.mkdirs()

        unzipFiles(bytes, restoreDir)

        File(applicationContext.filesDir, RESTORE_PENDING_MARKER).writeText("1")

        apiConfigRepository.setLastProfileSync(
            System.currentTimeMillis(),
            "success",
            "pending_restart"
        )
        return Result.success()
    }

    private fun zipFiles(files: List<File>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            files.forEach { file ->
                FileInputStream(file).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        bis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }
        return out.toByteArray()
    }

    private fun unzipFiles(bytes: ByteArray, destination: File) {
        ByteArrayInputStream(bytes).use { bais ->
            ZipInputStream(bais).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destination, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    companion object {
        private const val TAG = "CloudProfileSyncWorker"
        private const val WORK_NAME = "cloud_profile_sync_periodic"
        private const val IMMEDIATE_WORK_NAME = "cloud_profile_sync_immediate"

        internal const val DATABASE_NAME = "media.db"
        internal const val RESTORE_DIR = "pending_profile_restore"
        internal const val RESTORE_PENDING_MARKER = "profile_restore_pending"

        internal const val KEY_OPERATION = "operation"
        internal const val OPERATION_UPLOAD = "upload"
        internal const val OPERATION_DOWNLOAD = "download"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<CloudProfileSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun startBackup(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudProfileSyncWorker>()
                .setInputData(Data.Builder().putString(KEY_OPERATION, OPERATION_UPLOAD).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun startRestore(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudProfileSyncWorker>()
                .setInputData(Data.Builder().putString(KEY_OPERATION, OPERATION_DOWNLOAD).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
