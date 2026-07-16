package com.polishmediahub.app.data.plugin

import android.content.Context
import android.util.Log
import com.polishmediahub.app.data.legal.LegalSources
import com.polishmediahub.app.data.legal.LegalSourcesRepository
import com.polishmediahub.app.data.legal.SourceEntry
import com.polishmediahub.app.data.local.PluginDao
import com.polishmediahub.app.data.plugin.models.InstallablePlugin
import com.polishmediahub.app.data.source.CloudstreamSource
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.data.source.WebMediaSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import javax.inject.Provider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AniyomiRepoParserTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PluginRepository

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0

        server = MockWebServer()
        server.start()

        val context = mockk<Context>(relaxed = true)
        val pluginDao = mockk<PluginDao>(relaxed = true)
        every { pluginDao.observeAll() } returns flowOf(emptyList())

        val legalSources = mockk<LegalSourcesRepository>(relaxed = true)
        every { legalSources.load() } returns LegalSources(
            aniyomiRepo = SourceEntry(
                name = "test",
                url = server.url("/repo/").toString()
            )
        )

        repository = PluginRepository(
            context = context,
            pluginDao = pluginDao,
            client = client,
            dynamicPluginLoader = mockk(relaxed = true),
            kodiMediaSource = mockk(relaxed = true),
            webMediaSource = mockk(relaxed = true),
            cloudstreamSource = mockk(relaxed = true),
            quickJsMediaSourceProvider = mockk { every { get() } returns mockk(relaxed = true) },
            legalSourcesRepository = legalSources
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchAniyomiExtensions parses index and exposes extension metadata`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ANIYOMI_INDEX_JSON))

        val extensions = repository.fetchAniyomiExtensions(server.url("/repo/").toString())

        assertEquals(1, extensions.size)
        val extension = extensions.first()
        assertEquals("eu.kanade.tachiyomi.animeextension.all.animeonsen", extension.pkg)
        assertEquals("aniyomi-all.animeonsen-v14.10.apk", extension.apk)
        assertEquals("AnimeOnsen", extension.sources.first().name)
    }

    @Test
    fun `fetchAniyomiExtensions returns empty list on network timeout`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val extensions = repository.fetchAniyomiExtensions(server.url("/repo/").toString())

        assertTrue(extensions.isEmpty())
    }

    @Test
    fun `syncIndexes maps Aniyomi extensions to installable plugins with absolute APK URL and main class`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ANIYOMI_INDEX_JSON))

        repository.syncIndexes()

        val plugins = repository.availablePlugins.value.filterIsInstance<InstallablePlugin.Aniyomi>()
        assertEquals(1, plugins.size)
        val plugin = plugins.first()
        assertTrue(plugin.url.endsWith("/repo/apk/aniyomi-all.animeonsen-v14.10.apk"))
        assertEquals("eu.kanade.tachiyomi.animeextension.all.animeonsen.AnimeOnsen", plugin.mainClass)
        assertEquals("AnimeOnsen (all)", plugin.description)
        assertEquals(false, plugin.nsfw)
    }

    companion object {
        private val ANIYOMI_INDEX_JSON = """
            [
              {
                "name": "Aniyomi: AnimeOnsen",
                "pkg": "eu.kanade.tachiyomi.animeextension.all.animeonsen",
                "apk": "aniyomi-all.animeonsen-v14.10.apk",
                "lang": "all",
                "code": 10,
                "version": "14.10",
                "nsfw": 0,
                "sources": [
                  {
                    "name": "AnimeOnsen",
                    "lang": "all",
                    "id": "8542735178285060053",
                    "baseUrl": "https://www.animeonsen.xyz"
                  }
                ]
              }
            ]
        """.trimIndent()
    }
}
