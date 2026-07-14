package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tvhub.skeleton.model.MediaItem
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.MediaCard
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing

@Composable
fun WatchlistScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val mockItems = (1..12).map { index ->
        MediaItem(
            id = "wl$index",
            title = "Watchlist $index",
            subtitle = "To watch",
            posterUrl = "https://picsum.photos/seed/wl$index/300/450"
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        item { Text("Watchlist", style = AppTypography.headline, modifier = Modifier.padding(bottom = Spacing.md)) }
        items(mockItems, key = { it.id }) { item ->
            MediaCard(
                item = item,
                onClick = { onNavigate(Screen.Detail(item.id)) },
                modifier = Modifier.fillParentMaxWidth(0.25f)
            )
        }
    }
}
