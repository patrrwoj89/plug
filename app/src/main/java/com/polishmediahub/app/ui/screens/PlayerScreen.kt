@file:Suppress("UnsafeOptInUsageError")
@file:android.annotation.SuppressLint("UnsafeOptInUsageError")
package com.polishmediahub.app.ui.screens

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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.polishmediahub.app.R
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
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
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
            .apply { setDrmHttpDataSourceFactory(dataSourceFactory) }
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = DefaultTrackSelector.Parameters.Builder(context)
                .setPreferredAudioLanguage("pl")
                .setPreferredTextLanguage("pl")
                .setSelectUndeterminedTextLanguage(true)
                .build()
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(DefaultRenderersFactory(context).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON))
            .build()
            .apply { playWhenReady = true }
    }

    DisposableEffect(exoPlayer, item) {
        val videoUrl = item?.videoUrl
        if (!videoUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(ExoMediaItem.fromUri(videoUrl))
            exoPlayer.prepare()
            exoPlayer.seekTo(resumePosition)
        }

        val mediaSession = MediaSession.Builder(context, exoPlayer).build()

        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            mediaSession.release()
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
    val onCycleAudio = remember(exoPlayer) { { cycleAudioTrack(exoPlayer) } }
    val onCycleSubtitle = remember(exoPlayer) { { cycleSubtitleTrack(exoPlayer) } }

    PlayerContent(
        exoPlayer = exoPlayer,
        title = item?.title ?: stringResource(id = R.string.app_name),
        onBack = { onNavigate(Screen.Home) },
        onSaveProgress = { position, duration ->
            viewModel.saveProgress(position, duration)
        },
        onEnterPip = { activity?.enterPipMode() },
        onCycleAudio = onCycleAudio,
        onCycleSubtitle = onCycleSubtitle,
        modifier = modifier
    )
}

@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    title: String,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onCycleAudio: () -> Unit,
    onCycleSubtitle: () -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var audioLabel by remember { mutableStateOf("") }
    var subtitleLabel by remember { mutableStateOf("") }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                updateTrackLabels(tracks, onAudio = { audioLabel = it }, onSubtitle = { subtitleLabel = it })
            }
        }
        exoPlayer.addListener(listener)
        updateTrackLabels(exoPlayer.currentTracks, onAudio = { audioLabel = it }, onSubtitle = { subtitleLabel = it })

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
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE,
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        exoPlayer.playPause()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> {
                        exoPlayer.seekBy(10_000)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> {
                        exoPlayer.seekBy(-10_000)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        controlsVisible = !controlsVisible
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_X -> {
                        onCycleAudio()
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_Y -> {
                        onCycleSubtitle()
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
                audioLabel = audioLabel,
                subtitleLabel = subtitleLabel,
                onBack = onBack,
                onPlayPause = { exoPlayer.playPause() },
                onSeek = { position -> exoPlayer.seekTo(position.toLong()) },
                onEnterPip = onEnterPip,
                onCycleAudio = onCycleAudio,
                onCycleSubtitle = onCycleSubtitle
            )
        }
    }
}

@Composable
internal fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    audioLabel: String,
    subtitleLabel: String,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onEnterPip: () -> Unit,
    onCycleAudio: () -> Unit,
    onCycleSubtitle: () -> Unit
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

                IconButton(onClick = onCycleAudio) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio: $audioLabel",
                        tint = AppColor.OnSurface
                    )
                }
                if (audioLabel.isNotBlank()) {
                    Text(audioLabel, style = AppTypography.caption, modifier = Modifier.padding(start = Spacing.xs))
                }

                IconButton(onClick = onCycleSubtitle) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = "Subtitles: $subtitleLabel",
                        tint = AppColor.OnSurface
                    )
                }
                if (subtitleLabel.isNotBlank()) {
                    Text(subtitleLabel, style = AppTypography.caption, modifier = Modifier.padding(start = Spacing.xs))
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

private fun cycleAudioTrack(player: ExoPlayer) {
    val audioGroups = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
    if (audioGroups.isEmpty()) return
    val currentIndex = audioGroups.indexOfFirst { group ->
        (0 until group.length).any { group.isTrackSelected(it) }
    }
    val nextIndex = (currentIndex + 1) % audioGroups.size
    val builder = player.trackSelectionParameters.buildUpon()
    for (i in audioGroups.indices) {
        val group = audioGroups[i]
        val indices = if (i == nextIndex) listOf(0) else emptyList()
        builder.setOverrideForType(
            androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, indices)
        )
    }
    player.trackSelectionParameters = builder.build()
}

private fun cycleSubtitleTrack(player: ExoPlayer) {
    val textGroups = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
    if (textGroups.isEmpty()) return
    val currentIndex = textGroups.indexOfFirst { group ->
        (0 until group.length).any { group.isTrackSelected(it) }
    }
    val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % (textGroups.size + 1)
    val builder = player.trackSelectionParameters.buildUpon()
    for (i in textGroups.indices) {
        val group = textGroups[i]
        val indices = if (i == nextIndex) listOf(0) else emptyList()
        builder.setOverrideForType(
            androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, indices)
        )
    }
    if (nextIndex == textGroups.size) {
        builder.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
    }
    player.trackSelectionParameters = builder.build()
}

private fun updateTrackLabels(
    tracks: androidx.media3.common.Tracks,
    onAudio: (String) -> Unit,
    onSubtitle: (String) -> Unit
) {
    val audioGroup = tracks.groups.find { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && it.isSelected }
    val audioFormat = audioGroup?.let { group ->
        (0 until group.length).find { group.isTrackSelected(it) }?.let { index ->
            group.getTrackFormat(index).language?.uppercase() ?: ""
        }
    } ?: ""
    onAudio(audioFormat)

    val textGroup = tracks.groups.find { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected }
    val textFormat = textGroup?.let { group ->
        (0 until group.length).find { group.isTrackSelected(it) }?.let { index ->
            group.getTrackFormat(index).language?.uppercase() ?: ""
        }
    } ?: "Off"
    onSubtitle(textFormat)
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
