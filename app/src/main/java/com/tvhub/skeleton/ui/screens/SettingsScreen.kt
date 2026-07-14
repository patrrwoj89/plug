package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing

@Composable
fun SettingsScreen(
    @Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("Settings", style = AppTypography.headline)

        SettingRow(
            title = "OLED black mode",
            description = "Use pure black background",
            initialChecked = true
        )
        SettingRow(
            title = "Auto-play trailers",
            description = "Play trailers on focus",
            initialChecked = false
        )
        SettingRow(
            title = "External player",
            description = "Open streams in external player",
            initialChecked = false
        )
        SettingRow(
            title = "Frame rate matching",
            description = "Match display refresh rate to content",
            initialChecked = true
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            "This is a skeleton. Add your own settings, backend integrations and sources here.",
            style = AppTypography.caption
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    initialChecked: Boolean
) {
    var checked by remember { mutableStateOf(initialChecked) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppTypography.title)
            Text(description, style = AppTypography.caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it }
        )
    }
}
