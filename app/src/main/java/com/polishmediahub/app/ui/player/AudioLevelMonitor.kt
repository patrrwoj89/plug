package com.polishmediahub.app.ui.player

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.polishmediahub.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Lightweight wrapper around [Visualizer] that exposes a stream of audio loudness values in dB FS.
 * This component follows Zasada 5: the native [Visualizer] is released in [release] and no audio
 * session IDs are logged in release builds.
 */
class AudioLevelMonitor(
    context: Context,
    audioSessionId: Int
) {
    private val _levelDb = MutableStateFlow(0f)
    val levelDb: StateFlow<Float> = _levelDb

    private var visualizer: Visualizer? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        if (audioSessionId != 0) {
        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val size = if (captureSizeRange.size >= 2) captureSizeRange[1] else 1024
            val viz = Visualizer(audioSessionId).apply {
                captureSize = size
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform == null) return
                            _levelDb.value = computeDb(waveform)
                        }

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // FFT not needed for loudness detection.
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,
                    false
                )
                enabled = true
            }
            visualizer = viz
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Visualizer init failed for session $audioSessionId", e)
        }
        }
    }

    fun release() {
        try {
            visualizer?.setDataCaptureListener(null, 0, false, false)
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Visualizer release failed", e)
        }
        visualizer = null
    }

    private fun computeDb(waveform: ByteArray): Float {
        var sum = 0L
        for (b in waveform) {
            val amplitude = (b.toInt() and 0xFF) - 128
            sum += amplitude * amplitude.toLong()
        }
        val rms = sqrt(sum / waveform.size.toDouble()).toFloat()
        return if (rms < 1f) -100f else (20f * log10(rms / 128f))
    }

    companion object {
        private const val TAG = "AudioLevelMonitor"
    }
}
