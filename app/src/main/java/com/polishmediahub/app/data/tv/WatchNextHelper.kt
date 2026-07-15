@file:Suppress("RestrictedApi")
package com.polishmediahub.app.data.tv

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
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

    suspend fun addToWatchNext(mediaItem: MediaItem, resumePositionMs: Long, durationMs: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        withContext(Dispatchers.IO) {
            try {
                val existingId = findExistingWatchNextId(mediaItem.id)
                val intentUri = buildDeepLink(mediaItem)
                val posterUri = mediaItem.posterUrl?.let { Uri.parse(it) }

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
                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                    .setLastPlaybackPositionMillis(resumePositionMs.toInt())
                    .setDurationMillis(durationMs.toInt())

                if (existingId != null) {
                    helper.updateWatchNextProgram(builder.build(), existingId)
                } else {
                    helper.publishWatchNextProgram(builder.build())
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun removeFromWatchNext(internalId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        withContext(Dispatchers.IO) {
            try {
                findExistingWatchNextId(internalId)?.let { id ->
                    context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(id), null, null)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun buildDeepLink(mediaItem: MediaItem): Uri {
        return Uri.parse("polishmediahub://play/${mediaItem.id}")
    }

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
        } catch (_: Exception) {
            null
        }
    }
}
