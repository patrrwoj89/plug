package com.polishmediahub.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.theme.TVHubTheme
import org.junit.Rule
import org.junit.Test

class ComponentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.NEXUS_5.copy(screenWidth = 1920, screenHeight = 1080))

    @Test
    fun focusableSurface() {
        paparazzi.snapshot {
            TVHubTheme {
                FocusableSurface(onClick = {}) {
                    androidx.compose.material3.Text("Focus me", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    @Test
    fun emptyState() {
        paparazzi.snapshot {
            TVHubTheme {
                EmptyState(message = "Brak treści")
            }
        }
    }

    @Test
    fun errorState() {
        paparazzi.snapshot {
            TVHubTheme {
                ErrorState(message = "Błąd sieci", onRetry = {})
            }
        }
    }

    @Test
    fun sidebar() {
        paparazzi.snapshot {
            TVHubTheme {
                Sidebar(
                    current = Screen.Home,
                    onNavigate = {}
                )
            }
        }
    }
}
