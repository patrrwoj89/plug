package com.polishmediahub.app.data.tv

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class RecommendationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tvLauncherManager: TvLauncherManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            tvLauncherManager.syncRecommendations()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "recommendations_work"
        private const val REPEAT_INTERVAL_HOURS = 24L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecommendationsWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
