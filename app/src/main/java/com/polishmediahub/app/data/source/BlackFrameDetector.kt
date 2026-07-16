package com.polishmediahub.app.data.source

/**
 * Sample captured from the video surface and audio output.
 *
 * @param timestampMs wall-clock timestamp of the sample (ms)
 * @param averageLuma normalized 0..1 average luma of the video frame (1 = bright, 0 = black)
 * @param audioDb approximate loudness of the audio track in dB FS (0 dB = full scale)
 */
data class FrameSample(
    val timestampMs: Long,
    val averageLuma: Float,
    val audioDb: Float
)

/**
 * Pure, testable state machine that detects intro/outro segments from consecutive black and quiet
 * frames. It does not touch Android view or player APIs directly, so it can be unit tested with
 * hand-crafted [FrameSample] sequences.
 */
class BlackFrameDetector(
    private val minBlackDurationMs: Long = 1500L,
    private val blackLumaThreshold: Float = 0.05f,
    private val quietDbThreshold: Float = -40f,
    private val introWindowFraction: Float = 0.10f,
    private val outroWindowFraction: Float = 0.15f
) {

    data class State(
        val showSkipIntro: Boolean = false,
        val introEndMs: Long = 0L,
        val showSkipOutro: Boolean = false,
        val outroStartMs: Long = 0L,
        val outroEndMs: Long = 0L
    )

    private var blackSegmentStartMs: Long? = null

    fun process(sample: FrameSample, positionMs: Long, durationMs: Long): State {
        if (durationMs <= 0) return State()

        val introWindowEnd = (durationMs * introWindowFraction).toLong()
        val outroWindowStart = (durationMs * (1f - outroWindowFraction)).toLong()

        val inIntro = positionMs in 0L until introWindowEnd
        val inOutro = positionMs >= outroWindowStart && positionMs < durationMs

        val isBlackAndQuiet = sample.averageLuma < blackLumaThreshold && sample.audioDb < quietDbThreshold

        if ((inIntro || inOutro) && isBlackAndQuiet) {
            val start = blackSegmentStartMs ?: positionMs
            blackSegmentStartMs = start
            val elapsed = positionMs - start
            if (elapsed >= minBlackDurationMs) {
                return when {
                    inIntro -> State(showSkipIntro = true, introEndMs = positionMs)
                    else -> State(
                        showSkipOutro = true,
                        outroStartMs = start,
                        outroEndMs = durationMs
                    )
                }
            }
            return State()
        }

        blackSegmentStartMs = null
        return State()
    }

    fun reset() {
        blackSegmentStartMs = null
    }
}
