package com.tvhub.skeleton.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tvhub.skeleton.R
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val resumePosition by viewModel.resumePosition.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer, item) {
        val videoUrl = item?.videoUrl
        if (!videoUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(ExoMediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            exoPlayer.seekTo(resumePosition)
        }

        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (item?.videoUrl != null) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { onNavigate(Screen.Home) }

    val activity = context.findActivity()
    PlayerContent(
        exoPlayer = exoPlayer,
        title = item?.title ?: stringResource(id = R.string.app_name),
        onBack = { onNavigate(Screen.Home) },
        onSaveProgress = { position, duration ->
            viewModel.saveProgress(position, duration)
        },
        onEnterPip = { activity?.enterPipMode() },
        modifier = modifier
    )
}

@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    title: String,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5_000)
            onSaveProgress(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                        exoPlayer.playPause()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        exoPlayer.seekBy(10_000)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        exoPlayer.seekBy(-10_000)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        controlsVisible = !controlsVisible
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        onBack()
                        true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                title = title,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onBack = onBack,
                onPlayPause = { exoPlayer.playPause() },
                onSeek = { position -> exoPlayer.seekTo(position.toLong()) },
                onEnterPip = onEnterPip
            )
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onEnterPip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Black.copy(alpha = 0.6f))
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = AppColor.OnSurface)
            }
            Text(
                text = title,
                style = AppTypography.titleLarge,
                modifier = Modifier.padding(start = Spacing.md)
            )
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(id = if (isPlaying) R.string.pause else R.string.play),
                        tint = AppColor.OnSurface
                    )
                }

                IconButton(onClick = onEnterPip) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Picture in picture",
                        tint = AppColor.OnSurface
                    )
                }

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = onSeek,
                    modifier = Modifier.weight(1f)
                        .focusProperties { canFocus = true },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "${formatMs(currentPosition)} / ${formatMs(duration)}",
                    style = AppTypography.caption
                )
            }
        }
    }
}

private fun Activity.enterPipMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: Exception) {
            // PIP may not be supported on this device.
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun ExoPlayer.playPause() {
    if (isPlaying) pause() else play()
}

private fun ExoPlayer.seekBy(deltaMs: Long) {
    seekTo((currentPosition + deltaMs).coerceAtLeast(0L))
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
