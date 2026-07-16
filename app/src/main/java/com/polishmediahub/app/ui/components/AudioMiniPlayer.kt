package com.polishmediahub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.polishmediahub.app.model.AudioTrack
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.AudioMiniPlayerViewModel

private val MINI_PLAYER_HEIGHT = 64.dp

/**
 * Persistent audio mini-player shown at the bottom of [HomeScreen] when the user is listening to
 * a radio stream or podcast. All state is collected with [collectAsStateWithLifecycle] (Zasada 4).
 */
@Composable
fun AudioMiniPlayer(
    onPlay: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioMiniPlayerViewModel = hiltViewModel()
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        val track = currentTrack ?: return@AnimatedVisibility
        MiniPlayerContent(
            track = track,
            isPlaying = isPlaying,
            onPlay = { onPlay(track) },
            onPauseResume = { viewModel.toggle() },
            onStop = { viewModel.stop() }
        )
    }
}

@Composable
private fun MiniPlayerContent(
    track: AudioTrack,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT)
            .background(AppColor.SurfaceVariant.copy(alpha = 0.95f))
            .clickable(onClick = onPlay),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.title,
                imageLoader = context.imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md)
            ) {
                Text(
                    text = track.title,
                    style = AppTypography.body,
                    color = AppColor.OnSurface,
                    maxLines = 1
                )
                if (track.artist.isNotBlank()) {
                    Text(
                        text = track.artist,
                        style = AppTypography.caption,
                        color = AppColor.OnSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            TvIconButton(
                onClick = onPauseResume,
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
            TvIconButton(
                onClick = onStop,
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop"
            )
        }
    }
}
