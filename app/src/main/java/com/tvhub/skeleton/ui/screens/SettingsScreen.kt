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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.FocusableSurface
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Radius
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val autoplayTrailers by viewModel.autoplayTrailers.collectAsStateWithLifecycle()
    val saveSearchHistory by viewModel.saveSearchHistory.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("Settings", style = AppTypography.headline)

        SettingsToggle(
            title = "Dark theme",
            subtitle = "Use the dark colour scheme for the TV interface",
            checked = darkTheme,
            onCheckedChange = viewModel::setDarkTheme
        )

        SettingsToggle(
            title = "Autoplay trailers",
            subtitle = "Preview trailers automatically on the detail screen",
            checked = autoplayTrailers,
            onCheckedChange = viewModel::setAutoplayTrailers
        )

        SettingsToggle(
            title = "Save search history",
            subtitle = "Remember recent searches and show suggestions",
            checked = saveSearchHistory,
            onCheckedChange = viewModel::setSaveSearchHistory
        )

        SettingsSelector(
            title = "Preferred stream quality",
            value = preferredQuality,
            options = listOf("Auto", "1080p", "720p", "480p"),
            onSelect = viewModel::setPreferredQuality
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "PolishMediaHub Skeleton v0.2.0",
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FocusableSurface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
        backgroundColor = AppColor.SurfaceVariant,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTypography.title)
                Text(subtitle, style = AppTypography.caption, color = AppColor.OnSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = Spacing.md)
            )
        }
    }
}

@Composable
private fun SettingsSelector(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = AppTypography.title)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { option ->
                val selected = option == value
                FocusableSurface(
                    onClick = { onSelect(option) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
                    backgroundColor = if (selected) AppColor.Accent else AppColor.SurfaceVariant,
                    focusedBackgroundColor = if (selected) AppColor.Accent else AppColor.SurfaceHover
                ) {
                    Text(
                        text = option,
                        style = AppTypography.button,
                        color = if (selected) AppColor.Black else AppColor.OnSurface,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}
