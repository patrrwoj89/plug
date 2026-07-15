package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.EssentialSetupViewModel

@Composable
fun EssentialSetupScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EssentialSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var iptvSelected by remember { mutableStateOf(true) }
    var musicSelected by remember { mutableStateOf(true) }
    var webSelected by remember { mutableStateOf(true) }
    val firstTileRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(id = R.string.essential_setup_title),
            style = AppTypography.headline,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.essential_setup_subtitle),
            style = AppTypography.body,
            textAlign = TextAlign.Center,
            color = AppColor.OnSurface.copy(alpha = 0.7f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterHorizontally),
            modifier = Modifier.padding(vertical = Spacing.lg)
        ) {
            SetupTile(
                title = stringResource(id = R.string.essential_setup_iptv_title),
                subtitle = stringResource(id = R.string.essential_setup_iptv_subtitle),
                selected = iptvSelected,
                onToggle = { iptvSelected = !iptvSelected },
                modifier = Modifier
                    .focusRequester(firstTileRequester)
                    .weight(1f)
            )
            SetupTile(
                title = stringResource(id = R.string.essential_setup_music_title),
                subtitle = stringResource(id = R.string.essential_setup_music_subtitle),
                selected = musicSelected,
                onToggle = { musicSelected = !musicSelected },
                modifier = Modifier.weight(1f)
            )
            SetupTile(
                title = stringResource(id = R.string.essential_setup_web_title),
                subtitle = stringResource(id = R.string.essential_setup_web_subtitle),
                selected = webSelected,
                onToggle = { webSelected = !webSelected },
                modifier = Modifier.weight(1f)
            )
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            firstTileRequester.requestFocus()
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error.orEmpty(),
                style = AppTypography.caption,
                color = AppColor.Error,
                textAlign = TextAlign.Center
            )
        }

        TvButton(
            onClick = {
                viewModel.applySelectedSources(
                    iptv = iptvSelected,
                    music = musicSelected,
                    web = webSelected,
                    onComplete = onComplete
                )
            },
            enabled = !uiState.isLoading
        ) {
            Text(
                text = stringResource(id = R.string.essential_setup_confirm),
                style = AppTypography.button,
                color = AppColor.Black
            )
        }
    }
}

@Composable
private fun SetupTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableSurface(
        onClick = onToggle,
        modifier = modifier.height(180.dp),
        backgroundColor = if (selected) AppColor.SurfaceHover else AppColor.SurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColor.Accent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = title,
                    style = AppTypography.title,
                    color = AppColor.OnSurface
                )
                Text(
                    text = subtitle,
                    style = AppTypography.caption,
                    color = AppColor.OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
