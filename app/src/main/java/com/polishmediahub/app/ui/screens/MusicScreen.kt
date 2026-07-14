package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.model.AudioTrack
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.ShimmerBox
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.MusicViewModel

@Composable
fun MusicScreen(
    onPlay: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(stringResource(id = R.string.music_title), style = AppTypography.headline)

        Spacer(modifier = Modifier.height(Spacing.md))

        var query by remember { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(id = R.string.music_search_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        when {
            uiState.isLoading -> {
                repeat(4) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }
            uiState.error != null -> ErrorState(message = uiState.error ?: "", onRetry = { viewModel.load() })
            uiState.tracks.isEmpty() -> EmptyState(message = stringResource(id = R.string.music_empty))
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(vertical = Spacing.md)
                ) {
                    items(uiState.tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onPlay(track) },
                            onDownload = { viewModel.downloadTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, onClick: () -> Unit, onDownload: () -> Unit) {
    FocusableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = AppTypography.title)
                if (track.artist.isNotBlank()) {
                    Text(track.artist, style = AppTypography.body)
                }
            }
            FocusableSurface(
                onClick = onDownload,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            ) {
                Text(stringResource(id = R.string.download_audio), modifier = Modifier.padding(Spacing.sm))
            }
        }
    }
}
