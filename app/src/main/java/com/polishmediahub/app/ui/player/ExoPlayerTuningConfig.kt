@file:Suppress("UnsafeOptInUsageError")
@file:android.annotation.SuppressLint("UnsafeOptInUsageError")
package com.polishmediahub.app.ui.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator

 data class ExoPlayerTuningConfig(
    val tunneledPlaybackEnabled: Boolean = false,
    val parallelConnections: Int = 4,
    val minBufferMs: Int = 5_000,
    val maxBufferMs: Int = 50_000,
    val bufferForPlaybackMs: Int = 2_500,
    val bufferForPlaybackAfterRebufferMs: Int = 5_000,
    val backBufferMs: Int = 0,
    val initialAllocationCount: Int = 0,
    val targetBufferBytes: Int = -1
)

fun ExoPlayerTuningConfig.createLoadControl(): DefaultLoadControl {
    val allocator = DefaultAllocator(
        /* trimOnReset= */ true,
        /* individualAllocationSize= */ C.DEFAULT_BUFFER_SEGMENT_SIZE,
        /* initialAllocationCount= */ initialAllocationCount.coerceAtLeast(0)
    )
    if (targetBufferBytes > 0) {
        allocator.setTargetBufferSize(targetBufferBytes)
    }
    return DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            minBufferMs.coerceAtLeast(0),
            maxBufferMs.coerceAtLeast(minBufferMs),
            bufferForPlaybackMs.coerceAtLeast(0),
            bufferForPlaybackAfterRebufferMs.coerceAtLeast(0)
        )
        .setBackBuffer(backBufferMs.coerceAtLeast(0), true)
        .build()
}

fun createDefaultTrackSelector(context: Context, config: ExoPlayerTuningConfig): DefaultTrackSelector {
    return DefaultTrackSelector(context).apply {
        parameters = DefaultTrackSelector.Parameters.Builder()
            .setPreferredAudioLanguage("pl")
            .setPreferredTextLanguage("pl")
            .setSelectUndeterminedTextLanguage(true)
            .setTunnelingEnabled(config.tunneledPlaybackEnabled)
            .build()
    }
}


