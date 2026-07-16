package com.polishmediahub.app.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.polishmediahub.app.ui.theme.TVHubTheme
import org.junit.Rule
import org.junit.Test

class PlayerControlsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(screenWidth = 1920, screenHeight = 1080)
    )

    @Test
    fun playerControlsEnglishLabels() {
        paparazzi.snapshot {
            TVHubTheme {
                PlayerControls(
                    title = "English movie",
                    isPlaying = false,
                    isLive = false,
                    currentPosition = 5_000L,
                    duration = 120_000L,
                    audioLabel = "EN",
                    subtitleLabel = "PL",
                    onBack = {},
                    onPlayPause = {},
                    onSeek = {},
                    onEnterPip = {},
                    onCycleAudio = {},
                    onCycleSubtitle = {},
                    onOpenQuickSettings = {},
                    onSliderFocusChanged = {},
                    sliderFocusRequester = remember { FocusRequester() },
                    cinemaMode = false,
                    cinemaInfo = com.polishmediahub.app.ui.viewmodel.PlayerViewModel.CinemaInfo()
                )
            }
        }
    }

    @Test
    fun playerControlsPolishLabels() {
        paparazzi.snapshot {
            TVHubTheme {
                PlayerControls(
                    title = "Polski film",
                    isPlaying = true,
                    isLive = false,
                    currentPosition = 10_000L,
                    duration = 60_000L,
                    audioLabel = "PL",
                    subtitleLabel = "PL",
                    onBack = {},
                    onPlayPause = {},
                    onSeek = {},
                    onEnterPip = {},
                    onCycleAudio = {},
                    onCycleSubtitle = {},
                    onOpenQuickSettings = {},
                    onSliderFocusChanged = {},
                    sliderFocusRequester = remember { FocusRequester() },
                    cinemaMode = false,
                    cinemaInfo = com.polishmediahub.app.ui.viewmodel.PlayerViewModel.CinemaInfo()
                )
            }
        }
    }
}
