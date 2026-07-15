package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.polishmediahub.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.MediaCard
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val savedItems by viewModel.libraryItems.collectAsStateWithLifecycle()

    if (savedItems.isEmpty()) {
        EmptyState(
            message = stringResource(id = R.string.library_empty),
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.lg)
                .focusGroup()
                .focusRestorer(),
            contentPadding = PaddingValues(vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(span = { GridItemSpan(5) }) {
                Text(
                    text = stringResource(id = R.string.library),
                    style = AppTypography.headline,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
            }

            items(savedItems, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onNavigate(Screen.Detail(item.id)) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
