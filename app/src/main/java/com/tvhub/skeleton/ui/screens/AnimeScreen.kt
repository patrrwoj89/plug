package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.components.CategoryRow
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.AnimeViewModel

@Composable
fun AnimeScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnimeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item { Text("Anime", style = AppTypography.headline, modifier = Modifier.padding(horizontal = Spacing.lg)) }

        if (isLoading) {
            item { Text("Loading...", modifier = Modifier.padding(horizontal = Spacing.lg)) }
        }

        categories.forEach { category ->
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
