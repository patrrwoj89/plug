package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.audio.AudioHistoryRepository
import com.polishmediahub.app.data.audio.AudioRepository
import com.polishmediahub.app.data.remote.tmdb.TmdbMediaRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.TvLauncherManager
import com.polishmediahub.app.model.MediaItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `audio preferences and night mode states are emitted from settings repository`() = runTest(testDispatcher) {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.preferredAudioType } returns flowOf("dubbing")
        every { settingsRepository.nightModeEnabled } returns flowOf(true)
        every { settingsRepository.dialogueBoostGainmB } returns flowOf(2000)
        every { settingsRepository.preferredQuality } returns flowOf("Auto")
        every { settingsRepository.subtitleSize } returns flowOf(18f)
        every { settingsRepository.subtitleColor } returns flowOf("White")
        every { settingsRepository.subtitleVerticalOffset } returns flowOf(0f)
        every { settingsRepository.showLoadingStats } returns flowOf(false)
        every { settingsRepository.cinemaMode } returns flowOf(false)
        every { settingsRepository.autoSkipIntro } returns flowOf(true)
        every { settingsRepository.defaultIntroEndSeconds } returns flowOf(90)
        every { settingsRepository.defaultOutroDurationSeconds } returns flowOf(120)
        every { settingsRepository.useAlternativePlayer } returns flowOf(false)

        val watchHistoryRepository = mockk<WatchHistoryRepository>(relaxed = true)
        every { watchHistoryRepository.observePosition(any()) } returns flowOf(0L)

        val mediaRepository = mockk<MediaRepository>(relaxed = true)
        val mediaItem = MediaItem(id = "test", title = "Test Movie")
        coEvery { mediaRepository.byId("test") } returns mediaItem
        coEvery { mediaRepository.resolveItem(mediaItem) } returns mediaItem

        val viewModel = PlayerViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to "test")),
            mediaRepository = mediaRepository,
            audioRepository = mockk(relaxed = true),
            audioHistoryRepository = mockk(relaxed = true),
            torrentMediaSource = mockk(relaxed = true),
            watchHistoryRepository = watchHistoryRepository,
            tvLauncherManager = mockk(relaxed = true),
            traktMediaRepository = mockk(relaxed = true),
            tmdbMediaRepository = mockk(relaxed = true),
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        assertEquals("dubbing", viewModel.preferredAudioType.value)
        assertEquals(true, viewModel.nightModeEnabled.value)
        assertEquals(2000, viewModel.dialogueBoostGainmB.value)
    }
}
