package com.polishmediahub.app.data.remote.health

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltWorker
class HealthCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiConfigRepository: ApiConfigRepository,
    private val okHttpClient: OkHttpClient,
    private val engine: HealthCheckEngine,
    private val json: Json
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dispatcher = Dispatcher()
        val connectionPool = ConnectionPool(0, 1, TimeUnit.SECONDS)
        val client = okHttpClient.newBuilder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()

        try {
            val config = buildConfig()
            val status = engine.runChecks(client, config)
            val statusJson = json.encodeToString(HealthStatus.serializer(), status)
            apiConfigRepository.setHealthStatuses(statusJson)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Health check completed for ${status.sources.size} sources")
            }
            Result.success()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Health check failed: ${e.message}", e)
            }
            Result.retry()
        } finally {
            dispatcher.cancelAll()
            connectionPool.evictAll()
        }
    }

    private suspend fun buildConfig(): HealthConfig = HealthConfig(
        kodiUrl = apiConfigRepository.kodiUrl.first(),
        iptvUrls = split(apiConfigRepository.iptvSourceUrls.first()),
        epgUrl = apiConfigRepository.epgUrl.first(),
        stremioAddons = split(apiConfigRepository.stremioAddons.first()),
        cloudstreamRepos = split(apiConfigRepository.cloudstreamRepoUrls.first()),
        webSourceConfig = apiConfigRepository.webSourceConfig.first(),
        jellyfinUrl = apiConfigRepository.jellyfinUrl.first(),
        jellyfinToken = apiConfigRepository.jellyfinToken.first(),
        plexUrl = apiConfigRepository.plexUrl.first(),
        plexToken = apiConfigRepository.plexToken.first(),
        embyUrl = apiConfigRepository.embyUrl.first(),
        embyToken = apiConfigRepository.embyToken.first(),
        subsonicUrl = apiConfigRepository.subsonicUrl.first(),
        subsonicUser = apiConfigRepository.subsonicUser.first(),
        subsonicPassword = apiConfigRepository.subsonicPassword.first(),
        podcastFeeds = split(apiConfigRepository.podcastFeeds.first()),
        deezerProxyUrl = apiConfigRepository.deezerProxyUrl.first()
    )

    private fun split(value: String): List<String> = value
        .split("\n", ",", ";")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    companion object {
        private const val TAG = "HealthCheckWorker"
        private const val WORK_NAME = "health_check_periodic"
        private const val IMMEDIATE_WORK_NAME = "health_check_immediate"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(4, TimeUnit.HOURS)
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
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = OneTimeWorkRequestBuilder<HealthCheckWorker>()
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
