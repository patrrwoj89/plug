package com.polishmediahub.app.ui.player


import android.util.Log
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState

import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.screens.NextEpisodeOverlay
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UniversalVlcPlayer(
    mediaItem: MediaItem?,
    videoUrl: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val skipIntroState by viewModel.skipIntroState.collectAsStateWithLifecycle()
    val pendingSeekToMs by viewModel.pendingSeekToMs.collectAsStateWithLifecycle()
    val forceAutoPlayOverlay by viewModel.forceAutoPlayOverlay.collectAsStateWithLifecycle()
    val nextEpisode by viewModel.nextEpisode.collectAsStateWithLifecycle()

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

    DisposableEffect(resolvedItem.id, url) {
        var positionJob: Job? = null
        var progressJob: Job? = null
        var currentState: VlcPlayerState? = null

        try {
            LibVLC.loadLibraries()

            val options = ArrayList<String>().apply {
                add("--vout=android-display")
                add("--aout=opensles")
                add("--audio-time-stretch")
                add("--network-caching=1500")
                add("--file-caching=1500")

                val headers = resolvedItem.headers
                headers.forEach { (key, value) ->
                    add("--http-header-fields=$key: $value")
                }
                if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
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

            mediaPlayer.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        viewModel.setIsPlaying(true)
                        val position = mediaPlayer.getTime()
                        val duration = mediaPlayer.getLength()
                        viewModel.scrobbleStart(position, duration)

                        try {
                            val textTracks = mediaPlayer.getTracks(IMedia.Track.Type.Text)
                            textTracks?.firstOrNull()?.id?.let { trackId ->
                                mediaPlayer.selectTrack(trackId)
                            }
                        } catch (e: Exception) {
                            Log.e("UniversalVlcPlayer", "Błąd runtime silnika LibVLC: ${e.message}", e)
                        }
                    }
                    MediaPlayer.Event.Paused -> {
                        viewModel.setIsPlaying(false)
                        val position = mediaPlayer.getTime()
                        val duration = mediaPlayer.getLength()
                        viewModel.scrobblePause(position, duration)
                    }
                    MediaPlayer.Event.Stopped,
                    MediaPlayer.Event.EndReached -> {
                        viewModel.setIsPlaying(false)
                        val position = mediaPlayer.getTime()
                        val duration = mediaPlayer.getLength()
                        viewModel.scrobbleStop(position, duration)
                        viewModel.reportPlaybackProgress(position, duration, PlaybackState.STOPPED)
                    }
                    MediaPlayer.Event.EncounteredError -> {
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
                    val position = mediaPlayer.getTime().coerceAtLeast(0L)
                    val duration = mediaPlayer.getLength().coerceAtLeast(0L)
                    viewModel.updatePosition(position, duration)
                    delay(POSITION_POLL_MS)
                }
            }

            progressJob = scope.launch {
                while (isActive) {
                    delay(PROGRESS_REPORT_MS)
                    val position = mediaPlayer.getTime().coerceAtLeast(0L)
                    val duration = mediaPlayer.getLength().coerceAtLeast(0L)
                    val state = if (mediaPlayer.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    viewModel.saveProgress(position, duration)
                    viewModel.reportPlaybackProgress(position, duration, state)
                }
            }
        } catch (e: Exception) {
            errorMessageRes = R.string.player_error
            errorMessageArg = e.message
        }

        onDispose {
            positionJob?.cancel()
            progressJob?.cancel()
            try {
                currentState?.mediaPlayer?.let { mp ->
                    val position = mp.getTime().coerceAtLeast(0L)
                    val duration = mp.getLength().coerceAtLeast(0L)
                    viewModel.onPlaybackStopped(position, duration)
                    mp.stop()
                    mp.detachViews()
                    mp.release()
                }
                currentState?.media?.release()
                currentState?.libVlc?.release()
            } catch (e: Exception) {
                Log.e("UniversalVlcPlayer", "Błąd runtime silnika LibVLC: ${e.message}", e)
            }
        }
    }

    val mediaPlayer = playerState?.mediaPlayer

    LaunchedEffect(mediaPlayer, pendingSeekToMs) {
        val seekTo = pendingSeekToMs ?: return@LaunchedEffect
        mediaPlayer?.setTime(seekTo.coerceAtLeast(0L), true)
        viewModel.onSeekHandled()
    }

    BackHandler { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
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
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable(true)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    ) {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            mediaPlayer?.let {
                                if (it.isPlaying) it.pause() else it.play()
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        errorMessageRes?.let { resId ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessageArg ?: stringResource(id = resId),
                    style = AppTypography.titleLarge,
                    color = AppColor.OnSurface
                )
            }
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
            val currentPosition = mediaPlayer?.getTime()?.coerceAtLeast(0L) ?: 0L
            val currentDuration = mediaPlayer?.getLength()?.coerceAtLeast(0L) ?: 0L
            NextEpisodeOverlay(
                nextEpisode = nextEpisode,
                countdownSeconds = 0,
                onPlayNow = { viewModel.playNextEpisode(currentPosition, currentDuration) },
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
