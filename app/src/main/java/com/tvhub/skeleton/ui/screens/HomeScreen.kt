package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.MediaCard
import com.tvhub.skeleton.ui.components.WideCard
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
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

@Composable
private fun HeroSection(
    item: MediaItem?,
    onClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = "No featured content", style = AppTypography.body)
        }
        return
    }

    WideCard(
        item = item,
        onClick = { onClick(item) },
        modifier = modifier
    )
}

@Composable
private fun CategoryRow(
    category: Category,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = Spacing.sm)) {
        Text(
            text = category.name,
            style = AppTypography.titleLarge,
            modifier = Modifier.padding(start = Spacing.lg, bottom = Spacing.sm)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(category.items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}
