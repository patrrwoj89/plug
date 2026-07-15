package com.polishmediahub.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.navigation.TVApp
import com.polishmediahub.app.ui.screens.SplashRoute
import com.polishmediahub.app.ui.theme.TVHubTheme
import com.polishmediahub.app.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val darkTheme by settingsViewModel.darkTheme.collectAsStateWithLifecycle()
            TVHubTheme(darkTheme = darkTheme) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                if (showSplash) {
                    SplashRoute(onSplashFinished = { showSplash = false })
                } else {
                    TVApp()
                }
            }
        }
    }
}
