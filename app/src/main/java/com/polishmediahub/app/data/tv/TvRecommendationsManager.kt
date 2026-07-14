package com.polishmediahub.app.data.tv

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import android.media.tv.TvContract
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvRecommendationsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver = context.contentResolver

    @RequiresApi(Build.VERSION_CODES.O)
    fun addWatchNext(mediaItem: MediaItem, resumeMillis: Long = 0L, durationMillis: Long = 0L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            removeExistingWatchNext(mediaItem.id)
            val values = ContentValues().apply {
                put(TvContract.WatchNextPrograms.COLUMN_TITLE, mediaItem.title)
                put(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION, mediaItem.description)
                put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, mediaItem.posterUrl)
                put(TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, mediaItem.id)
                put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, resumeMillis)
                put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, durationMillis)
                put(TvContract.WatchNextPrograms.COLUMN_TYPE, TvContract.WatchNextPrograms.TYPE_CLIP)
                put(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, System.currentTimeMillis())
                put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, buildDeepLinkUri(mediaItem).toString())
            }
            resolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, values)
        } catch (_: Exception) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun removeExistingWatchNext(internalId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val cursor: Cursor? = resolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                arrayOf(TvContract.WatchNextPrograms._ID),
                "${TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID} = ?",
                arrayOf(internalId),
                null
            )
            cursor?.use {
                val idColumn = it.getColumnIndex(TvContract.WatchNextPrograms._ID)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    resolver.delete(
                        TvContract.buildWatchNextProgramUri(id),
                        null,
                        null
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    fun buildDeepLinkUri(mediaItem: MediaItem): Uri {
        return Uri.parse("polishmediahub://play/${mediaItem.id}")
    }
}
