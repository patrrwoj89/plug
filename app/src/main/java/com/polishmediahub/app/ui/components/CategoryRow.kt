package com.polishmediahub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing

@Composable
fun CategoryRow(
    category: Category,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = Spacing.sm)) {
        Text(
            text = category.name,
            style = AppTypography.titleLarge,
            modifier = Modifier
                .padding(start = Spacing.lg, bottom = Spacing.sm)
                .semantics { contentDescription = "Category: ${category.name}" }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(category.items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                )
            }
        }
    }
}
