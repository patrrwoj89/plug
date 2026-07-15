package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.CategoryRow
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.ShimmerBox
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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
                    HeroFeaturedBanner(
                        item = uiState.featured.firstOrNull(),
                        onPlay = { item -> onNavigate(Screen.Detail(item.id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        focusRequester = firstItemRequester
                    )
                }

                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        CategoryRow(
                            category = com.polishmediahub.app.model.Category(
                                id = "continue",
                                name = stringResource(id = R.string.continue_watching),
                                items = uiState.continueWatching
                            ),
                            onItemClick = { item -> onNavigate(Screen.Detail(item.id)) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                uiState.categories.forEach { category ->
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
}

@Composable
private fun HeroFeaturedBanner(
    item: MediaItem?,
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    if (item == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(Radius.md))
    ) {
        AsyncImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to AppColor.Black.copy(alpha = 0.85f),
                        0.5f to AppColor.Black.copy(alpha = 0.0f)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to AppColor.Black.copy(alpha = 0.0f),
                        0.7f to AppColor.Black.copy(alpha = 0.6f),
                        1.0f to AppColor.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = item.title,
                style = AppTypography.hero,
                color = AppColor.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.year.isNotBlank()) {
                    Text(
                        text = item.year,
                        style = AppTypography.body,
                        color = AppColor.OnSurfaceVariant
                    )
                }

                if (item.rating.isNotBlank()) {
                    RatingBadge(text = "TMDB: ${item.rating}")
                }

                item.filmwebRating?.takeIf { it.isNotBlank() }?.let { rating ->
                    RatingBadge(text = "Filmweb: $rating")
                }
            }

            TvButton(
                onClick = { onPlay(item) },
                modifier = if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.play_now),
                        style = AppTypography.button
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = AppColor.Accent,
                shape = RoundedCornerShape(Radius.sm)
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTypography.badge,
            color = AppColor.Black
        )
    }
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
