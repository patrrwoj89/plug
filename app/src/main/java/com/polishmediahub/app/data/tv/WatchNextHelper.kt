package com.polishmediahub.app.data.tv

import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchNextHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun addToWatchNext(mediaItem: MediaItem, resumePositionMs: Long, durationMs: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val existingId = findExistingWatchNextId(mediaItem.id)
            val intentUri = Uri.parse("tvhub://player/${mediaItem.id}")
            val posterUri = mediaItem.posterUrl?.let { Uri.parse(it) }

            val values = ContentValues().apply {
                put(TvContract.WatchNextPrograms.COLUMN_TITLE, mediaItem.title)
                put(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION, mediaItem.description)
                put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, posterUri?.toString())
                put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, intentUri.toString())
                put(TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, mediaItem.id)
                put(
                    TvContract.WatchNextPrograms.COLUMN_TYPE,
                    if (mediaItem.type == MediaItem.Type.SERIES) TvContract.WatchNextPrograms.TYPE_TV_EPISODE
                    else TvContract.WatchNextPrograms.TYPE_MOVIE
                )
                put(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, System.currentTimeMillis())
                put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, resumePositionMs)
                put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, durationMs)
            }

            if (existingId != null) {
                context.contentResolver.update(existingId, values, null, null)
            } else {
                context.contentResolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, values)
            }
        } catch (_: Exception) {
            // WatchNext requires a supported TV launcher; ignore on devices without it.
        }
    }

    private fun findExistingWatchNextId(internalId: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return try {
            context.contentResolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                arrayOf(TvContract.WatchNextPrograms._ID),
                "${TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID} = ?",
                arrayOf(internalId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms._ID))
                    TvContract.buildWatchNextProgramUri(id)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
