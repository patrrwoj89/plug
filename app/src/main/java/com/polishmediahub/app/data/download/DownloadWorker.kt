package com.polishmediahub.app.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.polishmediahub.app.data.local.DownloadDao
import com.polishmediahub.app.data.local.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
    private val downloadDao: DownloadDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_EXTENSION = "extension"

        fun enqueueData(mediaId: String, title: String, url: String, extension: String = "mp4") = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    KEY_DOWNLOAD_ID to UUID.randomUUID().toString(),
                    KEY_MEDIA_ID to mediaId,
                    KEY_TITLE to title,
                    KEY_URL to url,
                    KEY_EXTENSION to extension
                )
            )
            .build()
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val mediaId = inputData.getString(KEY_MEDIA_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: ""
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val extension = inputData.getString(KEY_EXTENSION) ?: "mp4"

        val localFile = File(applicationContext.filesDir, "downloads/$downloadId.$extension").apply { parentFile?.mkdirs() }

        downloadDao.upsert(
            DownloadEntity(
                downloadId = downloadId,
                mediaId = mediaId,
                title = title,
                url = url,
                localPath = localFile.absolutePath,
                status = "running"
            )
        )

        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    downloadDao.updateProgress(downloadId, "failed", 0, 0)
                    return Result.retry()
                }

                val total = response.body?.contentLength() ?: -1
                var downloaded = 0L
                response.body?.byteStream()?.use { input ->
                    localFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                downloadDao.updateProgress(downloadId, "running", downloaded, total)
                            }
                        }
                    }
                }
                downloadDao.updateProgress(downloadId, "completed", downloaded, total)
                Result.success()
            }
        } catch (e: Exception) {
            downloadDao.updateProgress(downloadId, "failed", 0, 0)
            Result.retry()
        }
    }
}
