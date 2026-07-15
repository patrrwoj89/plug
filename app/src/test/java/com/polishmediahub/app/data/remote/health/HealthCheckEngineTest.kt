package com.polishmediahub.app.data.remote.health

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HealthCheckEngineTest {

    private lateinit var server: MockWebServer
    private lateinit var engine: HealthCheckEngine
    private lateinit var shortTimeoutClient: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        engine = HealthCheckEngine(Json { ignoreUnknownKeys = true })
        shortTimeoutClient = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.MILLISECONDS)
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun url(path: String = "") = server.url(path).toString()

    @Test
    fun `kodi online returns ONLINE`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"id":1,"jsonrpc":"2.0","result":{"version":{"major":20,"minor":5,"patch":0}}}""")
        )
        val result = engine.runChecks(shortTimeoutClient, HealthConfig(kodiUrl = url()))
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "kodi" }.status)
    }

    @Test
    fun `iptv online returns ONLINE`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("#EXTM3U\n"))
        val result = engine.runChecks(shortTimeoutClient, HealthConfig(iptvUrls = listOf(url())))
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "iptv_0" }.status)
    }

    @Test
    fun `unconfigured source returns UNCONFIGURED`() = runTest {
        val result = engine.runChecks(shortTimeoutClient, HealthConfig())
        val kodi = result.sources.first { it.id == "kodi" }
        assertEquals(SourceHealth.UNCONFIGURED, kodi.status)
    }

    @Test
    fun `timeout returns OFFLINE`() = runTest {
        server.enqueue(MockResponse().setBodyDelay(2, TimeUnit.SECONDS).setResponseCode(200).setBody("delayed"))
        val result = engine.runChecks(shortTimeoutClient, HealthConfig(kodiUrl = url()))
        assertEquals(SourceHealth.OFFLINE, result.sources.first { it.id == "kodi" }.status)
    }

    @Test
    fun `subsonic healthy response requires status ok`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"subsonic-response":{"status":"ok","version":"1.16.1"}}""")
        )
        val result = engine.runChecks(
            shortTimeoutClient,
            HealthConfig(subsonicUrl = url(), subsonicUser = "u", subsonicPassword = "p")
        )
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "subsonic" }.status)
    }

    @Test
    fun `subsonic failed response returns OFFLINE`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"subsonic-response":{"status":"failed"}}""")
        )
        val result = engine.runChecks(
            shortTimeoutClient,
            HealthConfig(subsonicUrl = url(), subsonicUser = "u", subsonicPassword = "p")
        )
        assertEquals(SourceHealth.OFFLINE, result.sources.first { it.id == "subsonic" }.status)
    }

    @Test
    fun `web source config parsed and checked`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val cfg = HealthConfig(
            webSourceConfig = """[{"id":"test","name":"Test Web","baseUrl":"${url()}"}]"""
        )
        val result = engine.runChecks(shortTimeoutClient, cfg)
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "web_0" }.status)
    }

    @Test
    fun `plex identity endpoint checked`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<MediaContainer />"))
        val result = engine.runChecks(shortTimeoutClient, HealthConfig(plexUrl = url()))
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "plex" }.status)
    }

    @Test
    fun `jellyfin public info checked`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ServerName":"test"}"""))
        val result = engine.runChecks(shortTimeoutClient, HealthConfig(jellyfinUrl = url()))
        assertEquals(SourceHealth.ONLINE, result.sources.first { it.id == "jellyfin" }.status)
    }
}
