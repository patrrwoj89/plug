package com.polishmediahub.app.data.remote.cloud

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import com.polishmediahub.app.data.ApiConfigRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CloudProfileSyncWorkerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `doWork zips media db files and uploads them to the cloud worker`() = runTest(testDispatcher) {
        val tempDir = Files.createTempDirectory("cloudsync").toFile()
        val dbFile = File(tempDir, "media.db").apply {
            writeText("fake db")
            File(absolutePath + "-shm").writeText("shm")
            File(absolutePath + "-wal").writeText("wal")
        }

        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getDatabasePath("media.db") } returns dbFile
        every { context.cacheDir } returns tempDir

        val params = mockk<WorkerParameters>(relaxed = true)

        val capturedBytes = mutableListOf<ByteArray>()
        val client = mockk<CloudProfileSyncClient>(relaxed = true)
        coEvery { client.uploadProfileBackup(any(), any(), any()) } coAnswers {
            capturedBytes.add(firstArg())
            Result.success("ok")
        }

        val repo = mockk<ApiConfigRepository>(relaxed = true)
        every { repo.cloudflareWorkerUrl } returns flowOf("https://worker.example")
        every { repo.cloudflareAuthToken } returns flowOf("hub-token")

        val worker = CloudProfileSyncWorker(context, params, repo, client)
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertEquals(1, capturedBytes.size)

        val zipBytes = capturedBytes.first()
        val entries = mutableListOf<String>()
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zis.nextEntry
            }
        }
        assertTrue(entries.contains("media.db"))
        assertTrue(entries.contains("media.db-shm"))
        assertTrue(entries.contains("media.db-wal"))

        coVerify { client.uploadProfileBackup(any(), eq("https://worker.example"), eq("hub-token")) }
        coVerify { repo.setLastProfileSync(any(), eq("success"), any()) }
    }

    @Test
    fun `doWork returns retry when upload fails`() = runTest(testDispatcher) {
        val tempDir = Files.createTempDirectory("cloudsync").toFile()
        val dbFile = File(tempDir, "media.db").apply {
            writeText("fake db")
            File(absolutePath + "-shm").writeText("shm")
            File(absolutePath + "-wal").writeText("wal")
        }

        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getDatabasePath("media.db") } returns dbFile
        every { context.cacheDir } returns tempDir

        val params = mockk<WorkerParameters>(relaxed = true)

        val client = mockk<CloudProfileSyncClient>(relaxed = true)
        coEvery { client.uploadProfileBackup(any(), any(), any()) } returns Result.failure(RuntimeException("network"))

        val repo = mockk<ApiConfigRepository>(relaxed = true)
        every { repo.cloudflareWorkerUrl } returns flowOf("https://worker.example")
        every { repo.cloudflareAuthToken } returns flowOf("hub-token")

        val worker = CloudProfileSyncWorker(context, params, repo, client)
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.retry(), result)
        coVerify { repo.setLastProfileSync(any(), eq("error"), any()) }
    }
}
