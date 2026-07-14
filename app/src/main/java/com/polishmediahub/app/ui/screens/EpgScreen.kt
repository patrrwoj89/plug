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
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.ShimmerBox
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.EpgViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpgScreen(
    channelId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var url by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(stringResource(id = R.string.epg_title), style = AppTypography.headline)
        Spacer(modifier = Modifier.height(Spacing.md))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(id = R.string.epg_url)) },
            modifier = Modifier.fillMaxWidth(0.5f).focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        FocusableSurface(
            onClick = { viewModel.loadEpg(url) },
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            Text(stringResource(id = R.string.epg_load), modifier = Modifier.padding(Spacing.md), style = AppTypography.title)
        }
        Spacer(modifier = Modifier.height(Spacing.lg))

        if (uiState.isLoading) {
            repeat(4) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        } else if (uiState.error != null) {
            ErrorState(message = uiState.error ?: "", onRetry = { viewModel.loadEpg(url) })
        } else {
            channelId?.let { id ->
                val programs by viewModel.programForChannel(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                if (programs.isEmpty()) {
                    EmptyState(message = stringResource(id = R.string.epg_empty))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        contentPadding = PaddingValues(vertical = Spacing.md)
                    ) {
                        items(programs, key = { it.id }) { program ->
                            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
                                Text(program.title, style = AppTypography.title)
                                Text(
                                    "${formatTime(program.startTime)} – ${formatTime(program.endTime)}",
                                    style = AppTypography.body
                                )
                                if (program.description.isNotBlank()) {
                                    Text(program.description, style = AppTypography.caption)
                                }
                            }
                        }
                    }
                }
            } ?: EmptyState(message = stringResource(id = R.string.epg_empty))
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
