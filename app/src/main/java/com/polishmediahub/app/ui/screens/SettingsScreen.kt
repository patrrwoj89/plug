package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.polishmediahub.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PinViewModel
import com.polishmediahub.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    pinViewModel: PinViewModel = hiltViewModel()
) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val autoplayTrailers by viewModel.autoplayTrailers.collectAsStateWithLifecycle()
    val saveSearchHistory by viewModel.saveSearchHistory.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val spoilerBlurEnabled by viewModel.spoilerBlurEnabled.collectAsStateWithLifecycle()
    val subtitleSize by viewModel.subtitleSize.collectAsStateWithLifecycle()
    val subtitleColor by viewModel.subtitleColor.collectAsStateWithLifecycle()
    val subtitleVerticalOffset by viewModel.subtitleVerticalOffset.collectAsStateWithLifecycle()
    val showLoadingStats by viewModel.showLoadingStats.collectAsStateWithLifecycle()
    val pinEnabled by pinViewModel.pinEnabled.collectAsStateWithLifecycle()
    val pinCode by pinViewModel.pinCode.collectAsStateWithLifecycle()
    var pinVerified by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    if (pinEnabled && pinCode.isNotBlank() && !pinVerified) {
        PinScreen(
            onPinEntered = { entered ->
                if (entered == pinCode) {
                    pinVerified = true
                    pinError = false
                } else {
                    pinError = true
                }
            },
            onPinChanged = { pinError = false },
            onCancel = { onNavigate(Screen.Home) },
            isError = pinError
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(stringResource(id = R.string.settings), style = AppTypography.headline)

        SettingsToggle(
            title = stringResource(id = R.string.settings_dark_theme),
            subtitle = stringResource(id = R.string.settings_dark_theme_subtitle),
            checked = darkTheme,
            onCheckedChange = viewModel::setDarkTheme
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_autoplay_trailers),
            subtitle = stringResource(id = R.string.settings_autoplay_trailers_subtitle),
            checked = autoplayTrailers,
            onCheckedChange = viewModel::setAutoplayTrailers
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_save_search_history),
            subtitle = stringResource(id = R.string.settings_save_search_history_subtitle),
            checked = saveSearchHistory,
            onCheckedChange = viewModel::setSaveSearchHistory
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_spoiler_blur),
            subtitle = stringResource(id = R.string.settings_spoiler_blur_subtitle),
            checked = spoilerBlurEnabled,
            onCheckedChange = viewModel::setSpoilerBlur
        )

        val subtitleSizeOptions = listOf("14sp" to 14f, "18sp" to 18f, "24sp" to 24f, "32sp" to 32f)
        val subtitleColorOptions = listOf(
            R.string.subtitle_color_white to "White",
            R.string.subtitle_color_yellow to "Yellow",
            R.string.subtitle_color_gray to "Gray"
        )
        val subtitleOffsetOptions = listOf("-50" to -50f, "-25" to -25f, "0" to 0f, "+25" to 25f, "+50" to 50f)
        val whiteLabel = stringResource(id = R.string.subtitle_color_white)
        val yellowLabel = stringResource(id = R.string.subtitle_color_yellow)
        val grayLabel = stringResource(id = R.string.subtitle_color_gray)
        val colorDisplayToKey = mapOf(
            whiteLabel to "White",
            yellowLabel to "Yellow",
            grayLabel to "Gray"
        )

        Text(
            text = stringResource(id = R.string.settings_subtitles),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsSelector(
            title = stringResource(id = R.string.settings_subtitle_size),
            value = subtitleSizeOptions.find { it.second == subtitleSize }?.first ?: "18sp",
            options = subtitleSizeOptions.map { it.first },
            onSelect = { label ->
                subtitleSizeOptions.find { it.first == label }?.second?.let(viewModel::setSubtitleSize)
            }
        )

        SettingsSelector(
            title = stringResource(id = R.string.settings_subtitle_color),
            value = colorDisplayToKey.entries.find { it.value == subtitleColor }?.key
                ?: whiteLabel,
            options = colorDisplayToKey.keys.toList(),
            onSelect = { label ->
                colorDisplayToKey[label]?.let(viewModel::setSubtitleColor)
            }
        )

        SettingsSelector(
            title = stringResource(id = R.string.settings_subtitle_vertical_offset),
            value = subtitleOffsetOptions.find { it.second == subtitleVerticalOffset }?.first ?: "0",
            options = subtitleOffsetOptions.map { it.first },
            onSelect = { label ->
                subtitleOffsetOptions.find { it.first == label }?.second?.let(viewModel::setSubtitleVerticalOffset)
            }
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_show_loading_stats),
            subtitle = stringResource(id = R.string.settings_show_loading_stats_subtitle),
            checked = showLoadingStats,
            onCheckedChange = viewModel::setShowLoadingStats
        )

        SettingsSelector(
            title = stringResource(id = R.string.settings_preferred_quality),
            value = preferredQuality,
            options = listOf("Auto", "1080p", "720p", "480p"),
            onSelect = viewModel::setPreferredQuality
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_pin_enabled),
            subtitle = stringResource(id = R.string.settings_pin),
            checked = pinEnabled,
            onCheckedChange = { enabled ->
                if (!enabled) pinViewModel.setPin("", false)
            }
        )

        TvOutlinedTextField(
            value = pinCode,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinViewModel.setPin(it, pinEnabled) },
            label = { Text(stringResource(id = R.string.settings_pin)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(id = R.string.version_info),
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
