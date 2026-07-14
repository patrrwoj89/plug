package com.polishmediahub.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsScreenInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUpDisplay() {
        device.executeShellCommand("wm size 1920x1080")
        device.executeShellCommand("wm density 320")
    }

    @Test
    fun settingsScreenRendersOptions() {
        hiltRule.inject()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("polishmediahub://settings")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ActivityScenario.launch<MainActivity>(intent)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Dark theme").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Dark theme", substring = true, ignoreCase = true).assertExists()
    }
}
