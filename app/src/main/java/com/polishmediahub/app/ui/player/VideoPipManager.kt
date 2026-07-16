package com.polishmediahub.app.ui.player

import androidx.media3.exoplayer.ExoPlayer
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the active ExoPlayer instance so that video can continue playing
 * when the user leaves [PlayerScreen] and enters the in-app mini-player on
 * [HomeScreen]. The manager is application-scoped so it survives navigation.
 */
@Singleton
class VideoPipManager @Inject constructor() {

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _exoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val exoPlayer: StateFlow<ExoPlayer?> = _exoPlayer.asStateFlow()

    private var enterPipOnDestroy = false

    /**
     * Call from [PlayerScreen] after it builds (or reuses) an ExoPlayer.
     */
    fun setPlayer(player: ExoPlayer?, mediaItem: MediaItem?) {
        _exoPlayer.value = player
        _currentMediaItem.value = mediaItem
    }

    /**
     * Call from the BACK handler in [PlayerScreen] to keep the player alive.
     */
    fun requestEnterPip() {
        enterPipOnDestroy = true
    }

    /**
     * Returns whether the current dispose cycle should keep the player alive.
     */
    fun consumePipRequest(): Boolean {
        val should = enterPipOnDestroy
        enterPipOnDestroy = false
        return should
    }

    /**
     * The mini-player calls this once it is visible.
     */
    fun enterPip() {
        _isInPipMode.value = true
        _exoPlayer.value?.playWhenReady = true
    }

    /**
     * The full-screen player calls this when it regains focus.
     */
    fun exitPip() {
        _isInPipMode.value = false
    }

    /**
     * Stop playback and free the ExoPlayer. Call when the mini-player is closed
     * or the activity is destroyed.
     */
    fun stopAndRelease() {
        _isInPipMode.value = false
        _currentMediaItem.value = null
        _exoPlayer.value?.let { player ->
            try {
                player.stop()
                player.release()
            } catch (_: Exception) {
            }
        }
        _exoPlayer.value = null
    }
}
