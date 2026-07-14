package com.tvhub.skeleton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.tvhub.skeleton.navigation.TVApp
import com.tvhub.skeleton.ui.screens.SplashRoute
import com.tvhub.skeleton.ui.theme.TVHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            TVHubTheme {
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
