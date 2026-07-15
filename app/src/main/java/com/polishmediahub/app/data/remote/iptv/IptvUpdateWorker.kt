package com.polishmediahub.app.data.remote.iptv

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.iptv.EpgRepository
import com.polishmediahub.app.data.local.ChannelDao
import com.polishmediahub.app.data.local.ChannelEntity
import com.polishmediahub.app.model.MediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class IptvUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiConfigRepository: ApiConfigRepository,
    private val iptvRepository: IptvRepository,
    private val epgRepository: EpgRepository,
    private val channelDao: ChannelDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var m3uSuccess = false
        var epgSuccess = false
        var errorMessage: String? = null

        try {
            val urls = apiConfigRepository.iptvSourceUrls.first()
            if (urls.isNotBlank()) {
                val items = iptvRepository.loadChannels().distinctBy { it.id }
                channelDao.upsertAll(items.map { it.toChannelEntity() })
                m3uSuccess = true
                Log.d(TAG, "Cached ${items.size} IPTV channels")
            } else {
                m3uSuccess = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "M3U update failed: ${e.message}", e)
            errorMessage = e.message
        }

        try {
            val epgUrl = apiConfigRepository.epgUrl.first()
            if (epgUrl.isNotBlank()) {
                epgRepository.loadEpg(epgUrl)
                epgSuccess = true
            } else {
                epgSuccess = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "EPG update failed: ${e.message}", e)
            errorMessage = e.message
        }

        val status = if (m3uSuccess && epgSuccess) "success" else "error"
        apiConfigRepository.setLastEpgSync(System.currentTimeMillis(), status, errorMessage)

        if (m3uSuccess && epgSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun MediaItem.toChannelEntity(): ChannelEntity = ChannelEntity(
        id = id,
        name = title,
        logoUrl = posterUrl,
        groupTitle = genres.firstOrNull(),
        streamUrl = videoUrl ?: "",
        channelNumber = channelNumber,
        tvgId = tvgId,
        tvgName = subtitle.takeIf { it.isNotBlank() }
    )

    companion object {
        private const val TAG = "IptvUpdateWorker"
        private const val WORK_NAME = "iptv_update_periodic"
        private const val IMMEDIATE_WORK_NAME = "iptv_update_immediate"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<IptvUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun startImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = OneTimeWorkRequestBuilder<IptvUpdateWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
