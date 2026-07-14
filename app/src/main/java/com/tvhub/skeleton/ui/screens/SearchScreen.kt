package com.tvhub.skeleton.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.R
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.EmptyState
import com.tvhub.skeleton.ui.components.ErrorState
import com.tvhub.skeleton.ui.components.FocusableSurface
import com.tvhub.skeleton.ui.components.MediaCard
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Radius
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchFieldFocus = remember { FocusRequester() }

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

        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .focusRequester(searchFieldFocus)
                .onPreviewKeyEvent { event ->
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
            TextButton(onClick = viewModel::clearHistory) {
                Text(stringResource(id = R.string.clear_history), color = AppColor.OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        if (uiState.query.isNotBlank() && !uiState.isLoading && uiState.error == null) {
            Text(stringResource(id = R.string.results_count, uiState.results.size), style = AppTypography.title)

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
            IconButton(onClick = { /* remove single history entry, add to VM if needed */ }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.clear_history),
                    tint = AppColor.OnSurfaceVariant
                )
            }
        }
    }
}
