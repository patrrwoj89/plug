package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvTextButton
import com.polishmediahub.app.ui.components.tvFocusable
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val isInLibrary by viewModel.isInLibrary.collectAsStateWithLifecycle()
    val isInWatchlist by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val blurDescription by viewModel.blurDescription.collectAsStateWithLifecycle(initialValue = false)

    if (item == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.item_not_found), style = AppTypography.body)
        }
        return
    }

    DetailContent(
        item = item!!,
        isInLibrary = isInLibrary,
        isInWatchlist = isInWatchlist,
        blurDescription = blurDescription,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onPlay = { onNavigate(Screen.Player(item!!.id)) },
        onToggleLibrary = viewModel::toggleLibrary,
        onToggleWatchlist = viewModel::toggleWatchlist,
        modifier = modifier
    )
}

@Composable
private fun DetailContent(
    item: MediaItem,
    isInLibrary: Boolean,
    isInWatchlist: Boolean,
    blurDescription: Boolean,
    onPlay: () -> Unit,
    onToggleLibrary: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val scrollState = rememberScrollState()
    var spoilerRevealed by remember(item.id) { mutableStateOf(false) }
    val shouldBlur = blurDescription && !spoilerRevealed

    val displayedTitle = if (shouldBlur) {
        val episode = item.episode
        if (episode != null) {
            stringResource(id = R.string.episode_title, episode)
        } else {
            stringResource(id = R.string.spoiler_hidden_title)
        }
    } else {
        item.title
    }
    val displayedSubtitle = if (shouldBlur) "" else item.subtitle
    val posterBlurModifier = if (shouldBlur) Modifier.blur(16.dp) else Modifier

    val posterModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxSize()
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "poster_${item.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        }
    } else {
        Modifier.fillMaxSize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .then(
                    if (shouldBlur) {
                        Modifier.tvFocusable(scale = 1f, onClick = { spoilerRevealed = true })
                    } else {
                        Modifier
                    }
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUrl ?: item.backdropUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = posterModifier.then(posterBlurModifier)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.6f to AppColor.Black.copy(alpha = 0.6f),
                            1.0f to AppColor.Black
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(displayedTitle, style = AppTypography.hero, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (displayedSubtitle.isNotBlank()) {
                    Text(displayedSubtitle, style = AppTypography.body)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    FocusableSurface(
                        onClick = onPlay,
                        modifier = Modifier.height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
                        backgroundColor = AppColor.Accent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(id = R.string.play),
                                tint = AppColor.Black
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(stringResource(id = R.string.play), style = AppTypography.button, color = AppColor.Black)
                        }
                    }

                    TvTextButton(
                        text = stringResource(
                            id = if (isInLibrary) R.string.remove_from_library else R.string.add_to_library
                        ),
                        onClick = onToggleLibrary
                    )

                    TvTextButton(
                        text = stringResource(
                            id = if (isInWatchlist) R.string.remove_from_watchlist else R.string.add_to_watchlist
                        ),
                        onClick = onToggleWatchlist
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(stringResource(id = R.string.overview), style = AppTypography.titleLarge)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        scale = 1f,
                        onClick = { if (shouldBlur) spoilerRevealed = true }
                    )
            ) {
                Text(
                    text = item.description,
                    style = AppTypography.body,
                    modifier = if (shouldBlur) Modifier.blur(16.dp) else Modifier
                )
            }

            if (shouldBlur) {
                Text(
                    text = stringResource(id = R.string.spoiler_reveal_hint),
                    style = AppTypography.caption,
                    color = AppColor.OnSurfaceVariant
                )
            }

            Text("${stringResource(id = R.string.genres)}: ${item.genres.joinToString(", ")}", style = AppTypography.caption)
            val yearLabel = stringResource(id = R.string.year)
            val durationLabel = stringResource(id = R.string.duration)
            val ratingLabel = stringResource(id = R.string.rating)
            val filmwebLabel = stringResource(id = R.string.filmweb_rating)
            Text(
                buildString {
                    append("$yearLabel: ${item.year}")
                    append("  •  $durationLabel: ${item.duration}")
                    append("  •  $ratingLabel: ${item.rating}")
                    item.filmwebRating?.let { append("  •  $filmwebLabel: $it") }
                },
                style = AppTypography.caption
            )
        }
    }
}
