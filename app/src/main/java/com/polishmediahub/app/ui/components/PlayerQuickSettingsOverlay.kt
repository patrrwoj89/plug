package com.polishmediahub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing

@Composable
fun PlayerQuickSettingsOverlay(
    visible: Boolean,
    nightModeEnabled: Boolean,
    preferredAudioType: String,
    useAlternativePlayer: Boolean,
    onToggleNightMode: () -> Unit,
    onCycleAudioType: () -> Unit,
    onTogglePlayerEngine: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstButtonFocus = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            firstButtonFocus.requestFocus()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvButton(
                    onClick = onToggleNightMode,
                    modifier = Modifier.focusRequester(firstButtonFocus)
                ) {
                    val labelRes = if (nightModeEnabled) {
                        R.string.quick_settings_night_mode_on
                    } else {
                        R.string.quick_settings_night_mode_off
                    }
                    Text(
                        text = stringResource(id = labelRes),
                        style = AppTypography.button,
                        color = AppColor.Black
                    )
                }

                TvButton(onClick = onCycleAudioType) {
                    val labelRes = if (preferredAudioType == "dubbing") {
                        R.string.quick_settings_audio_dubbing
                    } else {
                        R.string.quick_settings_audio_lektor
                    }
                    Text(
                        text = stringResource(id = labelRes),
                        style = AppTypography.button,
                        color = AppColor.Black
                    )
                }

                TvButton(onClick = onTogglePlayerEngine) {
                    val labelRes = if (useAlternativePlayer) {
                        R.string.quick_settings_switch_exo
                    } else {
                        R.string.quick_settings_switch_vlc
                    }
                    Text(
                        text = stringResource(id = labelRes),
                        style = AppTypography.button,
                        color = AppColor.Black
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TvButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.close),
                        style = AppTypography.button,
                        color = AppColor.Black
                    )
                }
            }
        }
    }
}
