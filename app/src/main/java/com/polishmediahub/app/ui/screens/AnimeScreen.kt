package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.CategoryRow
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.AnimeViewModel

@Composable
fun AnimeScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    viewModel: AnimeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item { Text(stringResource(id = R.string.anime), style = AppTypography.headline, modifier = Modifier.padding(horizontal = Spacing.lg)) }

        if (isLoading) {
            item { EmptyState(message = stringResource(id = R.string.loading), modifier = Modifier.fillParentMaxSize()) }
        } else if (categories.isEmpty()) {
            item { EmptyState(message = stringResource(id = R.string.no_featured_content), modifier = Modifier.fillParentMaxSize()) }
        } else {
            categories.forEach { category ->
                item {
                    CategoryRow(
                        category = category,
                        onItemClick = { item -> onNavigate(Screen.Detail(item.id)) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
