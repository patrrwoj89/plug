package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.TvLauncherManager
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val torrentMediaSource: TorrentMediaSource,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tvLauncherManager: TvLauncherManager,
    private val traktMediaRepository: TraktMediaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    private val _resumePosition = MutableStateFlow(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _resolvedUrl = MutableStateFlow<String?>(null)
    val resolvedUrl: StateFlow<String?> = _resolvedUrl.asStateFlow()

    private val _torrentStatus = MutableStateFlow<com.polishmediahub.app.data.torrent.TorrentStatus?>(null)
    val torrentStatus: StateFlow<com.polishmediahub.app.data.torrent.TorrentStatus?> = _torrentStatus.asStateFlow()

    private val _torrentBuffering = MutableStateFlow<Int?>(null)
    val torrentBuffering: StateFlow<Int?> = _torrentBuffering.asStateFlow()

    private val _preferredQuality = MutableStateFlow("Auto")
    val preferredQuality: StateFlow<String> = _preferredQuality.asStateFlow()

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val mediaId = id ?: return@launch
            _item.value = mediaRepository.byId(mediaId)
            val current = _item.value
            if (current != null) {
                val resolved = mediaRepository.resolveItem(current)
                _item.value = resolved
                _resolvedUrl.value = resolved.videoUrl
                watchHistoryRepository.observePosition(mediaId).collect { position ->
                    _resumePosition.value = position
                }
            }
        }

        viewModelScope.launch {
            _item.filterNotNull().flatMapLatest { current ->
                if (current.id.startsWith("magnet:") || current.id.startsWith("torrent:")) {
                    val infoHash = current.id.substringAfter(":")
                    combine(
                        torrentMediaSource.statusFlow,
                        torrentMediaSource.bufferingProgress
                    ) { statuses, progress ->
                        statuses[infoHash] to progress[infoHash]
                    }
                } else {
                    flowOf(null to null)
                }
            }.collect { (status, progress) ->
                _torrentStatus.value = status
                _torrentBuffering.value = progress
            }
        }

        viewModelScope.launch {
            settingsRepository.preferredQuality.collect { _preferredQuality.value = it }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            tvLauncherManager.updatePlaybackProgress(current, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobblePause(current, progress)
        }
    }

    fun reportPlaybackProgress(positionMs: Long, durationMs: Long, state: PlaybackState) {
        val current = item.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaRepository.reportProgress(current, positionMs, durationMs, state)
            } catch (_: Exception) {
                // Ignore network errors; do not interrupt playback.
            }
        }
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            tvLauncherManager.onPlaybackStopped(current, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStop(current, progress)
            reportPlaybackProgress(positionMs, durationMs, PlaybackState.STOPPED)
        }
    }

    fun scrobbleStart(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStart(current, progress)
            reportPlaybackProgress(positionMs, durationMs, PlaybackState.PLAYING)
        }
    }

    fun scrobbleStop(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStop(current, progress)
            reportPlaybackProgress(positionMs, durationMs, PlaybackState.PAUSED)
        }
    }
}
