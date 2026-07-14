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
import androidx.compose.ui.unit.dp
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.MediaCard
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing

@Composable
fun LibraryScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val mockItems = (1..20).map { index ->
        com.tvhub.skeleton.model.MediaItem(
            id = "lib$index",
            title = "Library item $index",
            subtitle = "Saved • 2024",
            posterUrl = "https://picsum.photos/seed/lib$index/300/450"
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        item { Text("Library", style = AppTypography.headline, modifier = Modifier.padding(bottom = Spacing.md)) }
        items(mockItems, key = { it.id }) { item ->
            MediaCard(
                item = item,
                onClick = { onNavigate(Screen.Detail(item.id)) },
                modifier = Modifier.fillParentMaxWidth(0.25f)
            )
        }
    }
}
