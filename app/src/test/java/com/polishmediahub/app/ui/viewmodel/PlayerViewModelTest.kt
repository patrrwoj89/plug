package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.audio.AudioHistoryRepository
import com.polishmediahub.app.data.audio.AudioRepository
import com.polishmediahub.app.data.remote.homeassistant.HomeAssistantWebhookClient
import com.polishmediahub.app.data.remote.tmdb.TmdbMediaRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.TvLauncherManager
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.ui.player.VideoPipManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private fun createViewModel(
        homeAssistantWebhookClient: HomeAssistantWebhookClient = mockk(relaxed = true)
    ): Triple<PlayerViewModel, HomeAssistantWebhookClient, SettingsRepository> {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.preferredAudioType } returns flowOf("lector")
        every { settingsRepository.nightModeEnabled } returns flowOf(false)
        every { settingsRepository.dialogueBoostGainmB } returns flowOf(1000)
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

        val torrentMediaSource = mockk<TorrentMediaSource>(relaxed = true)
        every { torrentMediaSource.statusFlow } returns MutableStateFlow(emptyMap())
        every { torrentMediaSource.bufferingProgress } returns MutableStateFlow(emptyMap())

        val viewModel = PlayerViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to "test")),
            mediaRepository = mediaRepository,
            audioRepository = mockk(relaxed = true),
            audioHistoryRepository = mockk(relaxed = true),
            torrentMediaSource = torrentMediaSource,
            watchHistoryRepository = watchHistoryRepository,
            tvLauncherManager = mockk(relaxed = true),
            traktMediaRepository = mockk(relaxed = true),
            tmdbMediaRepository = mockk(relaxed = true),
            settingsRepository = settingsRepository,
            homeAssistantWebhookClient = homeAssistantWebhookClient,
            videoPipManager = VideoPipManager()
        )
        return Triple(viewModel, homeAssistantWebhookClient, settingsRepository)
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
            settingsRepository = settingsRepository,
            homeAssistantWebhookClient = mockk(relaxed = true),
            videoPipManager = VideoPipManager()
        )

        advanceUntilIdle()

        assertEquals("dubbing", viewModel.preferredAudioType.value)
        assertEquals(true, viewModel.nightModeEnabled.value)
        assertEquals(2000, viewModel.dialogueBoostGainmB.value)
    }

    @Test
    fun `setIsPlaying true sends play webhook`() = runTest(testDispatcher) {
        val (viewModel, homeAssistant, _) = createViewModel()
        advanceUntilIdle()

        viewModel.setIsPlaying(true)
        advanceUntilIdle()

        coVerify { homeAssistant.send("play", "Test Movie") }
    }

    @Test
    fun `setIsPlaying false sends pause webhook`() = runTest(testDispatcher) {
        val (viewModel, homeAssistant, _) = createViewModel()
        advanceUntilIdle()

        viewModel.setIsPlaying(false)
        advanceUntilIdle()

        coVerify { homeAssistant.send("pause", "Test Movie") }
    }

    @Test
    fun `onPlaybackStopped sends stop webhook`() = runTest(testDispatcher) {
        val (viewModel, homeAssistant, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onPlaybackStopped(0L, 1000L)
        advanceUntilIdle()

        coVerify { homeAssistant.send("stop", "Test Movie") }
    }

    @Test
    fun `toggleNightModeEnabled sends inverted value to settings repository`() = runTest(testDispatcher) {
        val (viewModel, _, settingsRepository) = createViewModel()
        advanceUntilIdle()

        viewModel.toggleNightModeEnabled()
        advanceUntilIdle()

        coVerify { settingsRepository.setNightModeEnabled(true) }
    }

    @Test
    fun `toggleUseAlternativePlayer sends inverted value to settings repository`() = runTest(testDispatcher) {
        val (viewModel, _, settingsRepository) = createViewModel()
        advanceUntilIdle()

        viewModel.toggleUseAlternativePlayer()
        advanceUntilIdle()

        coVerify { settingsRepository.setUseAlternativePlayer(true) }
    }

    @Test
    fun `cyclePreferredAudioType switches from lector to dubbing`() = runTest(testDispatcher) {
        val (viewModel, _, settingsRepository) = createViewModel()
        advanceUntilIdle()

        viewModel.cyclePreferredAudioType()
        advanceUntilIdle()

        coVerify { settingsRepository.setPreferredAudioType("dubbing") }
    }
}
