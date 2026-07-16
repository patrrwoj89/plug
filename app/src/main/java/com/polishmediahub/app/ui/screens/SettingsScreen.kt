package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onKeyEvent
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.polishmediahub.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.data.remote.health.HealthStatus
import com.polishmediahub.app.data.remote.health.SourceHealth
import com.polishmediahub.app.ui.viewmodel.LastEpgSyncState
import com.polishmediahub.app.ui.viewmodel.PinViewModel
import com.polishmediahub.app.ui.viewmodel.ProfileViewModel
import com.polishmediahub.app.ui.viewmodel.SettingsViewModel
import com.polishmediahub.app.ui.viewmodel.TraktPairingViewModel
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    pinViewModel: PinViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    traktPairingViewModel: TraktPairingViewModel = hiltViewModel()
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
    val mdbListApiKey by viewModel.mdbListApiKey.collectAsStateWithLifecycle()
    val lastTraktSync by viewModel.lastTraktSync.collectAsStateWithLifecycle()
    val cinemaMode by viewModel.cinemaMode.collectAsStateWithLifecycle()
    val autoSkipIntro by viewModel.autoSkipIntro.collectAsStateWithLifecycle()
    val defaultIntroEndSeconds by viewModel.defaultIntroEndSeconds.collectAsStateWithLifecycle()
    val defaultOutroDurationSeconds by viewModel.defaultOutroDurationSeconds.collectAsStateWithLifecycle()
    val useAlternativePlayer by viewModel.useAlternativePlayer.collectAsStateWithLifecycle()
    val preferredAudioType by viewModel.preferredAudioType.collectAsStateWithLifecycle()
    val nightModeEnabled by viewModel.nightModeEnabled.collectAsStateWithLifecycle()
    val dialogueBoostGainmB by viewModel.dialogueBoostGainmB.collectAsStateWithLifecycle()
    val amoledMode by viewModel.amoledMode.collectAsStateWithLifecycle()
    val pureBlackSurfaces by viewModel.pureBlackSurfaces.collectAsStateWithLifecycle()
    val tunneledPlaybackEnabled by viewModel.tunneledPlaybackEnabled.collectAsStateWithLifecycle()
    val exoplayerParallelConnections by viewModel.exoplayerParallelConnections.collectAsStateWithLifecycle()
    val exoplayerMinBufferMs by viewModel.exoplayerMinBufferMs.collectAsStateWithLifecycle()
    val exoplayerMaxBufferMs by viewModel.exoplayerMaxBufferMs.collectAsStateWithLifecycle()
    val exoplayerBufferForPlaybackMs by viewModel.exoplayerBufferForPlaybackMs.collectAsStateWithLifecycle()
    val exoplayerBufferForPlaybackAfterRebufferMs by viewModel.exoplayerBufferForPlaybackAfterRebufferMs.collectAsStateWithLifecycle()
    val exoplayerBackBufferMs by viewModel.exoplayerBackBufferMs.collectAsStateWithLifecycle()
    val exoplayerInitialAllocationCount by viewModel.exoplayerInitialAllocationCount.collectAsStateWithLifecycle()
    val exoplayerTargetBufferBytes by viewModel.exoplayerTargetBufferBytes.collectAsStateWithLifecycle()
    val streamRules by viewModel.streamRules.collectAsStateWithLifecycle()
    val bingeGroupingEnabled by viewModel.bingeGroupingEnabled.collectAsStateWithLifecycle()
    val useCloudflareBypass by viewModel.useCloudflareBypass.collectAsStateWithLifecycle()
    val cloudflareWorkerUrl by viewModel.cloudflareWorkerUrl.collectAsStateWithLifecycle()
    val cloudflareAuthToken by viewModel.cloudflareAuthToken.collectAsStateWithLifecycle()
    val homeAssistantUrl by viewModel.homeAssistantUrl.collectAsStateWithLifecycle()
    val homeAssistantToken by viewModel.homeAssistantToken.collectAsStateWithLifecycle()
    val homeAssistantWebhookEnabled by viewModel.homeAssistantWebhookEnabled.collectAsStateWithLifecycle()
    val lastProfileSync by viewModel.lastProfileSync.collectAsStateWithLifecycle()
    val pluginUpdateBadge by viewModel.pluginUpdateBadge.collectAsStateWithLifecycle()
    val pinEnabled by pinViewModel.pinEnabled.collectAsStateWithLifecycle()
    val pinCode by pinViewModel.pinCode.collectAsStateWithLifecycle()
    val lastEpgSync by viewModel.lastEpgSync.collectAsStateWithLifecycle()
    val profiles by profileViewModel.profiles.collectAsStateWithLifecycle()
    val currentProfile by profileViewModel.currentProfile.collectAsStateWithLifecycle()
    val sourceHealth by viewModel.sourceHealth.collectAsStateWithLifecycle()
    var pinVerified by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

        SettingsToggle(
            title = stringResource(id = R.string.settings_cinema_mode),
            subtitle = stringResource(id = R.string.settings_cinema_mode_subtitle),
            checked = cinemaMode,
            onCheckedChange = viewModel::setCinemaMode
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_use_alternative_player),
            subtitle = stringResource(id = R.string.settings_use_alternative_player_subtitle),
            checked = useAlternativePlayer,
            onCheckedChange = viewModel::setUseAlternativePlayer
        )

        Text(
            text = stringResource(id = R.string.settings_audio_premium_title),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        val audioTypeOptions = listOf(
            stringResource(id = R.string.settings_preferred_audio_lektor) to "lector",
            stringResource(id = R.string.settings_preferred_audio_dubbing) to "dubbing"
        )
        val audioTypeDisplay = audioTypeOptions.find { it.second == preferredAudioType }?.first
            ?: audioTypeOptions.first().first
        SettingsSelector(
            title = stringResource(id = R.string.settings_preferred_audio_type),
            value = audioTypeDisplay,
            options = audioTypeOptions.map { it.first },
            onSelect = { label ->
                audioTypeOptions.find { it.first == label }?.second?.let(viewModel::setPreferredAudioType)
            }
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_night_mode),
            subtitle = stringResource(id = R.string.settings_night_mode_subtitle),
            checked = nightModeEnabled,
            onCheckedChange = viewModel::setNightModeEnabled
        )

        Text(
            text = stringResource(id = R.string.settings_dialogue_boost),
            style = AppTypography.title
        )
        Text(
            text = stringResource(id = R.string.settings_dialogue_boost_subtitle),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )
        Text(
            text = "$dialogueBoostGainmB mB",
            style = AppTypography.caption,
            color = AppColor.OnSurface
        )
        Slider(
            value = dialogueBoostGainmB.toFloat(),
            onValueChange = { viewModel.setDialogueBoostGainmB(it.roundToInt().coerceIn(0, 3000)) },
            valueRange = 0f..3000f,
            steps = 29,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .focusProperties { canFocus = true }
                .focusable()
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            viewModel.setDialogueBoostGainmB((dialogueBoostGainmB + 100).coerceIn(0, 3000))
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            viewModel.setDialogueBoostGainmB((dialogueBoostGainmB - 100).coerceIn(0, 3000))
                            true
                        }
                        else -> false
                    }
                }
        )

        Text(
            text = stringResource(id = R.string.settings_appearance_and_playback_premium_title),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_amoled_mode),
            subtitle = stringResource(id = R.string.settings_amoled_mode_subtitle),
            checked = amoledMode,
            onCheckedChange = viewModel::setAmoledMode
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_pure_black_surfaces),
            subtitle = stringResource(id = R.string.settings_pure_black_surfaces_subtitle),
            checked = pureBlackSurfaces,
            onCheckedChange = viewModel::setPureBlackSurfaces
        )

        Text(
            text = stringResource(id = R.string.settings_exoplayer_tuning_title),
            style = AppTypography.title,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_tunneled_playback),
            subtitle = stringResource(id = R.string.settings_tunneled_playback_subtitle),
            checked = tunneledPlaybackEnabled,
            onCheckedChange = viewModel::setTunneledPlaybackEnabled
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_parallel_connections),
            value = exoplayerParallelConnections.toFloat(),
            valueRange = 1f..16f,
            steps = 14,
            onValueChange = { viewModel.setExoplayerParallelConnections(it.roundToInt()) }
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_min_buffer),
            value = exoplayerMinBufferMs.toFloat(),
            valueRange = 1000f..120000f,
            steps = 118,
            onValueChange = { viewModel.setExoplayerMinBufferMs(it.roundToInt()) },
            valueDisplay = { "${it.roundToInt()} ms" }
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_max_buffer),
            value = exoplayerMaxBufferMs.toFloat(),
            valueRange = 1000f..1200000f,
            steps = 1199,
            onValueChange = { viewModel.setExoplayerMaxBufferMs(it.roundToInt()) },
            valueDisplay = { "${it.roundToInt()} ms" }
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_back_buffer),
            value = exoplayerBackBufferMs.toFloat(),
            valueRange = 0f..120000f,
            steps = 119,
            onValueChange = { viewModel.setExoplayerBackBufferMs(it.roundToInt()) },
            valueDisplay = { "${it.roundToInt()} ms" }
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_initial_allocation),
            value = exoplayerInitialAllocationCount.toFloat(),
            valueRange = 0f..64f,
            steps = 63,
            onValueChange = { viewModel.setExoplayerInitialAllocationCount(it.roundToInt()) },
            valueDisplay = { "${it.roundToInt()}" }
        )

        SettingsSlider(
            title = stringResource(id = R.string.settings_exoplayer_target_buffer),
            value = exoplayerTargetBufferBytes.coerceAtLeast(0).toFloat(),
            valueRange = 0f..2_000_000_000f,
            steps = 1999,
            onValueChange = { viewModel.setExoplayerTargetBufferBytes(it.roundToInt()) },
            valueDisplay = { "${it.roundToInt()}" }
        )

        Text(
            text = stringResource(id = R.string.settings_stream_rules_title),
            style = AppTypography.title,
            modifier = Modifier.padding(top = Spacing.md)
        )
        Text(
            text = stringResource(id = R.string.settings_stream_rules_subtitle),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )
        TvOutlinedTextField(
            value = streamRules,
            onValueChange = viewModel::setStreamRules,
            label = { Text(stringResource(id = R.string.settings_stream_rules_title)) },
            placeholder = { Text("{\"enabled\":true,\"sizeMinMb\":500,\"sizeMaxMb\":51200,\"resolutions\":[\"1080p\",\"4K\"]}") },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_binge_grouping),
            subtitle = stringResource(id = R.string.settings_binge_grouping_subtitle),
            checked = bingeGroupingEnabled,
            onCheckedChange = viewModel::setBingeGroupingEnabled
        )

        Text(
            text = stringResource(id = R.string.settings_cloudflare_title),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_cloudflare_bypass),
            subtitle = stringResource(id = R.string.settings_cloudflare_bypass_subtitle),
            checked = useCloudflareBypass,
            onCheckedChange = viewModel::setUseCloudflareBypass
        )

        TvOutlinedTextField(
            value = cloudflareWorkerUrl,
            onValueChange = viewModel::setCloudflareWorkerUrl,
            label = { Text(stringResource(id = R.string.settings_cloudflare_worker_url)) },
            placeholder = { Text(stringResource(id = R.string.settings_cloudflare_worker_url_hint)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        TvOutlinedTextField(
            value = cloudflareAuthToken,
            onValueChange = viewModel::setCloudflareAuthToken,
            label = { Text(stringResource(id = R.string.settings_cloudflare_auth_token)) },
            placeholder = { Text(stringResource(id = R.string.settings_cloudflare_auth_token_hint)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default
        )

        Text(
            text = stringResource(id = R.string.settings_home_assistant_section),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_home_assistant_webhook_enabled),
            subtitle = stringResource(id = R.string.settings_home_assistant_webhook_enabled_subtitle),
            checked = homeAssistantWebhookEnabled,
            onCheckedChange = viewModel::setHomeAssistantWebhookEnabled
        )

        TvOutlinedTextField(
            value = homeAssistantUrl,
            onValueChange = viewModel::setHomeAssistantUrl,
            label = { Text(stringResource(id = R.string.settings_home_assistant_url)) },
            placeholder = { Text(stringResource(id = R.string.settings_home_assistant_url_hint)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        TvOutlinedTextField(
            value = homeAssistantToken,
            onValueChange = viewModel::setHomeAssistantToken,
            label = { Text(stringResource(id = R.string.settings_home_assistant_token)) },
            placeholder = { Text(stringResource(id = R.string.settings_home_assistant_token_hint)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default
        )

        Text(
            text = stringResource(id = R.string.settings_profile_sync_section),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        val locale = LocalLocale.current.platformLocale
        val profileSyncDate = if (lastProfileSync.at > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(lastProfileSync.at))
        } else {
            stringResource(id = R.string.sync_status_never)
        }
        val profileSyncStatus = when (lastProfileSync.status) {
            "success" -> stringResource(id = R.string.sync_status_success)
            "error" -> stringResource(id = R.string.sync_status_error) + (lastProfileSync.error?.let { " ($it)" } ?: "")
            else -> if (lastProfileSync.at > 0L) lastProfileSync.status else ""
        }
        Text(
            text = stringResource(id = R.string.profile_last_sync, profileSyncDate, profileSyncStatus),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        TvButton(onClick = { viewModel.syncProfilesNow() }) {
            Text(
                text = stringResource(id = R.string.profile_sync_now),
                color = AppColor.Black,
                style = AppTypography.button
            )
        }

        Text(
            text = stringResource(id = R.string.settings_plugin_update_section),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            TvButton(onClick = { viewModel.runPluginUpdateNow() }) {
                Text(
                    text = stringResource(id = R.string.plugin_update_now),
                    color = AppColor.Black,
                    style = AppTypography.button
                )
            }
            if (pluginUpdateBadge.count > 0) {
                Box(
                    modifier = Modifier
                        .background(AppColor.Error, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = pluginUpdateBadge.count.toString(),
                        color = AppColor.Black,
                        style = AppTypography.caption
                    )
                }
            }
        }

        val pluginUpdateDate = if (pluginUpdateBadge.at > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(pluginUpdateBadge.at))
        } else {
            stringResource(id = R.string.sync_status_never)
        }
        Text(
            text = stringResource(id = R.string.plugin_last_check, pluginUpdateDate),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        if (pluginUpdateBadge.count > 0) {
            TvButton(onClick = { viewModel.clearPluginUpdateBadge() }) {
                Text(
                    text = stringResource(id = R.string.plugin_update_clear_badge),
                    color = AppColor.Black,
                    style = AppTypography.button
                )
            }
        }

        Text(
            text = stringResource(id = R.string.settings_skip_intro_section),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SettingsToggle(
            title = stringResource(id = R.string.settings_auto_skip_intro),
            subtitle = stringResource(id = R.string.settings_auto_skip_intro_subtitle),
            checked = autoSkipIntro,
            onCheckedChange = viewModel::setAutoSkipIntro
        )

        TvOutlinedTextField(
            value = defaultIntroEndSeconds.toString(),
            onValueChange = {
                it.toIntOrNull()?.coerceIn(1, 600)?.let(viewModel::setDefaultIntroEndSeconds)
            },
            label = { Text(stringResource(id = R.string.settings_intro_end_seconds)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        TvOutlinedTextField(
            value = defaultOutroDurationSeconds.toString(),
            onValueChange = {
                it.toIntOrNull()?.coerceIn(1, 600)?.let(viewModel::setDefaultOutroDurationSeconds)
            },
            label = { Text(stringResource(id = R.string.settings_outro_duration_seconds)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        SettingsSelector(
            title = stringResource(id = R.string.settings_preferred_quality),
            value = preferredQuality,
            options = listOf("Auto", "1080p", "720p", "480p"),
            onSelect = viewModel::setPreferredQuality
        )

        TvOutlinedTextField(
            value = mdbListApiKey,
            onValueChange = viewModel::setMdbListApiKey,
            label = { Text(stringResource(id = R.string.settings_mdblist_api_key)) },
            placeholder = { Text(stringResource(id = R.string.settings_mdblist_api_key_hint)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default
        )

        Text(
            text = stringResource(id = R.string.trakt_sync_section),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        TraktPairingSection(viewModel = traktPairingViewModel)

        val syncTraktStatus = remember(lastTraktSync) { formatTraktSyncStatus(context, lastTraktSync) }
        Text(
            text = syncTraktStatus,
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        TvButton(onClick = { viewModel.syncTraktNow() }) {
            Text(
                text = stringResource(id = R.string.trakt_sync_now),
                color = AppColor.Black,
                style = AppTypography.button
            )
        }

        SettingsToggle(
            title = stringResource(id = R.string.settings_pin_enabled),
            subtitle = stringResource(id = R.string.settings_pin),
            checked = pinEnabled,
            onCheckedChange = { enabled ->
                if (!enabled) pinViewModel.setPin("", false)
            }
        )

        Text(
            text = stringResource(id = R.string.parental_control),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        ParentalControlSection(
            profiles = profiles,
            currentProfileId = currentProfile?.id,
            onUpdate = { profileId, maxAgeRating, allowNsfw ->
                profileViewModel.updateParentalControls(profileId, maxAgeRating, allowNsfw)
            }
        )

        TvOutlinedTextField(
            value = pinCode,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinViewModel.setPin(it, pinEnabled) },
            label = { Text(stringResource(id = R.string.settings_pin)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions.Default
        )

        Text(
            text = stringResource(id = R.string.settings_source_health),
            style = AppTypography.headline,
            modifier = Modifier.padding(top = Spacing.md)
        )

        SourceHealthSection(
            status = sourceHealth,
            onCheckNow = { viewModel.checkSourceHealthNow() }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        val syncStatusText = remember(lastEpgSync) { formatEpgSyncStatus(context, lastEpgSync) }
        Text(
            text = syncStatusText,
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

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

@Composable
private fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueDisplay: (Float) -> String = { it.roundToInt().toString() }
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = AppTypography.title)
        Text(valueDisplay(value), style = AppTypography.caption, color = AppColor.OnSurface)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { onValueChange(it) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .focusProperties { canFocus = true }
                .focusable()
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                    val step = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onValueChange((value + step).coerceAtMost(valueRange.endInclusive))
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onValueChange((value - step).coerceAtLeast(valueRange.start))
                            true
                        }
                        else -> false
                    }
                }
        )
    }
}

@Composable
private fun ParentalControlSection(
    profiles: List<com.polishmediahub.app.data.local.ProfileEntity>,
    currentProfileId: String?,
    onUpdate: (String, String?, Boolean) -> Unit
) {
    val ageOptions = listOf(
        stringResource(id = R.string.age_rating_no_limit) to null,
        "G" to "G",
        "PG" to "PG",
        "PG-13" to "PG-13",
        "R" to "R",
        "NC-17" to "NC-17",
        "7+" to "7",
        "12+" to "12",
        "16+" to "16",
        "18+" to "18"
    )

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (profiles.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_profiles),
                style = AppTypography.caption,
                color = AppColor.OnSurfaceVariant
            )
            return
        }
        profiles.forEach { profile ->
            val isCurrent = profile.id == currentProfileId
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = profile.name + if (isCurrent) " ${stringResource(id = R.string.parental_control_current)}" else "",
                    style = AppTypography.title
                )

                val currentValue = ageOptions.find { it.second == profile.maxAgeRating }?.first
                    ?: stringResource(id = R.string.age_rating_no_limit)

                SettingsSelector(
                    title = stringResource(id = R.string.parental_control_max_age_rating),
                    value = currentValue,
                    options = ageOptions.map { it.first },
                    onSelect = { selected ->
                        val rating = ageOptions.find { it.first == selected }?.second
                        onUpdate(profile.id, rating, profile.allowNsfw)
                    }
                )

                SettingsToggle(
                    title = stringResource(id = R.string.parental_control_allow_nsfw),
                    subtitle = stringResource(id = R.string.parental_control_allow_nsfw_subtitle),
                    checked = profile.allowNsfw,
                    onCheckedChange = { allowed ->
                        onUpdate(profile.id, profile.maxAgeRating, allowed)
                    }
                )
            }
        }
    }
}

private fun formatEpgSyncStatus(context: android.content.Context, state: LastEpgSyncState): String {
    val date = if (state.at > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(state.at))
    } else {
        context.getString(R.string.epg_status_never)
    }
    val status = when (state.status) {
        "success" -> context.getString(R.string.epg_status_success)
        "error" -> context.getString(R.string.epg_status_error) + (state.error?.let { " ($it)" } ?: "")
        else -> if (state.at > 0L) state.status else ""
    }
    return context.getString(R.string.epg_last_sync, date, status)
}

private fun formatTraktSyncStatus(context: android.content.Context, state: com.polishmediahub.app.ui.viewmodel.LastTraktSyncState): String {
    val date = if (state.at > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(state.at))
    } else {
        context.getString(R.string.trakt_sync_status_never)
    }
    val status = when (state.status) {
        "success" -> context.getString(R.string.trakt_sync_status_success)
        "error" -> context.getString(R.string.trakt_sync_status_error) + (state.error?.let { " ($it)" } ?: "")
        else -> if (state.at > 0L) state.status else ""
    }
    return context.getString(R.string.trakt_last_sync, date, status)
}

private fun formatHealthCheckDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun SourceHealthSection(
    status: HealthStatus?,
    onCheckNow: () -> Unit
) {
    if (status == null) {
        Text(
            text = stringResource(id = R.string.health_status_no_data),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )
    } else {
        val date = if (status.lastCheckAt > 0L) {
            formatHealthCheckDate(status.lastCheckAt)
        } else {
            stringResource(id = R.string.health_status_never)
        }
        Text(
            text = stringResource(id = R.string.health_last_check, date),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            status.sources.forEach { source ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (source.status) {
                                    SourceHealth.ONLINE -> AppColor.Success
                                    SourceHealth.OFFLINE -> AppColor.Error
                                    else -> AppColor.OnSurfaceMuted
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = source.label,
                        style = AppTypography.title,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = when (source.status) {
                            SourceHealth.ONLINE -> stringResource(id = R.string.health_status_online)
                            SourceHealth.OFFLINE -> stringResource(id = R.string.health_status_offline)
                            else -> stringResource(id = R.string.health_status_unconfigured)
                        },
                        style = AppTypography.caption,
                        color = when (source.status) {
                            SourceHealth.ONLINE -> AppColor.Success
                            SourceHealth.OFFLINE -> AppColor.Error
                            else -> AppColor.OnSurfaceMuted
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.sm))
    TvButton(onClick = onCheckNow) {
        Text(
            text = stringResource(id = R.string.health_check_now),
            color = AppColor.Black,
            style = AppTypography.button
        )
    }
}
