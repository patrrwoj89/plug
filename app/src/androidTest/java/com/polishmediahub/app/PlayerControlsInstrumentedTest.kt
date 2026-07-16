package com.polishmediahub.app

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polishmediahub.app.ui.screens.PlayerControls
import com.polishmediahub.app.ui.theme.TVHubTheme
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerControlsDisplayAudioAndSubtitleLabels() {
        var playClicked = false
        composeTestRule.setContent {
            TVHubTheme {
                PlayerControls(
                    title = "Test movie",
                    isPlaying = true,
                    isLive = false,
                    currentPosition = 10_000L,
                    duration = 60_000L,
                    audioLabel = "PL",
                    subtitleLabel = "PL",
                    onBack = {},
                    onPlayPause = { playClicked = true },
                    onSeek = {},
                    onEnterPip = {},
                    onCycleAudio = {},
                    onCycleSubtitle = {},
                    onOpenQuickSettings = {},
                    onSliderFocusChanged = {},
                    sliderFocusRequester = remember { FocusRequester() },
                    cinemaMode = false,
                    cinemaInfo = PlayerViewModel.CinemaInfo()
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Audio: PL").assertExists()
        composeTestRule.onNodeWithContentDescription("Subtitles: PL").assertExists()
        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assertTrue(playClicked)
    }
}
