@file:Suppress("RestrictedApi")
package com.polishmediahub.app.data.tv

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchNextHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val helper = PreviewChannelHelper(context)

    internal suspend fun addToWatchNext(
        mediaItem: MediaItem,
        resumePositionMs: Long,
        durationMs: Long,
        kind: WatchNextKind = WatchNextKind.CONTINUE
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        withContext(Dispatchers.IO) {
            try {
                val existingId = findExistingWatchNextId(mediaItem.id)
                val intentUri = buildDeepLink(mediaItem)
                val posterUri = mediaItem.posterUrl?.let { it.toUri() }

                val builder = WatchNextProgram.Builder()
                    .setTitle(mediaItem.title)
                    .setDescription(mediaItem.description)
                    .setPosterArtUri(posterUri)
                    .setIntentUri(intentUri)
                    .setInternalProviderId(mediaItem.id)
                    .setType(
                        if (mediaItem.type == MediaItem.Type.SERIES) {
                            TvContractCompat.PreviewProgramColumns.TYPE_TV_EPISODE
                        } else {
                            TvContractCompat.PreviewProgramColumns.TYPE_MOVIE
                        }
                    )
                    .setWatchNextType(kind.value)
                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                    .setLastPlaybackPositionMillis(resumePositionMs.toInt())
                    .setDurationMillis(durationMs.toInt())

                if (existingId != null) {
                    helper.updateWatchNextProgram(builder.build(), existingId)
                } else {
                    helper.publishWatchNextProgram(builder.build())
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "publishWatchNext failed: ${e.message}", e)
            }
        }
    }

    private val TAG = "WatchNextHelper"

    suspend fun removeFromWatchNext(internalId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        withContext(Dispatchers.IO) {
            try {
                findExistingWatchNextId(internalId)?.let { id ->
                    context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(id), null, null)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "removeFromWatchNext failed: ${e.message}", e)
            }
        }
    }

    suspend fun clearAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.delete(TvContractCompat.WatchNextPrograms.CONTENT_URI, null, null)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "clearAll failed: ${e.message}", e)
            }
        }
    }

    private fun buildDeepLink(mediaItem: MediaItem) = "polishmediahub://play/${mediaItem.id}".toUri()

    private fun findExistingWatchNextId(internalId: String): Long? {
        return try {
            context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                arrayOf(TvContractCompat.WatchNextPrograms._ID),
                "${TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID} = ?",
                arrayOf(internalId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(TvContractCompat.WatchNextPrograms._ID))
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "findExistingWatchNextId failed: ${e.message}", e)
            null
        }
    }
}
