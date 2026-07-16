package com.polishmediahub.app.data.plugin

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
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks configured plugin manifests and repository indexes for updates
 * every 24 hours when the device is idle. When newer JS manifests or binary DEX plugins are
 * detected, the optimized DEX cache is cleared so the updated files are loaded on next use.
 * The update count is exposed as a notification badge in Settings and the admin panel.
 */
@HiltWorker
class PluginUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pluginRepository: PluginRepository,
    private val apiConfigRepository: ApiConfigRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val updatedManifests = pluginRepository.checkUpdates()
            pluginRepository.syncIndexes()
            val availablePlugins = pluginRepository.availablePlugins.value

            val updateCount = updatedManifests + availablePlugins.size

            if (updateCount > 0) {
                pluginRepository.clearDexCache()
            }

            apiConfigRepository.setLastPluginUpdate(
                count = updateCount,
                timestamp = System.currentTimeMillis()
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Plugin update check finished: $updateCount updates available")
            }

            Result.success()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Plugin update check failed", e)
            apiConfigRepository.setLastPluginUpdate(0, System.currentTimeMillis(), e.message)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PluginUpdateWorker"
        private const val WORK_NAME = "plugin_update_periodic"
        private const val IMMEDIATE_WORK_NAME = "plugin_update_immediate"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PluginUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun startImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PluginUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
