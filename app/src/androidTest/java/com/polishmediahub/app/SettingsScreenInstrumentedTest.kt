package com.polishmediahub.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.polishmediahub.app.ui.screens.SettingsScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsScreenInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsScreenRendersOptions() {
        hiltRule.inject()
        composeTestRule.setContent {
            SettingsScreen(onNavigate = { /* no-op */ })
        }
        composeTestRule.onNodeWithText("Settings", substring = true, ignoreCase = true).assertExists()
    }
}
