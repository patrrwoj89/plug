package com.polishmediahub.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.theme.TVHubTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TVHubInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun focusableSurfaceRendersText() {
        composeTestRule.setContent {
            TVHubTheme {
                FocusableSurface(onClick = {}) {
                    androidx.compose.material3.Text("Focus me")
                }
            }
        }

        composeTestRule.onNodeWithText("Focus me").assertExists()
    }
}
