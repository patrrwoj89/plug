package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.ShimmerBox
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.TorrentsViewModel

@Composable
fun TorrentsScreen(
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var magnet by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(stringResource(id = R.string.torrents_title), style = AppTypography.headline)
        Spacer(modifier = Modifier.height(Spacing.md))
        TvOutlinedTextField(
            value = magnet,
            onValueChange = { magnet = it },
            label = { Text(stringResource(id = R.string.torrents_magnet)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            focusRequester = focusRequester,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        FocusableSurface(
            onClick = { viewModel.addMagnet(magnet) },
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text(stringResource(id = R.string.torrents_add), modifier = Modifier.padding(Spacing.md), style = AppTypography.title)
        }
        Spacer(modifier = Modifier.height(Spacing.lg))

        if (uiState.isLoading) {
            repeat(3) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        } else if (uiState.error != null) {
            ErrorState(message = uiState.error ?: "", onRetry = { viewModel.addMagnet(magnet) })
        } else if (uiState.torrents.isEmpty()) {
            EmptyState(message = stringResource(id = R.string.torrents_empty))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) {
                items(uiState.torrents, key = { it.id }) { item ->
                    FocusableSurface(
                        onClick = { onPlay(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(item.title, style = AppTypography.title)
                        }
                    }
                }
            }
        }
    }
}
