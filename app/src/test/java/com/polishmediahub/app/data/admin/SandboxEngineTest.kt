package com.polishmediahub.app.data.admin

import com.polishmediahub.app.data.source.KodiMediaSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SandboxEngineTest {

    private lateinit var kodiMediaSource: KodiMediaSource
    private lateinit var engine: SandboxEngine

    @Before
    fun setup() {
        kodiMediaSource = mockk(relaxed = true)
        engine = SandboxEngine(kodiMediaSource, OkHttpClient())
    }

    @Test
    fun `testJson accepts a valid MediaItem JSON and returns a preview`() {
        val code = """{"id":"movie:test","title":"Test Movie","videoUrl":"https://example.com/video.mp4","type":"MOVIE","genres":["Action"]}"""

        val result = engine.testJson(code)

        assertTrue(result.success)
        assertEquals("MediaItem structure valid", result.message)
        assertNotNull(result.preview)
        assertEquals("movie:test", result.preview?.get("id")?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `testJson rejects malformed JSON`() {
        val code = "{ not valid json }"

        val result = engine.testJson(code)

        assertFalse(result.success)
        assertTrue(result.message.contains("JSON syntax error"))
        assertNull(result.preview)
    }

    @Test
    fun `testJson rejects MediaItem without required id and title`() {
        val code = """{"year":"2025","type":"MOVIE"}"""

        val result = engine.testJson(code)

        assertFalse(result.success)
        assertTrue(result.message.contains("validation failed"))
    }

    @Test
    fun `testPython returns error when Kodi URL is blank`() = runTest {
        val result = engine.testPython("print('hello')", "")

        assertFalse(result.success)
        assertEquals("Kodi URL is not configured", result.message)
    }

    @Test
    fun `testPython writes script to Kodi and runs it returning RPC output`() = runTest {
        coEvery { kodiMediaSource.writeFile(any(), any()) } returns
            """{"jsonrpc":"2.0","id":1,"result":"ok"}"""
        coEvery { kodiMediaSource.runScript(any()) } returns
            """{"jsonrpc":"2.0","id":1,"result":"done"}"""

        val result = engine.testPython("print('hello')", "http://192.168.1.10:8080")

        assertTrue(result.success)
        assertEquals("Python script executed on Kodi", result.message)
        assertTrue(result.output!!.contains("done"))
        coVerify { kodiMediaSource.writeFile("special://home/addons/plugin.video.fanfilm/test_scraper.py", "print('hello')") }
        coVerify { kodiMediaSource.runScript("special://home/addons/plugin.video.fanfilm/test_scraper.py") }
    }

    @Test
    fun `testPython returns error when Files_WriteFile reports an RPC error`() = runTest {
        coEvery { kodiMediaSource.writeFile(any(), any()) } returns
            """{"jsonrpc":"2.0","id":1,"error":{"code":-32100,"message":"Access denied"}}"""

        val result = engine.testPython("print('hello')", "http://192.168.1.10:8080")

        assertFalse(result.success)
        assertTrue(result.message.contains("Files.WriteFile error"))
    }

    @Test
    fun `testPython returns error when XBMC_RunScript does not respond`() = runTest {
        coEvery { kodiMediaSource.writeFile(any(), any()) } returns
            """{"jsonrpc":"2.0","id":1,"result":"ok"}"""
        coEvery { kodiMediaSource.runScript(any()) } returns ""

        val result = engine.testPython("print('hello')", "http://192.168.1.10:8080")

        assertFalse(result.success)
        assertTrue(result.message.contains("did not respond to XBMC.RunScript"))
    }
}
