package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.polishmediahub.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.MediaCard
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val savedItems by viewModel.watchlistItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        item { Text(stringResource(id = R.string.watchlist), style = AppTypography.headline, modifier = Modifier.padding(bottom = Spacing.md)) }

        if (savedItems.isEmpty()) {
            item {
                EmptyState(
                    message = stringResource(id = R.string.watchlist_empty),
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else {
            items(savedItems, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onNavigate(Screen.Detail(item.id)) },
                    modifier = Modifier.fillParentMaxWidth(0.25f)
                )
            }
        }
    }
}
