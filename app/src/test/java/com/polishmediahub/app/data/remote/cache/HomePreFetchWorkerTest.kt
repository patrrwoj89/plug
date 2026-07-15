package com.polishmediahub.app.data.remote.cache

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePreFetchWorkerTest {

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
    fun `doWork prefetches unique poster and backdrop urls from featured and categories`() = runTest(testDispatcher) {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        val params = mockk<WorkerParameters>(relaxed = true)
        val imageLoader = mockk<ImageLoader>(relaxed = true)
        val mediaRepository = mockk<MediaRepository>(relaxed = true)

        coEvery { mediaRepository.featured() } returns listOf(
            MediaItem(id = "1", title = "A", posterUrl = "https://example.com/a.jpg", backdropUrl = "https://example.com/a_back.jpg"),
            MediaItem(id = "2", title = "B", posterUrl = "https://example.com/b.jpg")
        )
        coEvery { mediaRepository.categories() } returns listOf(
            Category(
                id = "cat1",
                name = "Cat",
                items = listOf(
                    MediaItem(id = "3", title = "C", posterUrl = "https://example.com/c.jpg"),
                    MediaItem(id = "1", title = "A", posterUrl = "https://example.com/a.jpg") // duplicate id filtered
                )
            )
        )

        val executedUrls = mutableListOf<String>()
        coEvery { imageLoader.execute(any()) } coAnswers {
            executedUrls.add(firstArg<ImageRequest>().data.toString())
            mockk<SuccessResult>(relaxed = true)
        }

        val worker = HomePreFetchWorker(context, params, mediaRepository, imageLoader)
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertEquals(
            listOf(
                "https://example.com/a.jpg",
                "https://example.com/a_back.jpg",
                "https://example.com/b.jpg",
                "https://example.com/c.jpg"
            ),
            executedUrls
        )
        coVerify(exactly = 4) { imageLoader.execute(any()) }
    }
}
