package com.polishmediahub.app.data.remote.anime

import android.util.Log
import com.polishmediahub.app.model.MediaItem
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DocchiMediaSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: DocchiMediaSource

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0

        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val serverUrl = server.url("/")
                val newUrl = original.url.newBuilder()
                    .scheme(serverUrl.scheme)
                    .host(serverUrl.host)
                    .port(serverUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        source = DocchiMediaSource(client, Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search maps series list to MediaItem with Polish metadata`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[
                    {
                      "mal_id": 51535,
                      "title": "Attack on Titan: The Final Season Part 3",
                      "title_en": "Shingeki no Kyojin",
                      "slug": "shingeki-no-kyojin-the-final-season-kanketsu-hen",
                      "cover": "https://cdn.myanimelist.net/images/anime/1279/131078l.jpg",
                      "adult_content": "false",
                      "series_type": "Special",
                      "episodes": 2,
                      "season_year": 2023,
                      "genres": ["Action", "Drama"],
                      "aired_from": "2023-10-01T00:00:00+00:00"
                    }
                ]""".trimIndent()
            )
        )

        val results = source.search("Attack on Titan")

        assertEquals(1, results.size)
        val item = results.first()
        assertEquals("docchi:shingeki-no-kyojin-the-final-season-kanketsu-hen", item.id)
        assertEquals("Attack on Titan: The Final Season Part 3", item.title)
        assertEquals("2023", item.year)
        assertTrue(item.posterUrl?.startsWith("https://cdn.myanimelist.net") == true)
        assertTrue(item.genres.contains("Action"))
        assertEquals(MediaItem.Type.SERIES, item.type)
    }

    @Test
    fun `byId returns null on network timeout`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val result = source.byId("docchi:missing-slug")

        assertNull(result)
    }

    @Test
    fun `resolve returns first episode player URL`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[
                    { "id": 1, "anime_episode_number": 1, "player": "https://ebd.cda.pl/123456", "player_hosting": "Cda" },
                    { "id": 2, "anime_episode_number": 1, "player": "https://example.com/video.mp4", "player_hosting": "Direct" }
                ]""".trimIndent()
            )
        )

        val item = MediaItem(
            id = "docchi:shingeki",
            title = "Test",
            episode = 1,
            type = MediaItem.Type.SERIES
        )
        val url = source.resolve(item)

        assertEquals("https://ebd.cda.pl/123456", url)
    }

    @Test
    fun `resolveItem sets player URL and referer headers`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[
                    { "id": 1, "anime_episode_number": 1, "player": "https://ebd.cda.pl/123456", "player_hosting": "Cda" }
                ]""".trimIndent()
            )
        )

        val item = MediaItem(
            id = "docchi:shingeki",
            title = "Test",
            episode = 1,
            type = MediaItem.Type.SERIES
        )
        val resolved = source.resolveItem(item)

        assertEquals("https://ebd.cda.pl/123456", resolved.videoUrl)
        assertEquals("https://docchi.pl/", resolved.headers["Referer"])
        assertTrue(resolved.headers.containsKey("User-Agent"))
    }
}
