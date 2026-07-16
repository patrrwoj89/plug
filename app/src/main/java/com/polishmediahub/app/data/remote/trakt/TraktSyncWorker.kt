package com.polishmediahub.app.data.remote.trakt

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
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
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.SavedMediaRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.remote.cache.HomePreFetchWorker
import com.polishmediahub.app.data.local.SavedMediaEntity
import com.polishmediahub.app.data.local.WatchedEntity
import com.polishmediahub.app.model.MediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class TraktSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiConfigRepository: ApiConfigRepository,
    private val profileRepository: ProfileRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val savedMediaRepository: SavedMediaRepository,
    private val traktMediaRepository: TraktMediaRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var errorMessage: String? = null
        try {
            val profile = profileRepository.currentProfile.first()
            if (profile == null) {
                apiConfigRepository.setLastTraktSync(System.currentTimeMillis(), "error", "No active profile")
                return@withContext Result.failure()
            }

            // Pull remote lists. TraktMediaRepository handles auth and returns items together
            // with the timestamp Trakt reports for the last watch / list addition.
            val remoteWatched = traktMediaRepository.watchedItemsWithTimestamps()
            val remoteWatchlist = traktMediaRepository.watchlistWithTimestamps()

            // Pull local Room data for the active profile.
            val localHistory = watchHistoryRepository.getAllForCurrentProfile()
            val localWatchlist = savedMediaRepository.getWatchlist()

            // --- Watched history two-way merge ---
            val remoteWatchedMap = remoteWatched.associateBy { it.first.id }
            val localHistoryMap = localHistory.associateBy { it.id }

            // Items Trakt knows about but local is missing or older -> pull down.
            val toDownloadWatched = remoteWatched.filter { (item, ts) ->
                val local = localHistoryMap[item.id]
                local == null || ts > local.watchedAt
            }
            // Items local knows about but Trakt is missing or older -> push up.
            val toUploadWatched = localHistory.filter { local ->
                val remote = remoteWatchedMap[local.id]
                remote == null || local.watchedAt > remote.second
            }

            toDownloadWatched.forEach { (item, ts) ->
                watchHistoryRepository.addOrUpdateHistory(item, positionMs = 0, durationMs = 0, watchedAt = ts)
            }

            val watchedUploadItems = toUploadWatched.mapNotNull { it.toMediaItem() }
            if (watchedUploadItems.isNotEmpty()) {
                traktMediaRepository.syncWatchedToTrakt(watchedUploadItems.zip(toUploadWatched.map { it.watchedAt }))
            }

            // --- Watchlist two-way merge ---
            val remoteWatchlistMap = remoteWatchlist.associateBy { it.first.id }
            val localWatchlistMap = localWatchlist.associateBy { it.id }

            // Match by cross IDs (Trakt id preferred, then TMDB, then IMDb).
            fun matchLocal(mediaItem: MediaItem): SavedMediaEntity? {
                return localWatchlist.find { local ->
                    local.id == mediaItem.id ||
                        (mediaItem.traktId != null && local.traktId == mediaItem.traktId) ||
                        (mediaItem.tmdbId != null && local.tmdbId == mediaItem.tmdbId) ||
                        (!mediaItem.imdbId.isNullOrBlank() && local.imdbId == mediaItem.imdbId)
                }
            }

            fun matchRemote(local: SavedMediaEntity): Pair<MediaItem, Long>? {
                return remoteWatchlist.find { (mediaItem, _) ->
                    mediaItem.id == local.id ||
                        (local.traktId != null && mediaItem.traktId == local.traktId) ||
                        (local.tmdbId != null && mediaItem.tmdbId == local.tmdbId) ||
                        (!local.imdbId.isNullOrBlank() && mediaItem.imdbId == local.imdbId)
                }
            }

            // Pull remote watchlist items that are not stored locally.
            val toDownloadWatchlist = remoteWatchlist.filter { (item, ts) ->
                val local = matchLocal(item)
                local == null || ts > (local.addedAt)
            }
            toDownloadWatchlist.forEach { (item, ts) ->
                val existing = matchLocal(item)
                if (existing == null || ts > existing.addedAt) {
                    savedMediaRepository.addToWatchlist(item, ts)
                }
            }

            // Push local watchlist items that Trakt doesn't have yet.
            val toUploadWatchlist = localWatchlist.filter { local ->
                val remote = matchRemote(local)
                remote == null || (local.addedAt > remote.second)
            }.map { it.toModel() }
            if (toUploadWatchlist.isNotEmpty()) {
                traktMediaRepository.syncWatchlistToTrakt(toUploadWatchlist)
            }

            apiConfigRepository.setLastTraktSync(System.currentTimeMillis(), "success")
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Trakt sync success. Watched: ${toDownloadWatched.size} down, ${watchedUploadItems.size} up. Watchlist: ${toDownloadWatchlist.size} down, ${toUploadWatchlist.size} up.")
            }
            HomePreFetchWorker.startImmediate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Trakt sync failed: ${e.message}", e)
            errorMessage = e.message
            apiConfigRepository.setLastTraktSync(System.currentTimeMillis(), "error", errorMessage)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "TraktSyncWorker"
        private const val WORK_NAME = "trakt_sync_periodic"
        private const val IMMEDIATE_WORK_NAME = "trakt_sync_immediate"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<TraktSyncWorker>(6, TimeUnit.HOURS)
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
            val request = OneTimeWorkRequestBuilder<TraktSyncWorker>()
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

private fun WatchedEntity.toMediaItem(): MediaItem? {
    if (title.isBlank()) return null
    return MediaItem(
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        posterUrl = posterUrl.takeIf { it.isNotBlank() },
        backdropUrl = backdropUrl.takeIf { it.isNotBlank() },
        year = year,
        type = runCatching { MediaItem.Type.valueOf(type) }.getOrDefault(MediaItem.Type.MOVIE),
        season = season,
        episode = episode,
        tmdbId = tmdbId,
        traktId = traktId,
        imdbId = imdbId
    )
}

private fun SavedMediaEntity.toModel(): MediaItem = MediaItem(
    id = id,
    title = title,
    subtitle = subtitle,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    year = year,
    duration = duration,
    rating = rating,
    videoUrl = videoUrl.takeIf { it.isNotBlank() },
    tmdbId = tmdbId,
    traktId = traktId,
    imdbId = imdbId,
    season = season,
    episode = episode
)
