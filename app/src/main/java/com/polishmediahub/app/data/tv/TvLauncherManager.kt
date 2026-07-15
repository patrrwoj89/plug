@file:Suppress("RestrictedApi")
package com.polishmediahub.app.data.tv

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.polishmediahub.app.R
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.source.FederatedMediaRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvLauncherManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val watchNextHelper: WatchNextHelper,
    private val federatedMediaRepository: FederatedMediaRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val previewChannelHelper = PreviewChannelHelper(context)

    private var previewChannelId: Long
        get() = prefs.getLong(KEY_PREVIEW_CHANNEL_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_PREVIEW_CHANNEL_ID, value).apply()

    init {
        scope.launch { startWatchingHistory() }
    }

    private suspend fun startWatchingHistory() {
        watchHistoryRepository.observeHistory().collectLatest { history ->
            history.forEach { (item, entity) ->
                val finished = entity.durationMs > 0 && entity.positionMs >= entity.durationMs - FINISHED_THRESHOLD_MS
                if (finished) {
                    watchNextHelper.removeFromWatchNext(item.id)
                } else if (entity.positionMs > 0) {
                    watchNextHelper.addToWatchNext(item, entity.positionMs, entity.durationMs)
                }
            }
        }
    }

    suspend fun onPlaybackStopped(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        withContext(Dispatchers.IO) {
            watchHistoryRepository.updatePosition(mediaItem.id, positionMs, durationMs)
            val finished = durationMs > 0 && positionMs >= durationMs - FINISHED_THRESHOLD_MS
            if (finished) {
                watchNextHelper.removeFromWatchNext(mediaItem.id)
            } else {
                watchNextHelper.addToWatchNext(mediaItem, positionMs, durationMs)
            }
        }
    }

    suspend fun syncRecommendations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        withContext(Dispatchers.IO) {
            try {
                ensurePreviewChannel()
                val channelId = previewChannelId
                if (channelId <= 0) return@withContext

                deleteExistingPreviewPrograms(channelId)

                val featured = federatedMediaRepository.featured().take(MAX_RECOMMENDATIONS)
                featured.forEach { item ->
                    try {
                        val program = buildPreviewProgram(item, channelId)
                        previewChannelHelper.publishPreviewProgram(program)
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
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
                            .setData(Uri.parse("polishmediahub://home"))
                    )
                    .build()
                id = previewChannelHelper.publishChannel(channel)
                previewChannelId = id
            } catch (_: Exception) {
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
            } catch (_: Exception) {
            }
        }
    }

    private fun buildPreviewProgram(item: MediaItem, channelId: Long): PreviewProgram {
        val intentUri = Uri.parse("polishmediahub://detail/${item.id}")
        val type = when (item.type) {
            MediaItem.Type.SERIES, MediaItem.Type.EPISODE -> TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES
            MediaItem.Type.CHANNEL -> TvContractCompat.PreviewProgramColumns.TYPE_CHANNEL
            else -> TvContractCompat.PreviewProgramColumns.TYPE_MOVIE
        }
        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setTitle(item.title)
            .setDescription(item.description)
            .setPosterArtUri(item.posterUrl?.let { Uri.parse(it) })
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
        private const val PREFS_NAME = "tv_launcher_manager"
        private const val KEY_PREVIEW_CHANNEL_ID = "preview_channel_id"
        private const val FINISHED_THRESHOLD_MS = 10_000L
        private const val MAX_RECOMMENDATIONS = 20
    }
}
