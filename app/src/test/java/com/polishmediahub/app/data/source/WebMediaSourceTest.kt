package com.polishmediahub.app.data.source

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.plugin.QuickJsEngine
import com.polishmediahub.app.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.inject.Provider

class WebMediaSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: WebMediaSource
    private lateinit var apiConfig: ApiConfigRepository
    private lateinit var quickJsEngine: QuickJsEngine

    private val client = OkHttpClient()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        apiConfig = mockk(relaxed = true)
        quickJsEngine = mockk(relaxed = true)
        source = WebMediaSource(client, Provider { quickJsEngine }, MemoryCookieJar(), apiConfig)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun configure(
        bypass: Boolean = false,
        workerUrl: String = "",
        token: String = ""
    ) {
        every { apiConfig.useCloudflareBypass } returns flowOf(bypass)
        every { apiConfig.cloudflareWorkerUrl } returns flowOf(workerUrl)
        every { apiConfig.cloudflareAuthToken } returns flowOf(token)
    }

    private fun sourceConfig(): String {
        val base = server.url("/").toString().removeSuffix("/")
        return """[{"id":"test","name":"Test","baseUrl":"$base/","itemUrlTemplate":"$base/stream-page"}]"""
    }

    private fun mediaItem(): MediaItem = MediaItem(
        id = "web:test:page",
        title = "Test",
        videoUrl = server.url("/stream-page").toString(),
        type = MediaItem.Type.MOVIE
    )

    @Test
    fun `resolveItem uses Cloudflare worker and merges returned headers`() = runTest {
        source.configure(sourceConfig())
        configure(bypass = true, workerUrl = server.url("/").toString(), token = "hub-secret")

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"streamUrl":"https://cdn.example.com/video.mp4","headers":{"Referer":"https://example.com/"}}""")
        )

        val resolved = source.resolveItem(mediaItem())

        assertEquals("https://cdn.example.com/video.mp4", resolved.videoUrl)
        assertEquals("https://example.com/", resolved.headers["Referer"])

        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertTrue(request?.path?.startsWith("/resolve?url=") == true)
        assertEquals("hub-secret", request?.getHeader("X-Hub-Token"))
    }

    @Test
    fun `resolveItem falls back to local extraction when Cloudflare worker fails`() = runTest {
        source.configure(sourceConfig())
        configure(bypass = true, workerUrl = server.url("/").toString(), token = "hub-secret")

        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "<html><body><video src=\"https://cdn.example.com/local.mp4\"></video></body></html>"
            )
        )

        val resolved = source.resolveItem(mediaItem())

        assertEquals("https://cdn.example.com/local.mp4", resolved.videoUrl)
    }

    @Test
    fun `resolveItem uses local extraction when Cloudflare bypass is disabled`() = runTest {
        source.configure(sourceConfig())
        configure(bypass = false)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "<html><body><video src=\"https://cdn.example.com/local.mp4\"></video></body></html>"
            )
        )

        val resolved = source.resolveItem(mediaItem())

        assertEquals("https://cdn.example.com/local.mp4", resolved.videoUrl)
        assertTrue(server.requestCount == 1)
    }

    @Test
    fun `resolve returns original videoUrl when no stream can be found`() = runTest {
        source.configure(sourceConfig())
        configure(bypass = false)

        server.enqueue(MockResponse().setResponseCode(200).setBody("<html><body>No video here</body></html>"))

        val url = source.resolve(mediaItem())

        assertEquals(mediaItem().videoUrl, url)
    }

    @Test
    fun `resolveViaCloudflare returns null for forbidden response`() = runTest {
        source.configure(sourceConfig())
        configure(bypass = true, workerUrl = server.url("/").toString(), token = "wrong-token")

        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "<html><body><video src=\"https://cdn.example.com/fallback.mp4\"></video></body></html>"
            )
        )

        val resolved = source.resolveItem(mediaItem())

        assertEquals("https://cdn.example.com/fallback.mp4", resolved.videoUrl)
        assertNull(resolved.headers["Referer"])
    }
}
