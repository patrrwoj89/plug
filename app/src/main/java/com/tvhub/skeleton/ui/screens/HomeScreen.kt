package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.R
import com.tvhub.skeleton.model.MediaItem
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.CategoryRow
import com.tvhub.skeleton.ui.components.EmptyState
import com.tvhub.skeleton.ui.components.ErrorState
import com.tvhub.skeleton.ui.components.ShimmerBox
import com.tvhub.skeleton.ui.components.WideCard
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberLazyListState()
    val firstItemRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstItemRequester.requestFocus()
    }

    when {
        uiState.isLoading -> {
            HomeLoading(modifier = modifier)
        }
        uiState.error != null -> {
            ErrorState(
                message = uiState.error ?: stringResource(id = R.string.item_not_found),
                modifier = modifier
            )
        }
        uiState.featured.isEmpty() && uiState.categories.isEmpty() -> {
            EmptyState(
                message = stringResource(id = R.string.no_featured_content),
                modifier = modifier
            )
        }
        else -> {
            LazyColumn(
                state = columnState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item {
                    HeroSection(
                        item = uiState.featured.firstOrNull(),
                        onClick = { item -> onNavigate(Screen.Detail(item.id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .focusRequester(firstItemRequester)
                    )
                }

                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        CategoryRow(
                            category = com.tvhub.skeleton.model.Category(
                                id = "continue",
                                name = stringResource(id = R.string.continue_watching),
                                items = uiState.continueWatching
                            ),
                            onItemClick = { item -> onNavigate(Screen.Detail(item.id)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                uiState.categories.forEach { category ->
                    item {
                        CategoryRow(
                            category = category,
                            onItemClick = { item -> onNavigate(Screen.Detail(item.id)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    item: MediaItem?,
    onClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    WideCard(
        item = item,
        onClick = { onClick(item) },
        modifier = modifier
    )
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(360.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(32.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(32.dp))
        Spacer(modifier = Modifier.height(Spacing.md))
        repeat(3) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(180.dp))
        }
    }
}
