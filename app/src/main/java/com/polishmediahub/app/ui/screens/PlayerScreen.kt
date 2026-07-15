@file:Suppress("UnsafeOptInUsageError")
@file:android.annotation.SuppressLint("UnsafeOptInUsageError")
package com.polishmediahub.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Typeface
import android.os.Build
import android.util.Rational
import android.util.TypedValue
import android.view.KeyEvent
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.core.net.toUri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.CaptionStyleCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.Format
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.polishmediahub.app.R
import com.polishmediahub.app.data.torrent.TorrentStatus
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.components.TvIconButton
import com.polishmediahub.app.ui.components.TvTextButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlayerScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val resumePosition by viewModel.resumePosition.collectAsStateWithLifecycle()
    val resolvedUrl by viewModel.resolvedUrl.collectAsStateWithLifecycle()
    val torrentStatus by viewModel.torrentStatus.collectAsStateWithLifecycle()
    val torrentBuffering by viewModel.torrentBuffering.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val nextEpisode by viewModel.nextEpisode.collectAsStateWithLifecycle()
    val autoPlayCancelled by viewModel.autoPlayCancelled.collectAsStateWithLifecycle()
    val subtitleSize by viewModel.subtitleSize.collectAsStateWithLifecycle()
    val subtitleColor by viewModel.subtitleColor.collectAsStateWithLifecycle()
    val subtitleVerticalOffset by viewModel.subtitleVerticalOffset.collectAsStateWithLifecycle()
    val showLoadingStats by viewModel.showLoadingStats.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val headers = item?.headers.orEmpty()
    val videoUrl = resolvedUrl ?: item?.videoUrl
    val exoPlayer = remember(context, videoUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .apply {
                setUserAgent("PolishMediaHub/1.0 (Android TV)")
                if (headers.isNotEmpty()) setDefaultRequestProperties(headers)
            }
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
        val isP2PStream = !videoUrl.isNullOrBlank() &&
            (videoUrl.contains("127.0.0.1") || videoUrl.contains("localhost", ignoreCase = true))
        val loadControl = if (isP2PStream) {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 60_000, 2_500, 10_000)
                .build()
        } else {
            DefaultLoadControl.Builder().build()
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setRenderersFactory(DefaultRenderersFactory(context).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON))
            .build()
            .apply { playWhenReady = true }
    }

    LaunchedEffect(preferredQuality, exoPlayer) {
        applyPreferredQuality(exoPlayer, preferredQuality)
    }

    DisposableEffect(exoPlayer) {
        viewModel.setPlayer(exoPlayer)
        onDispose { viewModel.setPlayer(null) }
    }

    DisposableEffect(exoPlayer, item, resolvedUrl) {
        val mediaItem = item
        val url = resolvedUrl ?: mediaItem?.videoUrl
        if (!url.isNullOrBlank()) {
            val mimeType = when {
                url.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                url.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                url.endsWith(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
                url.contains("/stream?", ignoreCase = true) -> null
                else -> null
            }
            val mediaItemBuilder = ExoMediaItem.Builder()
                .setUri(url)
                .setMimeType(mimeType)
            mediaItem?.subtitleUrl?.let { subUrl ->
                val subMimeType = when {
                    subUrl.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    subUrl.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    else -> MimeTypes.TEXT_VTT
                }
                val subUri = subUrl.toUri()
                val subConfig = androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(subMimeType)
                    .setLanguage(mediaItem.subtitleLanguage ?: "pl")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
            mediaItem?.let { item ->
                val drmUuid = when (item.drmScheme?.lowercase()) {
                    "widevine" -> C.WIDEVINE_UUID
                    "playready" -> C.PLAYREADY_UUID
                    "clearkey" -> C.CLEARKEY_UUID
                    else -> null
                }
                if (drmUuid != null && !item.drmLicenseUrl.isNullOrBlank()) {
                    val drmConfig = ExoMediaItem.DrmConfiguration.Builder(drmUuid)
                        .setLicenseUri(item.drmLicenseUrl.toUri())
                        .setLicenseRequestHeaders(item.drmHeaders)
                        .build()
                    mediaItemBuilder.setDrmConfiguration(drmConfig)
                }
            }
            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()
            exoPlayer.seekTo(resumePosition)
        }

        val mediaSession = MediaSession.Builder(context, exoPlayer).build()

        onDispose {
            viewModel.onPlaybackStopped(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            mediaSession.release()
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (videoUrl != null) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activity = context.findActivity()

    PlayerContent(
        exoPlayer = exoPlayer,
        mediaItem = item,
        title = item?.title ?: stringResource(id = R.string.app_name),
        nextEpisode = nextEpisode,
        autoPlayCancelled = autoPlayCancelled,
        onBack = { onNavigate(Screen.Home) },
        onSaveProgress = { position, duration ->
            viewModel.saveProgress(position, duration)
        },
        onScrobbleStart = { position, duration -> viewModel.scrobbleStart(position, duration) },
        onScrobbleStop = { position, duration -> viewModel.scrobbleStop(position, duration) },
        onReportProgress = { position, duration, isPlaying ->
            val state = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
            viewModel.reportPlaybackProgress(position, duration, state)
        },
        onEnterPip = { activity?.enterPipMode() },
        onCancelAutoPlay = { viewModel.cancelAutoPlay() },
        onPlayNextEpisode = { position, duration -> viewModel.playNextEpisode(position, duration) },
        torrentBuffering = torrentBuffering,
        torrentStatus = torrentStatus,
        showLoadingStats = showLoadingStats,
        playerStats = playerStats,
        subtitleSize = subtitleSize,
        subtitleColor = subtitleColor,
        subtitleVerticalOffset = subtitleVerticalOffset,
        modifier = modifier
    )
}

@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    mediaItem: MediaItem?,
    title: String,
    nextEpisode: MediaItem?,
    autoPlayCancelled: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    onScrobbleStart: (Long, Long) -> Unit,
    onScrobbleStop: (Long, Long) -> Unit,
    onReportProgress: (Long, Long, Boolean) -> Unit,
    onCancelAutoPlay: () -> Unit,
    onPlayNextEpisode: (Long, Long) -> Unit,
    torrentBuffering: Int?,
    torrentStatus: TorrentStatus?,
    showLoadingStats: Boolean,
    playerStats: com.polishmediahub.app.ui.viewmodel.PlayerViewModel.PlayerStats,
    subtitleSize: Float,
    subtitleColor: String,
    subtitleVerticalOffset: Float,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var audioOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var selectedAudioIndex by remember { mutableIntStateOf(-1) }
    var audioLabel by remember { mutableStateOf("") }
    var subtitleOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var selectedSubtitleIndex by remember { mutableIntStateOf(-1) }
    var subtitleLabel by remember { mutableStateOf("Off") }
    var sliderFocused by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoPlayTriggered by remember(mediaItem?.id) { mutableStateOf(false) }
    val isLive = mediaItem?.isLive == true
    val isSeriesLike = mediaItem?.type == MediaItem.Type.SERIES || mediaItem?.type == MediaItem.Type.EPISODE
    val remainingMs = if (duration > currentPosition) duration - currentPosition else 0L
    val overlayVisible = nextEpisode != null && !autoPlayCancelled && isSeriesLike && !isLive && remainingMs in 0..15_000
    val countdownSeconds = (remainingMs / 1000).toInt().coerceIn(0, 15)
    val coverUrl = mediaItem?.posterUrl
    val density = LocalDensity.current

    BackHandler { if (overlayVisible) onCancelAutoPlay() else onBack() }

    LaunchedEffect(exoPlayer, nextEpisode, autoPlayCancelled, isLive, isSeriesLike) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                if (playing) onScrobbleStart(pos, dur) else onScrobbleStop(pos, dur)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                audioOptions = tracks.audioOptions()
                subtitleOptions = tracks.textOptions()
                if (selectedAudioIndex == -1 && audioOptions.isNotEmpty()) {
                    selectedAudioIndex = audioOptions.preferredAudioIndex()
                    exoPlayer.applyAudioOption(selectedAudioIndex, audioOptions)
                }
                selectedAudioIndex = exoPlayer.findSelectedAudioIndex(audioOptions)
                audioLabel = audioOptions.getOrNull(selectedAudioIndex)?.label ?: ""
                selectedSubtitleIndex = exoPlayer.findSelectedSubtitleIndex(subtitleOptions)
                subtitleLabel = subtitleOptions.getOrNull(selectedSubtitleIndex)?.label ?: "Off"
            }
        }
        exoPlayer.addListener(listener)
        val tracks = exoPlayer.currentTracks
        audioOptions = tracks.audioOptions()
        subtitleOptions = tracks.textOptions()
        if (audioOptions.isNotEmpty()) {
            selectedAudioIndex = audioOptions.preferredAudioIndex()
            exoPlayer.applyAudioOption(selectedAudioIndex, audioOptions)
            audioLabel = audioOptions[selectedAudioIndex].label
        }

        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            val rem = if (duration > currentPosition) duration - currentPosition else 0L
            if (nextEpisode != null && !autoPlayCancelled && isSeriesLike && !isLive && !autoPlayTriggered && rem <= 0) {
                autoPlayTriggered = true
                onPlayNextEpisode(currentPosition, duration)
            }
            delay(500)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5_000)
            val position = exoPlayer.currentPosition.coerceAtLeast(0L)
            val duration = exoPlayer.duration.coerceAtLeast(0L)
            onSaveProgress(position, duration)
            onReportProgress(position, duration, exoPlayer.isPlaying)
        }
    }

    LaunchedEffect(lastInteraction) {
        delay(5_000)
        if (controlsVisible) controlsVisible = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                controlsVisible = true
                lastInteraction = System.currentTimeMillis()
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                if (sliderFocused && (
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                )) {
                    return@onPreviewKeyEvent false
                }
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE,
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        exoPlayer.playPause()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> {
                        if (!isLive) {
                            exoPlayer.seekBy(10_000)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> {
                        if (!isLive) {
                            exoPlayer.seekBy(-10_000)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        controlsVisible = !controlsVisible
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_X -> {
                        if (audioOptions.isNotEmpty()) {
                            selectedAudioIndex = (selectedAudioIndex + 1) % audioOptions.size
                            exoPlayer.applyAudioOption(selectedAudioIndex, audioOptions)
                            audioLabel = audioOptions[selectedAudioIndex].label
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_Y -> {
                        selectedSubtitleIndex = if (subtitleOptions.isEmpty()) -1 else (selectedSubtitleIndex + 1) % (subtitleOptions.size + 1)
                        exoPlayer.applySubtitleOption(selectedSubtitleIndex, subtitleOptions)
                        subtitleLabel = subtitleOptions.getOrNull(selectedSubtitleIndex)?.label ?: "Off"
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (overlayVisible) {
                            onCancelAutoPlay()
                        } else {
                            onBack()
                        }
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
            update = { playerView ->
                playerView.subtitleView?.let { subtitleView ->
                    subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize)
                    val foregroundColor = when (subtitleColor) {
                        "Yellow" -> AndroidColor.YELLOW
                        "Gray" -> AndroidColor.GRAY
                        else -> AndroidColor.WHITE
                    }
                    val style = CaptionStyleCompat(
                        foregroundColor,
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        AndroidColor.BLACK,
                        Typeface.DEFAULT
                    )
                    subtitleView.setStyle(style)
                    val offsetPx = with(density) { subtitleVerticalOffset.dp.roundToPx() }
                    if (offsetPx >= 0) {
                        subtitleView.setPadding(0, 0, 0, offsetPx)
                    } else {
                        subtitleView.setPadding(0, -offsetPx, 0, 0)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLive && !coverUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.4f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .align(Alignment.Center)
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                title = title,
                isPlaying = isPlaying,
                isLive = isLive,
                currentPosition = currentPosition,
                duration = duration,
                audioLabel = audioLabel,
                subtitleLabel = subtitleLabel,
                onBack = onBack,
                onPlayPause = { exoPlayer.playPause() },
                onSeek = { position -> exoPlayer.seekTo(position) },
                onEnterPip = onEnterPip,
                onCycleAudio = {
                    if (audioOptions.isNotEmpty()) {
                        selectedAudioIndex = (selectedAudioIndex + 1) % audioOptions.size
                        exoPlayer.applyAudioOption(selectedAudioIndex, audioOptions)
                        audioLabel = audioOptions[selectedAudioIndex].label
                    }
                },
                onCycleSubtitle = {
                    selectedSubtitleIndex = if (subtitleOptions.isEmpty()) -1 else (selectedSubtitleIndex + 1) % (subtitleOptions.size + 1)
                    exoPlayer.applySubtitleOption(selectedSubtitleIndex, subtitleOptions)
                    subtitleLabel = subtitleOptions.getOrNull(selectedSubtitleIndex)?.label ?: "Off"
                },
                onSliderFocusChanged = { sliderFocused = it }
            )
        }

        if (torrentBuffering != null && torrentBuffering < 100 && torrentStatus != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Buforowanie Torrenta... $torrentBuffering%",
                    style = AppTypography.titleLarge,
                    color = AppColor.OnSurface
                )
                Text(
                    text = "${torrentStatus.state} | Peers: ${torrentStatus.numPeers} | Seeds: ${torrentStatus.numSeeds}",
                    style = AppTypography.caption,
                    color = AppColor.OnSurface
                )
            }
        }

        if (overlayVisible) {
            NextEpisodeOverlay(
                nextEpisode = nextEpisode,
                countdownSeconds = countdownSeconds,
                onPlayNow = { onPlayNextEpisode(currentPosition, duration) },
                onCancel = onCancelAutoPlay
            )
        }

        if (showLoadingStats) {
            PlayerStatsOverlay(playerStats = playerStats)
        }
    }
}

@Composable
private fun NextEpisodeOverlay(
    nextEpisode: MediaItem,
    countdownSeconds: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(nextEpisode.backdropUrl ?: nextEpisode.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = nextEpisode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(220.dp)
            )

            Text(
                text = nextEpisode.title,
                style = AppTypography.titleLarge
            )

            if (nextEpisode.season != null && nextEpisode.episode != null) {
                Text(
                    text = stringResource(
                        id = R.string.next_episode_season_episode,
                        nextEpisode.season,
                        nextEpisode.episode
                    ),
                    style = AppTypography.body
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = Spacing.md)
            ) {
                CircularProgressIndicator(
                    progress = { countdownSeconds / 15f },
                    modifier = Modifier.size(96.dp),
                    color = AppColor.Accent,
                    trackColor = AppColor.SurfaceVariant
                )
                Text(
                    text = countdownSeconds.toString(),
                    style = AppTypography.headline
                )
            }

            Text(
                text = stringResource(id = R.string.next_episode_title, countdownSeconds),
                style = AppTypography.body
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                TvButton(onClick = onPlayNow) {
                    Text(
                        text = stringResource(id = R.string.next_episode_play_now),
                        color = AppColor.Black,
                        style = AppTypography.button
                    )
                }

                TvTextButton(
                    text = stringResource(id = R.string.next_episode_cancel),
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun PlayerStatsOverlay(
    playerStats: PlayerViewModel.PlayerStats
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            val fps = if (playerStats.frameRate > 0) {
                String.format(Locale.US, "%.1f", playerStats.frameRate)
            } else {
                "--"
            }
            val bitrate = String.format(Locale.US, "%.1f", playerStats.currentBitrateMbps)
            Text(
                text = "Res: ${playerStats.resolution}",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "FPS: $fps",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "Video: ${playerStats.videoCodec}",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "Audio: ${playerStats.audioCodec}",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "Bitrate: $bitrate Mbps",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "Dropped: ${playerStats.droppedFrames}",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
            Text(
                text = "Jank: ${playerStats.jankFrames}",
                style = AppTypography.caption,
                color = AppColor.OnSurface
            )
        }
    }
}

@Composable
internal fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    isLive: Boolean,
    currentPosition: Long,
    duration: Long,
    audioLabel: String,
    subtitleLabel: String,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onEnterPip: () -> Unit,
    onCycleAudio: () -> Unit,
    onCycleSubtitle: () -> Unit,
    onSliderFocusChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLive) Color.Black.copy(alpha = 0.3f) else AppColor.Black.copy(alpha = 0.6f))
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvIconButton(
                onClick = onBack,
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back)
            )
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
                TvIconButton(
                    onClick = onPlayPause,
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = if (isPlaying) R.string.pause else R.string.play)
                )

                TvIconButton(
                    onClick = onEnterPip,
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Picture in picture"
                )

                TvIconButton(
                    onClick = onCycleAudio,
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Audio: $audioLabel"
                )
                if (audioLabel.isNotBlank()) {
                    Text(audioLabel, style = AppTypography.caption, modifier = Modifier.padding(start = Spacing.xs))
                }

                TvIconButton(
                    onClick = onCycleSubtitle,
                    imageVector = Icons.Default.ClosedCaption,
                    contentDescription = "Subtitles: $subtitleLabel"
                )
                if (subtitleLabel.isNotBlank()) {
                    Text(subtitleLabel, style = AppTypography.caption, modifier = Modifier.padding(start = Spacing.xs))
                }

                if (isLive) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Transmisja na żywo",
                        style = AppTypography.caption,
                        color = AppColor.OnSurface
                    )
                } else {
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { fraction ->
                            if (duration > 0) {
                                val newMs = (fraction * duration).toLong().coerceIn(0L, duration)
                                onSeek(newMs)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties { canFocus = true }
                            .focusable()
                            .onFocusChanged { onSliderFocusChanged(it.isFocused) }
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (duration > 0) {
                                            val newMs = (currentPosition + 5_000L).coerceAtMost(duration)
                                            onSeek(newMs)
                                        }
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (duration > 0) {
                                            val newMs = (currentPosition - 5_000L).coerceAtLeast(0L)
                                            onSeek(newMs)
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
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
}

private fun applyPreferredQuality(exoPlayer: ExoPlayer, quality: String) {
    val (maxWidth, maxHeight) = when (quality) {
        "1080p" -> 1920 to 1080
        "720p" -> 1280 to 720
        "480p" -> 854 to 480
        else -> Int.MAX_VALUE to Int.MAX_VALUE
    }
    val params = exoPlayer.trackSelectionParameters as? DefaultTrackSelector.Parameters ?: return
    exoPlayer.trackSelectionParameters = params.buildUpon()
        .setMaxVideoSize(maxWidth, maxHeight)
        .build()
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

private data class TrackOption(
    val group: TrackGroup,
    val index: Int,
    val format: Format,
    val label: String,
    val language: String,
    val isAudioDescription: Boolean
)

private fun Tracks.audioOptions(): List<TrackOption> {
    return groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .flatMap { group ->
            (0 until group.length).mapNotNull { index ->
                val format = group.getTrackFormat(index)
                val label = format.trackLabel()
                val language = format.language?.lowercase() ?: "und"
                val isAd = (format.roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO) != 0
                TrackOption(group.mediaTrackGroup, index, format, label, language, isAd)
            }
        }
}

private fun Tracks.textOptions(): List<TrackOption> {
    return groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMap { group ->
            (0 until group.length).mapNotNull { index ->
                val format = group.getTrackFormat(index)
                val label = format.trackLabel()
                val language = format.language?.lowercase() ?: "und"
                TrackOption(group.mediaTrackGroup, index, format, label, language, isAudioDescription = false)
            }
        }
}

private fun Format.trackLabel(): String {
    val formatLabel = this.label
    if (!formatLabel.isNullOrBlank()) return formatLabel
    val lang = language?.lowercase() ?: "und"
    val base = when (lang) {
        "pl", "pol" -> "Polski"
        "en", "eng", "en-gb", "en-us" -> "English"
        else -> java.util.Locale(lang).getDisplayLanguage(java.util.Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    val role = when {
        (roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO) != 0 -> " (Audiodeskrypcja)"
        (roleFlags and C.ROLE_FLAG_DUB) != 0 -> " (Dubbing)"
        (roleFlags and C.ROLE_FLAG_COMMENTARY) != 0 -> " (Komentarz)"
        (roleFlags and C.ROLE_FLAG_ALTERNATE) != 0 -> " (Lektor)"
        (roleFlags and C.ROLE_FLAG_MAIN) != 0 -> " (Oryginał)"
        else -> ""
    }
    return "$base$role"
}

private fun List<TrackOption>.preferredAudioIndex(): Int {
    val plIndex = indexOfFirst { it.language == "pl" && !it.isAudioDescription }
    if (plIndex != -1) return plIndex
    val nonAdIndex = indexOfFirst { !it.isAudioDescription }
    return if (nonAdIndex != -1) nonAdIndex else 0
}

private fun ExoPlayer.applyAudioOption(index: Int, options: List<TrackOption>) {
    if (options.isEmpty() || index !in options.indices) return
    val selected = options[index]
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setOverrideForType(TrackSelectionOverride(selected.group, listOf(selected.index)))
        .build()
}

private fun ExoPlayer.applySubtitleOption(index: Int, options: List<TrackOption>) {
    val builder = trackSelectionParameters.buildUpon()
    if (index == -1 || options.isEmpty()) {
        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
    } else if (index in options.indices) {
        val selected = options[index]
        builder.setOverrideForType(TrackSelectionOverride(selected.group, listOf(selected.index)))
    }
    trackSelectionParameters = builder.build()
}

private fun ExoPlayer.findSelectedAudioIndex(options: List<TrackOption>): Int {
    if (options.isEmpty()) return -1
    val selected = currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .flatMap { group ->
            (0 until group.length)
                .filter { group.isTrackSelected(it) }
                .map { group to it }
        }
        .firstOrNull() ?: return -1
    return options.indexOfFirst { it.group == selected.first.mediaTrackGroup && it.index == selected.second }
}

private fun ExoPlayer.findSelectedSubtitleIndex(options: List<TrackOption>): Int {
    if (options.isEmpty()) return -1
    val selected = currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMap { group ->
            (0 until group.length)
                .filter { group.isTrackSelected(it) }
                .map { group to it }
        }
        .firstOrNull() ?: return -1
    return options.indexOfFirst { it.group == selected.first.mediaTrackGroup && it.index == selected.second }
}
