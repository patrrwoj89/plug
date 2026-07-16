package com.polishmediahub.app.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel

val LocalPlayerViewModel = staticCompositionLocalOf<PlayerViewModel> {
    error("PlayerViewModel not provided")
}
