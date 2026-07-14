package com.polishmediahub.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.screens.PinScreen
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

    @Test
    fun wideCard() {
        paparazzi.snapshot {
            TVHubTheme {
                WideCard(
                    item = MediaItem(
                        id = "snapshot:1",
                        title = "Wide Snapshot",
                        subtitle = "2024 • Drama",
                        description = "Sample description",
                        type = MediaItem.Type.MOVIE
                    ),
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun categoryRow() {
        paparazzi.snapshot {
            TVHubTheme {
                CategoryRow(
                    category = Category(
                        id = "snapshot:category",
                        name = "Snapshot Category",
                        items = List(5) { index ->
                            MediaItem(
                                id = "snapshot:$index",
                                title = "Movie $index",
                                subtitle = "2024",
                                type = MediaItem.Type.MOVIE
                            )
                        }
                    ),
                    onItemClick = {}
                )
            }
        }
    }

    @Test
    fun pinScreen() {
        paparazzi.snapshot {
            TVHubTheme {
                PinScreen(
                    onPinEntered = {},
                    onCancel = {}
                )
            }
        }
    }
}
