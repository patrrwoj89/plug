@file:Suppress("RestrictedApi")
package com.polishmediahub.app.data.tv

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.R
import com.polishmediahub.app.data.ContentFilter
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.SavedMediaRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.data.local.WatchedEntity
import com.polishmediahub.app.data.source.FederatedMediaRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvLauncherManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val savedMediaRepository: SavedMediaRepository,
    private val watchNextHelper: WatchNextHelper,
    private val federatedMediaRepository: FederatedMediaRepository,
    private val profileRepository: ProfileRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val previewChannelHelper = PreviewChannelHelper(context)
    private val lastProgressUpdateMs = mutableMapOf<String, Long>()

    private var previewChannelId: Long
        get() = prefs.getLong(KEY_PREVIEW_CHANNEL_ID, -1L)
        set(value) = prefs.edit { putLong(KEY_PREVIEW_CHANNEL_ID, value) }

    fun start() {
        scope.launch { observeProfileChanges() }
        scope.launch { startWatchingHistory() }
    }

    private suspend fun observeProfileChanges() {
        profileRepository.currentProfile.collectLatest { profile ->
            if (profile == null) return@collectLatest
            withContext(Dispatchers.IO) {
                syncProfileData(profile)
            }
        }
    }

    private suspend fun syncProfileData(profile: ProfileEntity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            watchNextHelper.clearAll()
            syncWatchNextForProfile(profile)
            syncWatchlistForProfile(profile)
            syncRecommendationsForProfile(profile)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "syncProfileData failed: ${e.message}", e)
        }
    }

    internal suspend fun syncWatchNextForProfile(profile: ProfileEntity) {
        val history = watchHistoryRepository.observeHistory().first()
        val entries = buildWatchNextItems(profile, history)
        entries.forEach { entry ->
            watchNextHelper.addToWatchNext(entry.item, entry.positionMs, entry.durationMs, entry.kind)
        }
    }

    internal suspend fun syncWatchlistForProfile(profile: ProfileEntity) {
        val watchlist = savedMediaRepository.observeWatchlist().first()
        val items = buildWatchlistItems(profile, watchlist)
        items.forEach { item ->
            watchNextHelper.addToWatchNext(item, 0, 0, WatchNextKind.WATCHLIST)
        }
    }

    private suspend fun syncRecommendationsForProfile(profile: ProfileEntity) {
        ensurePreviewChannel()
        val channelId = previewChannelId
        if (channelId <= 0) return

        deleteExistingPreviewPrograms(channelId)

        try {
            val featured = federatedMediaRepository.featured()
            val items = buildPreviewItems(profile, featured)
            items.forEach { item ->
                try {
                    val program = buildPreviewProgram(item, channelId)
                    previewChannelHelper.publishPreviewProgram(program)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "publishPreviewProgram failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "syncRecommendationsForProfile failed: ${e.message}", e)
        }
    }

    /**
     * Throttled progress update suitable for periodic save callbacks while the video is playing.
     * The system [ContentResolver] is only touched every [MIN_PROGRESS_UPDATE_INTERVAL_MS] to avoid
     * freezing the Android TV launcher with IPC traffic.
     */
    fun updatePlaybackProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        scope.launch { syncWatchNext(mediaItem, positionMs, durationMs, force = false) }
    }

    /**
     * Force-synchronizes Watch Next when the user pauses or leaves the player. This must always
     * be written to the launcher so the resume position is accurate.
     */
    suspend fun onPlaybackStopped(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        withContext(Dispatchers.IO) {
            syncWatchNext(mediaItem, positionMs, durationMs, force = true)
        }
    }

    private suspend fun startWatchingHistory() {
        watchHistoryRepository.observeHistory().collectLatest { history ->
            val profile = profileRepository.currentProfile.value
            history.forEach { (item, entity) ->
                if (!ContentFilter.isAllowed(item, profile)) return@forEach
                syncWatchNextToLauncher(item, entity.positionMs, entity.durationMs, force = false, kind = WatchNextKind.CONTINUE)
            }
        }
    }

    private suspend fun syncWatchNext(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        force: Boolean
    ) {
        watchHistoryRepository.updatePosition(mediaItem, positionMs, durationMs)
        syncWatchNextToLauncher(mediaItem, positionMs, durationMs, force, kind = WatchNextKind.CONTINUE)
    }

    private suspend fun syncWatchNextToLauncher(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        force: Boolean,
        kind: WatchNextKind
    ) {
        if (!force) {
            val now = System.currentTimeMillis()
            val last = lastProgressUpdateMs[mediaItem.id] ?: 0L
            if (now - last < MIN_PROGRESS_UPDATE_INTERVAL_MS) return
            lastProgressUpdateMs[mediaItem.id] = now
        } else {
            lastProgressUpdateMs[mediaItem.id] = System.currentTimeMillis()
        }

        if (!ContentFilter.isAllowed(mediaItem, profileRepository.currentProfile.value)) {
            watchNextHelper.removeFromWatchNext(mediaItem.id)
            return
        }

        val finished = durationMs > 0 && positionMs >= durationMs - FINISHED_THRESHOLD_MS
        if (finished) {
            watchNextHelper.removeFromWatchNext(mediaItem.id)
        } else {
            watchNextHelper.addToWatchNext(mediaItem, positionMs, durationMs, kind)
        }
    }

    /**
     * Public entry used by [RecommendationsWorker] to refresh the Preview Channel
     * based on the currently active profile.
     */
    suspend fun syncRecommendations() {
        val profile = profileRepository.currentProfile.first() ?: return
        syncRecommendationsForProfile(profile)
    }

    private suspend fun ensurePreviewChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        withContext(Dispatchers.IO) {
            try {
                var id = previewChannelId
                if (id > 0 && previewChannelHelper.getPreviewChannel(id) != null) {
                    return@withContext
                }
                val channel = PreviewChannel.Builder()
                    .setDisplayName(context.getString(R.string.app_name))
                    .setDescription(context.getString(R.string.recommendations_channel_description))
                    .setAppLinkIntent(
                        android.content.Intent(context, com.polishmediahub.app.MainActivity::class.java)
                            .setAction(android.content.Intent.ACTION_VIEW)
                            .setData("polishmediahub://home".toUri())
                    )
                    .build()
                id = previewChannelHelper.publishChannel(channel)
                previewChannelId = id
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "ensurePreviewChannel failed: ${e.message}", e)
            }
        }
    }

    private suspend fun deleteExistingPreviewPrograms(channelId: Long) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.query(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    arrayOf(TvContractCompat.PreviewPrograms._ID),
                    "${TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID} = ?",
                    arrayOf(channelId.toString()),
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms._ID)
                    while (cursor.moveToNext()) {
                        val programId = cursor.getLong(idColumn)
                        previewChannelHelper.deletePreviewProgram(programId)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "deleteExistingPreviewPrograms failed: ${e.message}", e)
            }
        }
    }

    private fun buildPreviewProgram(item: MediaItem, channelId: Long): PreviewProgram {
        val intentUri = "polishmediahub://detail/${item.id}".toUri()
        val type = when (item.type) {
            MediaItem.Type.SERIES, MediaItem.Type.EPISODE -> TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES
            MediaItem.Type.CHANNEL -> TvContractCompat.PreviewProgramColumns.TYPE_CHANNEL
            else -> TvContractCompat.PreviewProgramColumns.TYPE_MOVIE
        }
        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setTitle(item.title)
            .setDescription(item.description)
            .setPosterArtUri(item.posterUrl?.let { it.toUri() })
            .setIntentUri(intentUri)
            .setInternalProviderId(item.id)
            .setType(type)
            .setDurationMillis(parseDurationMillis(item.duration).toInt())

        item.year.takeIf { it.isNotBlank() }?.let { builder.setReleaseDate(it) }
        return builder.build()
    }

    private fun parseDurationMillis(duration: String): Long {
        val parts = duration.split(":")
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val seconds = parts[2].toLongOrNull() ?: 0L
                ((hours * 3600) + (minutes * 60) + seconds) * 1000
            }
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val seconds = parts[1].toLongOrNull() ?: 0L
                ((minutes * 60) + seconds) * 1000
            }
            else -> duration.toLongOrNull()?.times(1000) ?: 0L
        }
    }

    companion object {
        private const val TAG = "TvLauncherManager"
        private const val PREFS_NAME = "tv_launcher_manager"
        private const val KEY_PREVIEW_CHANNEL_ID = "preview_channel_id"
        private const val MIN_PROGRESS_UPDATE_INTERVAL_MS = 15_000L
    }
}

private const val FINISHED_THRESHOLD_MS = 10_000L
private const val MAX_RECOMMENDATIONS = 20

internal data class WatchNextEntry(
    val item: MediaItem,
    val positionMs: Long,
    val durationMs: Long,
    val kind: WatchNextKind
)

internal fun isUnfinished(positionMs: Long, durationMs: Long): Boolean {
    if (positionMs <= 0) return false
    if (durationMs <= 0) return true
    return positionMs < durationMs - FINISHED_THRESHOLD_MS
}

internal fun buildWatchNextItems(
    profile: ProfileEntity?,
    history: List<Pair<MediaItem, WatchedEntity>>
): List<WatchNextEntry> = history.mapNotNull { (item, entity) ->
    if (!ContentFilter.isAllowed(item, profile)) return@mapNotNull null
    if (!isUnfinished(entity.positionMs, entity.durationMs)) return@mapNotNull null
    WatchNextEntry(item, entity.positionMs, entity.durationMs, WatchNextKind.CONTINUE)
}

internal fun buildWatchlistItems(
    profile: ProfileEntity?,
    watchlist: List<MediaItem>
): List<MediaItem> = watchlist.filter { ContentFilter.isAllowed(it, profile) }

private val RECOMMENDATION_TYPES = setOf(
    MediaItem.Type.MOVIE,
    MediaItem.Type.SERIES,
    MediaItem.Type.EPISODE
)

internal fun buildPreviewItems(
    profile: ProfileEntity?,
    featured: List<MediaItem>
): List<MediaItem> {
    val allowed = ContentFilter.filter(featured, profile)
    return allowed.filter { it.type in RECOMMENDATION_TYPES }.take(MAX_RECOMMENDATIONS)
}
