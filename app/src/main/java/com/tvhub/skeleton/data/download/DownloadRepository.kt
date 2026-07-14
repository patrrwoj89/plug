package com.tvhub.skeleton.data.download

import android.content.Context
import androidx.work.WorkManager
import com.tvhub.skeleton.data.local.DownloadDao
import com.tvhub.skeleton.data.local.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val downloadDao: DownloadDao
) {

    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeAll()

    fun startDownload(mediaId: String, title: String, url: String) {
        val request = DownloadWorker.enqueueData(mediaId, title, url)
        workManager.enqueue(request)
    }

    suspend fun deleteDownload(downloadId: String) {
        downloadDao.getById(downloadId)?.let {
            File(it.localPath).delete()
        }
        downloadDao.delete(downloadId)
        workManager.cancelWorkById(java.util.UUID.fromString(downloadId))
    }
}
