package com.polishmediahub.app.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackFrameDetectorTest {

    @Test
    fun `detects intro black frames with low audio`() {
        val detector = BlackFrameDetector()
        val durationMs = 120_000L

        // Bright frames in the very beginning should not trigger intro.
        val start = detector.process(FrameSample(100L, 0.8f, -20f), 100L, durationMs)
        assertFalse(start.showSkipIntro)

        // 1500 ms of consecutive black, quiet frames inside the first 10%.
        detector.process(FrameSample(200L, 0.04f, -55f), 200L, durationMs)
        val intro = detector.process(FrameSample(1700L, 0.04f, -55f), 1700L, durationMs)
        assertTrue(intro.showSkipIntro)
        assertEquals(1700L, intro.introEndMs)
    }

    @Test
    fun `detects outro black frames with low audio`() {
        val detector = BlackFrameDetector()
        val durationMs = 120_000L

        val outroStart = 105_000L
        detector.process(FrameSample(outroStart, 0.03f, -60f), outroStart, durationMs)
        val outroEnd = outroStart + 1500L
        val outro = detector.process(FrameSample(outroEnd, 0.03f, -60f), outroEnd, durationMs)
        assertTrue(outro.showSkipOutro)
        assertEquals(outroStart, outro.outroStartMs)
    }

    @Test
    fun `black frames without audio silence do not trigger skip`() {
        val detector = BlackFrameDetector()
        val durationMs = 120_000L

        val result = detector.process(FrameSample(2000L, 0.02f, -20f), 2000L, durationMs)
        assertFalse(result.showSkipIntro)
        assertFalse(result.showSkipOutro)
    }

    @Test
    fun `reset clears any active state`() {
        val detector = BlackFrameDetector()
        val durationMs = 120_000L

        detector.process(FrameSample(200L, 0.04f, -55f), 200L, durationMs)
        detector.process(FrameSample(1700L, 0.04f, -55f), 1700L, durationMs)
        detector.reset()

        val afterReset = detector.process(FrameSample(1800L, 0.04f, -55f), 1800L, durationMs)
        assertFalse(afterReset.showSkipIntro)
    }
}
