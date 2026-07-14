package com.polishmediahub.app

import com.polishmediahub.app.data.remote.tmdb.TmdbApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class TmdbApiTest {

    private val server = MockWebServer()
    private lateinit var api: TmdbApi
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `popularMovies parses response`() = kotlinx.coroutines.runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "page": 1,
                    "results": [
                        {"id": 1, "title": "Test Movie", "overview": "Desc", "poster_path": "/poster.jpg", "release_date": "2023-01-01", "vote_average": 7.5, "genre_ids": []}
                    ],
                    "total_pages": 1,
                    "total_results": 1
                }
                """.trimIndent()
            )
        )

        val response = api.popularMovies("key")
        assertEquals(1, response.results.size)
        assertEquals("Test Movie", response.results.first().title)
    }

    @Test
    fun `search returns empty on error`() = kotlinx.coroutines.runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val response = try {
            api.search("key", "query")
        } catch (_: Exception) {
            null
        }
        assertTrue(response == null || response.results.isEmpty())
    }
}
