package com.polishmediahub.app.ui.player

import android.app.Activity
import android.util.Log
import com.polishmediahub.app.BuildConfig
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import com.polishmediahub.app.ui.components.PlayerQuickSettingsOverlay
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.screens.NextEpisodeOverlay
import com.polishmediahub.app.ui.screens.PlayerControls
import com.polishmediahub.app.ui.screens.PlayerStatsOverlay
import com.polishmediahub.app.ui.screens.enterPipMode
import com.polishmediahub.app.ui.screens.findActivity
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
import com.polishmediahub.app.data.source.FrameSample
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout

private const val POSITION_POLL_MS = 500L
private const val PROGRESS_REPORT_MS = 5_000L
private const val STATS_POLL_MS = 1_000L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UniversalVlcPlayer(
    mediaItem: MediaItem?,
    videoUrl: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    preferredAudioType: String = "lector",
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val activity = context.findActivity()

    val skipIntroState by viewModel.skipIntroState.collectAsStateWithLifecycle()
    val pendingSeekToMs by viewModel.pendingSeekToMs.collectAsStateWithLifecycle()
    val forceAutoPlayOverlay by viewModel.forceAutoPlayOverlay.collectAsStateWithLifecycle()
    val nextEpisode by viewModel.nextEpisode.collectAsStateWithLifecycle()
    val autoPlayCancelled by viewModel.autoPlayCancelled.collectAsStateWithLifecycle()
    val showLoadingStats by viewModel.showLoadingStats.collectAsStateWithLifecycle()
    val cinemaMode by viewModel.cinemaMode.collectAsStateWithLifecycle()
    val cinemaInfo by viewModel.cinemaInfo.collectAsStateWithLifecycle()
    val isInPipMode by viewModel.isInPipMode.collectAsStateWithLifecycle()
    val subtitleSize by viewModel.subtitleSize.collectAsStateWithLifecycle()
    val subtitleColor by viewModel.subtitleColor.collectAsStateWithLifecycle()
    val subtitleVerticalOffset by viewModel.subtitleVerticalOffset.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()

    val resolvedItem = mediaItem ?: return
    val url = videoUrl ?: resolvedItem.videoUrl

    if (url.isNullOrBlank()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.player_error),
                style = AppTypography.titleLarge,
                color = AppColor.OnSurface
            )
        }
        return
    }

    var playerState by remember { mutableStateOf<VlcPlayerState?>(null) }
    var errorMessageRes by remember { mutableStateOf<Int?>(null) }
    var errorMessageArg by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(!isInPipMode) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var audioOptions by remember { mutableStateOf<List<VlcTrackOption>>(emptyList()) }
    var selectedAudioIndex by remember { mutableIntStateOf(-1) }
    var audioLabel by remember { mutableStateOf("") }
    var subtitleOptions by remember { mutableStateOf<List<VlcTrackOption>>(emptyList()) }
    var selectedSubtitleIndex by remember { mutableIntStateOf(-1) }
    var subtitleLabel by remember { mutableStateOf("") }
    var sliderFocused by remember { mutableStateOf(false) }
    var quickSettingsVisible by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoPlayTriggered by remember(resolvedItem.id) { mutableStateOf(false) }
    val isLive = resolvedItem.isLive
    val isSeriesLike = resolvedItem.type == MediaItem.Type.SERIES || resolvedItem.type == MediaItem.Type.EPISODE
    val sliderFocusRequester = remember { FocusRequester() }
    val quickSettingsNightMode by viewModel.nightModeEnabled.collectAsStateWithLifecycle()
    val quickSettingsAudioType by viewModel.preferredAudioType.collectAsStateWithLifecycle()
    val quickSettingsAlternative by viewModel.useAlternativePlayer.collectAsStateWithLifecycle()

    val currentPreferredAudioType by rememberUpdatedState(preferredAudioType)

    val remainingMs = if (duration > currentPosition) duration - currentPosition else 0L
    val overlayVisible = (
        nextEpisode != null && !autoPlayCancelled && isSeriesLike && !isLive &&
            remainingMs in 0..15_000L
        ) || forceAutoPlayOverlay
    val countdownSeconds = (remainingMs / 1000).toInt().coerceIn(0, 15)

    val dimAlpha by animateFloatAsState(
        targetValue = if (isPlaying && cinemaMode && !controlsVisible && !isInPipMode) 0.45f else 0f,
        label = "vlc_dim_alpha"
    )

    DisposableEffect(resolvedItem.id, url) {
        var positionJob: Job? = null
        var progressJob: Job? = null
        var statsJob: Job? = null
        var currentState: VlcPlayerState? = null

        try {
            LibVLC.loadLibraries()

            val subSize = subtitleSize
            val subColor = subtitleColor
            val subOffset = subtitleVerticalOffset

            val options = ArrayList<String>().apply {
                add("--vout=android-display")
                add("--aout=opensles")
                add("--audio-time-stretch")
                add("--network-caching=1500")
                add("--file-caching=1500")
                add("--sub-text-scale=${subtitleTextScale(subSize)}")
                add("--freetype-color=${subtitleColorToVlc(subColor)}")
                add("--freetype-opacity=255")
                val offsetPx = (subOffset * density).toInt().coerceAtLeast(0)
                if (offsetPx > 0) add("--sub-margin=$offsetPx")

                resolvedItem.headers.forEach { (key, value) ->
                    add("--http-header-fields=$key: $value")
                }
                if (!resolvedItem.headers.containsKey("User-Agent") &&
                    !resolvedItem.headers.containsKey("user-agent")
                ) {
                    add("--http-header-fields=User-Agent: PolishMediaHub/1.0 (Android TV)")
                }
            }

            val libVlc = LibVLC(context, options)
            val mediaPlayer = MediaPlayer(libVlc)
            val media = Media(libVlc, url)

            resolvedItem.subtitleUrl?.let { subtitleUrl ->
                resolvedItem.subtitleHeaders.forEach { (key, value) ->
                    media.addOption(":http-header-fields=$key: $value")
                }
                media.addSlave(
                    IMedia.Slave(
                        IMedia.Slave.Type.Subtitle,
                        4,
                        subtitleUrl
                    )
                )
            }

            var tracksInitialized = false

            mediaPlayer.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                        viewModel.setIsPlaying(true)
                        val position = mediaPlayer.time
                        val length = mediaPlayer.length
                        currentPosition = position.coerceAtLeast(0L)
                        duration = length.coerceAtLeast(0L)
                        viewModel.scrobbleStart(position, length)

                        try {
                            if (!tracksInitialized) {
                                audioOptions = parseVlcTracks(
                                    mediaPlayer.getTracks(IMedia.Track.Type.Audio)
                                )
                                if (audioOptions.isNotEmpty()) {
                                    selectedAudioIndex = preferredAudioIndex(audioOptions, currentPreferredAudioType)
                                    mediaPlayer.selectTrack(audioOptions[selectedAudioIndex].id)
                                    audioLabel = audioOptions[selectedAudioIndex].label
                                }

                                subtitleOptions = parseVlcTracks(
                                    mediaPlayer.getTracks(IMedia.Track.Type.Text)
                                )
                                if (subtitleOptions.isNotEmpty()) {
                                    selectedSubtitleIndex = preferredSubtitleIndex(subtitleOptions)
                                    mediaPlayer.selectTrack(subtitleOptions[selectedSubtitleIndex].id)
                                    subtitleLabel = subtitleOptions[selectedSubtitleIndex].label
                                }
                                tracksInitialized = true
                            } else {
                                val selectedAudio = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Audio)
                                selectedAudioIndex = audioOptions.indexOfFirst { it.id == selectedAudio?.id }
                                audioLabel = audioOptions.getOrNull(selectedAudioIndex)?.label ?: ""
                                val selectedSub = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Text)
                                selectedSubtitleIndex = subtitleOptions.indexOfFirst { it.id == selectedSub?.id }
                                subtitleLabel = subtitleOptions.getOrNull(selectedSubtitleIndex)?.label ?: "Off"
                            }
                        } catch (e: Exception) {
                            if (BuildConfig.DEBUG) Log.e("UniversalVlcPlayer", "Błąd runtime silnika LibVLC: ${e.message}", e)
                        }
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                        viewModel.setIsPlaying(false)
                        val position = mediaPlayer.time
                        val length = mediaPlayer.length
                        viewModel.scrobblePause(position, length)
                        controlsVisible = true
                    }
                    MediaPlayer.Event.Stopped,
                    MediaPlayer.Event.EndReached -> {
                        isPlaying = false
                        viewModel.setIsPlaying(false)
                        val position = mediaPlayer.time
                        val length = mediaPlayer.length
                        viewModel.scrobbleStop(position, length)
                        viewModel.reportPlaybackProgress(position, length, PlaybackState.STOPPED)
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        isPlaying = false
                        viewModel.setIsPlaying(false)
                        errorMessageRes = R.string.player_error
                        errorMessageArg = null
                    }
                    else -> {}
                }
            }

            currentState = VlcPlayerState(libVlc, mediaPlayer, media)
            playerState = currentState

            positionJob = scope.launch {
                while (isActive) {
                    val position = mediaPlayer.time.coerceAtLeast(0L)
                    val length = mediaPlayer.length.coerceAtLeast(0L)
                    currentPosition = position
                    duration = length
                    viewModel.onFrameSample(FrameSample(position, 1f, 0f), position, length)
                    viewModel.updatePosition(position, length)

                    val rem = if (length > position) length - position else 0L
                    if (
                        viewModel.nextEpisode.value != null &&
                        !viewModel.autoPlayCancelled.value &&
                        isSeriesLike && !isLive &&
                        !autoPlayTriggered && rem <= 0
                    ) {
                        autoPlayTriggered = true
                        viewModel.playNextEpisode(position, length)
                    }

                    delay(POSITION_POLL_MS)
                }
            }

            progressJob = scope.launch {
                while (isActive) {
                    delay(PROGRESS_REPORT_MS)
                    val position = mediaPlayer.time.coerceAtLeast(0L)
                    val length = mediaPlayer.length.coerceAtLeast(0L)
                    val state = if (mediaPlayer.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    viewModel.saveProgress(position, length)
                    viewModel.reportPlaybackProgress(position, length, state)
                }
            }

            statsJob = scope.launch {
                while (isActive) {
                    delay(STATS_POLL_MS)
                    try {
                        val videoTrack = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Video) as? IMedia.VideoTrack
                        val audioTrack = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Audio) as? IMedia.AudioTrack
                        val resolution = if (videoTrack != null && videoTrack.width > 0 && videoTrack.height > 0) {
                            "${videoTrack.width}x${videoTrack.height}"
                        } else "--"
                        val frameRate = if (videoTrack != null && videoTrack.frameRateDen > 0) {
                            videoTrack.frameRateNum / videoTrack.frameRateDen.toFloat()
                        } else 0f
                        val bitrate = ((videoTrack?.bitrate ?: 0) + (audioTrack?.bitrate ?: 0)).toFloat()
                        viewModel.updatePlayerStats(
                            PlayerViewModel.PlayerStats(
                                resolution = resolution,
                                frameRate = frameRate,
                                videoCodec = videoTrack?.codec ?: videoTrack?.originalCodec ?: "--",
                                audioCodec = audioTrack?.codec ?: audioTrack?.originalCodec ?: "--",
                                currentBitrateMbps = bitrate / 1_000_000f,
                                droppedFrames = 0,
                                jankFrames = 0
                            )
                        )
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.w("UniversalVlcPlayer", "Stats update failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            errorMessageRes = R.string.player_error
            errorMessageArg = e.message
            if (BuildConfig.DEBUG) Log.e("UniversalVlcPlayer", "Błąd inicjalizacji LibVLC: ${e.message}", e)
        }

        onDispose {
            positionJob?.cancel()
            progressJob?.cancel()
            statsJob?.cancel()
            try {
                currentState?.mediaPlayer?.let { mp ->
                    val position = mp.time.coerceAtLeast(0L)
                    val length = mp.length.coerceAtLeast(0L)
                    viewModel.onPlaybackStopped(position, length)
                    mp.stop()
                    mp.detachViews()
                    mp.release()
                }
                currentState?.media?.release()
                currentState?.libVlc?.release()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("UniversalVlcPlayer", "Błąd runtime silnika LibVLC: ${e.message}", e)
            }
        }
    }

    val mediaPlayer = playerState?.mediaPlayer

    LaunchedEffect(mediaPlayer, pendingSeekToMs) {
        val seekTo = pendingSeekToMs ?: return@LaunchedEffect
        mediaPlayer?.setTime(seekTo.coerceAtLeast(0L), true)
        viewModel.onSeekHandled()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isInPipMode) {
        controlsVisible = !isInPipMode
    }

    LaunchedEffect(lastInteraction, isPlaying, cinemaMode) {
        delay(5_000)
        if (controlsVisible && isPlaying && cinemaMode) controlsVisible = false
    }

    LaunchedEffect(currentPreferredAudioType, mediaPlayer) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        if (audioOptions.isNotEmpty()) {
            val index = preferredAudioIndex(audioOptions, currentPreferredAudioType)
            if (index != selectedAudioIndex && index in audioOptions.indices) {
                selectedAudioIndex = index
                mp.selectTrack(audioOptions[index].id)
                audioLabel = audioOptions[index].label
            }
        }
    }

    BackHandler {
        if (quickSettingsVisible) {
            quickSettingsVisible = false
            sliderFocusRequester.requestFocus()
        } else if (overlayVisible) {
            viewModel.cancelAutoPlay()
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable(true)
            .onPreviewKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                controlsVisible = true
                lastInteraction = System.currentTimeMillis()
                if (native.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                if (quickSettingsVisible) {
                    when (native.keyCode) {
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            quickSettingsVisible = false
                            sliderFocusRequester.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                if (sliderFocused && (
                        native.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                            native.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    )
                ) {
                    return@onPreviewKeyEvent false
                }
                when (native.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_SPACE,
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        mediaPlayer?.let { mp ->
                            if (mp.isPlaying) mp.pause() else mp.play()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_BUTTON_R1 -> {
                        if (!isLive) {
                            val newMs = (currentPosition + 10_000L).coerceAtMost(duration)
                            mediaPlayer?.setTime(newMs, true)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_BUTTON_L1 -> {
                        if (!isLive) {
                            val newMs = (currentPosition - 10_000L).coerceAtLeast(0L)
                            mediaPlayer?.setTime(newMs, true)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        controlsVisible = !controlsVisible
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_X -> {
                        if (audioOptions.isNotEmpty()) {
                            selectedAudioIndex = (selectedAudioIndex + 1) % audioOptions.size
                            mediaPlayer?.selectTrack(audioOptions[selectedAudioIndex].id)
                            audioLabel = audioOptions[selectedAudioIndex].label
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BUTTON_Y -> {
                        if (subtitleOptions.isEmpty()) {
                            selectedSubtitleIndex = -1
                            subtitleLabel = "Off"
                        } else {
                            selectedSubtitleIndex = if (selectedSubtitleIndex == -1) {
                                0
                            } else {
                                (selectedSubtitleIndex + 1) % (subtitleOptions.size + 1)
                            }
                            if (selectedSubtitleIndex == subtitleOptions.size) {
                                selectedSubtitleIndex = -1
                                subtitleLabel = "Off"
                                mediaPlayer?.unselectTrackType(IMedia.Track.Type.Text)
                            } else {
                                mediaPlayer?.selectTrack(subtitleOptions[selectedSubtitleIndex].id)
                                subtitleLabel = subtitleOptions[selectedSubtitleIndex].label
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (overlayVisible) {
                            viewModel.cancelAutoPlay()
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
                VLCVideoLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { layout ->
                playerState?.let { state ->
                    if (state.mediaPlayer != null && !state.viewsAttached) {
                        state.mediaPlayer.attachViews(layout, null, false, false)
                        state.mediaPlayer.setMedia(state.media)
                        state.mediaPlayer.play()
                        state.viewsAttached = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
        )

        errorMessageRes?.let { resId ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessageArg ?: stringResource(id = resId),
                    style = AppTypography.titleLarge,
                    color = AppColor.OnSurface
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                title = resolvedItem.title,
                isPlaying = isPlaying,
                isLive = isLive,
                currentPosition = currentPosition,
                duration = duration,
                audioLabel = audioLabel,
                subtitleLabel = subtitleLabel,
                onBack = onBack,
                onPlayPause = {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) mp.pause() else mp.play()
                        Unit
                    }
                },
                onSeek = { newMs ->
                    mediaPlayer?.setTime(newMs.coerceIn(0L, duration), true)
                    Unit
                },
                onEnterPip = { activity?.enterPipMode() },
                onCycleAudio = {
                    if (audioOptions.isNotEmpty()) {
                        selectedAudioIndex = (selectedAudioIndex + 1) % audioOptions.size
                        mediaPlayer?.selectTrack(audioOptions[selectedAudioIndex].id)
                        audioLabel = audioOptions[selectedAudioIndex].label
                    }
                },
                onCycleSubtitle = {
                    if (subtitleOptions.isEmpty()) {
                        selectedSubtitleIndex = -1
                        subtitleLabel = "Off"
                    } else {
                        selectedSubtitleIndex = if (selectedSubtitleIndex == -1) {
                            0
                        } else {
                            (selectedSubtitleIndex + 1) % (subtitleOptions.size + 1)
                        }
                        if (selectedSubtitleIndex == subtitleOptions.size) {
                            selectedSubtitleIndex = -1
                            subtitleLabel = "Off"
                            mediaPlayer?.unselectTrackType(IMedia.Track.Type.Text)
                        } else {
                            mediaPlayer?.selectTrack(subtitleOptions[selectedSubtitleIndex].id)
                            subtitleLabel = subtitleOptions[selectedSubtitleIndex].label
                        }
                    }
                },
                onOpenQuickSettings = { quickSettingsVisible = true },
                onSliderFocusChanged = { sliderFocused = it },
                sliderFocusRequester = sliderFocusRequester,
                cinemaMode = cinemaMode,
                cinemaInfo = cinemaInfo
            )
        }

        PlayerQuickSettingsOverlay(
            visible = quickSettingsVisible && !isInPipMode,
            nightModeEnabled = quickSettingsNightMode,
            preferredAudioType = quickSettingsAudioType,
            useAlternativePlayer = quickSettingsAlternative,
            onToggleNightMode = viewModel::toggleNightModeEnabled,
            onCycleAudioType = viewModel::cyclePreferredAudioType,
            onTogglePlayerEngine = viewModel::toggleUseAlternativePlayer,
            onDismiss = {
                quickSettingsVisible = false
                sliderFocusRequester.requestFocus()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showLoadingStats && !isInPipMode) {
            PlayerStatsOverlay(playerStats = playerStats)
        }

        val showSkipButton =
            (skipIntroState.showSkipIntro || skipIntroState.showSkipOutro) &&
                !(forceAutoPlayOverlay && nextEpisode != null)

        AnimatedVisibility(
            visible = showSkipButton,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            val skipLabel = when {
                skipIntroState.showSkipOutro -> stringResource(id = R.string.skip_outro)
                else -> stringResource(id = R.string.skip_intro)
            }
            val onSkip = when {
                skipIntroState.showSkipOutro -> { { viewModel.onSkipOutro() } }
                else -> { { viewModel.onSkipIntro() } }
            }
            TvButton(
                onClick = onSkip,
                modifier = Modifier.padding(Spacing.md)
            ) {
                Text(
                    text = skipLabel,
                    style = AppTypography.button,
                    color = AppColor.Black
                )
            }
        }

        if (forceAutoPlayOverlay && nextEpisode != null) {
            NextEpisodeOverlay(
                nextEpisode = nextEpisode,
                countdownSeconds = countdownSeconds,
                onPlayNow = { viewModel.playNextEpisode(currentPosition, duration) },
                onCancel = { viewModel.cancelAutoPlay() }
            )
        }
    }
}

private data class VlcPlayerState(
    val libVlc: LibVLC?,
    val mediaPlayer: MediaPlayer?,
    val media: Media?,
    var viewsAttached: Boolean = false
)

private data class VlcTrackOption(
    val id: String,
    val label: String,
    val language: String,
    val isAudioDescription: Boolean,
    val isDubbing: Boolean,
    val isLektor: Boolean,
    val channelCount: Int
)

private fun parseVlcTracks(tracks: Array<IMedia.Track>?): List<VlcTrackOption> {
    if (tracks == null) return emptyList()
    return tracks.map { track ->
        val language = track.language?.lowercase() ?: "und"
        val description = track.description.orEmpty()
        val isAd = isAudioDescriptionTrack(track)
        val channelCount = (track as? IMedia.AudioTrack)?.channels ?: 0
        val isDub = track.isDubbingTrack(channelCount)
        val isLek = track.isLektorTrack(channelCount)
        val baseLabel = when {
            track.name?.isNotBlank() == true -> track.name
            description.isNotBlank() -> description
            language != "und" -> language.uppercase()
            else -> "Track ${track.id}"
        }
        val roleSuffix = when {
            isAd -> " (Audiodeskrypcja)"
            isDub -> " (Dubbing)"
            isLek -> " (Lektor)"
            else -> ""
        }
        VlcTrackOption(
            id = track.id,
            label = "$baseLabel$roleSuffix",
            language = language,
            isAudioDescription = isAd,
            isDubbing = isDub,
            isLektor = isLek,
            channelCount = channelCount
        )
    }
}

private fun isAudioDescriptionTrack(track: IMedia.Track): Boolean {
    val language = track.language?.lowercase().orEmpty()
    val description = track.description?.lowercase().orEmpty()
    if (language == "qad" || language.startsWith("qad")) return true
    if (description.contains("audiodeskrypcja", ignoreCase = true) ||
        description.contains("audio description", ignoreCase = true) ||
        description.contains("visually impaired", ignoreCase = true) ||
        description.contains("opis dla niewidomych", ignoreCase = true)
    ) {
        return true
    }
    if (Regex("\\b(ad|description)\\b", RegexOption.IGNORE_CASE).containsMatchIn(description)) {
        return true
    }
    return false
}

private fun IMedia.Track.isDubbingTrack(channelCount: Int): Boolean {
    val text = ((description ?: "") + " " + (name ?: "")).lowercase()
    return text.contains("dubbing") ||
        text.contains("dub") ||
        text.contains("5.1") ||
        text.contains("6ch") ||
        text.contains("e-ac3") ||
        text.contains("eac3") ||
        text.contains("dts") ||
        channelCount >= 4
}

private fun IMedia.Track.isLektorTrack(channelCount: Int): Boolean {
    val text = ((description ?: "") + " " + (name ?: "")).lowercase()
    return text.contains("lektor") ||
        text.contains("lector") ||
        (channelCount in 1..2 && !isDubbingTrack(channelCount))
}

private fun preferredAudioIndex(options: List<VlcTrackOption>, preferredAudioType: String): Int {
    val nonAd = options.filter { !it.isAudioDescription }
    val pl = nonAd.filter { it.language in listOf("pl", "pol") }
    val candidates = if (pl.isNotEmpty()) pl else nonAd
    val preferred = when {
        preferredAudioType.contains("dub", ignoreCase = true) -> candidates.filter { it.isDubbing }
        preferredAudioType.contains("lektor", ignoreCase = true) ||
            preferredAudioType.contains("lector", ignoreCase = true) -> candidates.filter { it.isLektor }
        else -> candidates
    }
    val chosen = preferred.firstOrNull() ?: candidates.firstOrNull()
    if (chosen != null) return options.indexOf(chosen)
    return if (options.isNotEmpty()) 0 else -1
}

private fun preferredSubtitleIndex(options: List<VlcTrackOption>): Int {
    val plIndex = options.indexOfFirst { it.language in listOf("pl", "pol") }
    return if (plIndex != -1) plIndex else 0
}

private fun subtitleTextScale(sizeSp: Float): Int {
    val base = 14f
    val scale = ((sizeSp / base) * 100f).toInt()
    return scale.coerceIn(10, 500)
}

private fun subtitleColorToVlc(color: String): Int {
    return when (color.lowercase()) {
        "yellow" -> 0xFFFF00
        "gray", "grey" -> 0x808080
        "white" -> 0xFFFFFF
        "black" -> 0x000000
        "red" -> 0xFF0000
        "green" -> 0x00FF00
        "blue" -> 0x0000FF
        else -> 0xFFFFFF
    }
}


