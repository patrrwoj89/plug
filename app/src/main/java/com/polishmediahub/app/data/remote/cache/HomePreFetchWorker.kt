package com.polishmediahub.app.data.remote.cache

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
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class HomePreFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val imageLoader: ImageLoader
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val featured = runCatching { mediaRepository.featured() }.getOrDefault(emptyList())
            val categories = runCatching { mediaRepository.categories() }.getOrDefault(emptyList())
            val categoryItems = categories.flatMap { it.items }

            val allItems = (featured + categoryItems).distinctBy { it.id }
            val imageUrls = allItems
                .flatMap { listOfNotNull(it.posterUrl, it.backdropUrl) }
                .distinct()
                .filter { it.isNotBlank() }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Prefetching ${imageUrls.size} images for ${allItems.size} home items")
            }

            imageUrls.forEach { url ->
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.execute(request)
            }

            Result.success()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Home prefetch failed: ${e.message}", e)
            }
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HomePreFetchWorker"
        private const val WORK_NAME = "home_prefetch_periodic"
        private const val IMMEDIATE_WORK_NAME = "home_prefetch_immediate"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<HomePreFetchWorker>(12, TimeUnit.HOURS)
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
            val request = OneTimeWorkRequestBuilder<HomePreFetchWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
