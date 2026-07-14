package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.MediaCard
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.WatchlistViewModel

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
        item { Text("Watchlist", style = AppTypography.headline, modifier = Modifier.padding(bottom = Spacing.md)) }

        if (savedItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your watchlist is empty.\nAdd items from the home screen.",
                        style = AppTypography.body,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
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
