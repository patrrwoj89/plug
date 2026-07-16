package com.polishmediahub.app.ui.player

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.annotation.SuppressLint
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.media3.ui.PlayerView
import com.polishmediahub.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Captures a small thumbnail of the current video frame to estimate the average luma.
 * This is used by [BlackFrameDetector] to identify black intro/outro sequences.
 *
 * A [PlayerView] may render to either a [SurfaceView] or a [TextureView]. [SurfaceView] paths use
 * [PixelCopy] (API 24+), while [TextureView] uses its built-in [TextureView.bitmap] getter.
 */
@SuppressLint("UnsafeOptInUsageError")
class FrameSampler {

    suspend fun sample(playerView: PlayerView?): Float = when (val view = playerView?.videoSurfaceView) {
        null -> BRIGHT_LUMA
        is TextureView -> sampleTextureView(view)
        is SurfaceView -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sampleSurfaceView(view)
        } else {
            BRIGHT_LUMA
        }
        else -> BRIGHT_LUMA
    }

    private fun sampleTextureView(view: TextureView): Float {
        val bitmap = try {
            view.bitmap
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "TextureView bitmap failed", e)
            null
        }
        return bitmap?.let { averageLuma(it) } ?: BRIGHT_LUMA
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun sampleSurfaceView(view: SurfaceView): Float {
        val bitmap = createBitmap(CAPTURE_WIDTH, CAPTURE_HEIGHT)
        return try {
            suspendCancellableCoroutine { cont ->
                PixelCopy.request(
                    view,
                    bitmap,
                    { result ->
                        when (result) {
                            PixelCopy.SUCCESS -> cont.resume(averageLuma(bitmap))
                            else -> {
                                if (!bitmap.isRecycled) bitmap.recycle()
                                cont.resume(BRIGHT_LUMA)
                            }
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
        } catch (e: Exception) {
            if (!bitmap.isRecycled) bitmap.recycle()
            if (BuildConfig.DEBUG) Log.w(TAG, "PixelCopy failed", e)
            BRIGHT_LUMA
        }
    }

    private fun averageLuma(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var sum = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sum += (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        }
        val average = sum / pixels.size
        bitmap.recycle()
        return average.toFloat().coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "FrameSampler"
        private const val CAPTURE_WIDTH = 16
        private const val CAPTURE_HEIGHT = 16
        internal const val BRIGHT_LUMA = 1f
    }
}
