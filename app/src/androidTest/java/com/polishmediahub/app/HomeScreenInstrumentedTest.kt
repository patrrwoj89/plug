package com.polishmediahub.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.polishmediahub.app.ui.screens.HomeScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HomeScreenInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenRendersHeader() {
        hiltRule.inject()
        composeTestRule.setContent {
            HomeScreen(onNavigate = { /* no-op */ })
        }
        composeTestRule.onNodeWithText("No featured content", substring = true, ignoreCase = true).assertExists()
    }
}
