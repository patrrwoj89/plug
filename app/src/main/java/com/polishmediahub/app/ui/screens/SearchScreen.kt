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
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.MediaCard
import com.polishmediahub.app.ui.components.TvIconButton
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.components.TvTextButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchFieldFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val voicePrompt = stringResource(id = R.string.voice_search_prompt)
    val voiceNoResults = stringResource(id = R.string.voice_search_no_results)

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val query = matches?.firstOrNull { it.isNotBlank() }
            if (!query.isNullOrBlank()) {
                viewModel.submitSearch(query)
            } else {
                android.widget.Toast.makeText(context, voiceNoResults, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            voiceLauncher.launch(intent)
        } else {
            android.widget.Toast.makeText(context, voiceNoResults, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        searchFieldFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(
            text = stringResource(id = R.string.search),
            style = AppTypography.headline,
            modifier = Modifier.padding(bottom = Spacing.md)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            TvOutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f),
                focusRequester = searchFieldFocus,
                onPreviewKeyEvent = { event ->
                    if (event.key == Key.Enter) {
                        viewModel.submitSearch(uiState.query)
                        true
                    } else {
                        false
                    }
                },
                placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.search)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            TvIconButton(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(id = R.string.voice_search),
                onClick = ::launchVoiceSearch
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (uiState.isLoading) {
            EmptyState(message = stringResource(id = R.string.searching))
        } else if (uiState.error != null) {
            ErrorState(
                message = uiState.error ?: stringResource(id = R.string.item_not_found),
                onRetry = { viewModel.submitSearch(uiState.query) }
            )
        } else if (uiState.query.isBlank() && history.isNotEmpty()) {
            RowTitle(stringResource(id = R.string.recent_searches))
            history.forEach { query ->
                HistoryChip(
                    query = query,
                    onClick = { viewModel.submitSearch(query) }
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            TvTextButton(
                text = stringResource(id = R.string.clear_history),
                onClick = viewModel::clearHistory
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        if (uiState.query.isNotBlank() && !uiState.isLoading && uiState.error == null) {
            Text(pluralStringResource(id = R.plurals.results_count, count = uiState.results.size, uiState.results.size), style = AppTypography.title)

            if (uiState.results.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                EmptyState(message = stringResource(id = R.string.no_featured_content))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(vertical = Spacing.md)
                ) {
                    items(uiState.results, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onNavigate(Screen.Detail(item.id)) },
                            modifier = Modifier.fillMaxWidth(0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowTitle(text: String) {
    Text(
        text = text,
        style = AppTypography.titleLarge,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

@Composable
private fun HistoryChip(
    query: String,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.5f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
        backgroundColor = AppColor.SurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = AppColor.OnSurfaceVariant
            )
            Text(query, style = AppTypography.body, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.clear_history),
                tint = AppColor.OnSurfaceVariant
            )
        }
    }
}
