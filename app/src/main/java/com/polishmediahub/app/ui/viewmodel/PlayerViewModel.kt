package com.polishmediahub.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.audio.AudioHistoryRepository
import com.polishmediahub.app.data.audio.AudioRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.TvLauncherManager
import com.polishmediahub.app.model.AudioTrack
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val audioRepository: AudioRepository,
    private val audioHistoryRepository: AudioHistoryRepository,
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

    private val _nextEpisode = MutableStateFlow<MediaItem?>(null)
    val nextEpisode: StateFlow<MediaItem?> = _nextEpisode.asStateFlow()

    private val _autoPlayCancelled = MutableStateFlow(false)
    val autoPlayCancelled: StateFlow<Boolean> = _autoPlayCancelled.asStateFlow()

    private var currentAudioTrack: AudioTrack? = null

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val mediaId = id ?: return@launch

            val mediaItem = loadMediaItem(mediaId)
            _item.value = mediaItem

            if (mediaItem != null && isAudioItem(mediaItem)) {
                val audioTrack = currentAudioTrack ?: audioRepository.byId(mediaId)
                if (audioTrack != null) {
                    val resolvedStream = audioRepository.resolve(audioTrack)
                    if (!resolvedStream.isNullOrBlank()) {
                        _resolvedUrl.value = resolvedStream
                        currentAudioTrack = audioTrack.copy(streamUrl = resolvedStream)
                        _item.value = currentAudioTrack!!.toMediaItem()
                    } else {
                        _resolvedUrl.value = audioTrack.streamUrl
                    }
                    if (mediaItem.isLive.not()) {
                        _resumePosition.value = audioHistoryRepository.getPosition(mediaId)
                    }
                }
            } else if (mediaItem != null) {
                val resolved = mediaRepository.resolveItem(mediaItem)
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
                    combine(
                        torrentMediaSource.statusFlow,
                        torrentMediaSource.bufferingProgress
                    ) { statuses, progress ->
                        statuses[infoHashFrom(current.id)] to progress[infoHashFrom(current.id)]
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

        viewModelScope.launch {
            _item.filterNotNull().collect { current ->
                if (isSeriesLike(current)) {
                    loadNextEpisode(current)
                } else {
                    _nextEpisode.value = null
                }
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            savePlaybackProgress(current, positionMs, durationMs)
        }
    }

    fun reportPlaybackProgress(positionMs: Long, durationMs: Long, state: PlaybackState) {
        val current = item.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reportProgress(current, positionMs, durationMs, state)
            } catch (_: Exception) {
                // Ignore network errors; do not interrupt playback.
            }
        }
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            finishPlayback(current, positionMs, durationMs)
        }
    }

    fun cancelAutoPlay() {
        _autoPlayCancelled.value = true
    }

    fun resetAutoPlayCancel() {
        _autoPlayCancelled.value = false
    }

    fun playNextEpisode(previousPositionMs: Long, previousDurationMs: Long) {
        val next = _nextEpisode.value ?: return
        val previous = _item.value ?: return
        viewModelScope.launch {
            try {
                finishPlayback(previous, previousPositionMs, previousDurationMs)
                val resolved = mediaRepository.resolveItem(next)
                _item.value = resolved
                _resolvedUrl.value = resolved.videoUrl
                _resumePosition.value = 0L
                _nextEpisode.value = null
                _autoPlayCancelled.value = false
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "playNextEpisode failed: ${e.message}")
            }
        }
    }

    private suspend fun loadNextEpisode(current: MediaItem) {
        _nextEpisode.value = null
        try {
            val next = findNextEpisode(current)
            _nextEpisode.value = next
        } catch (e: Exception) {
            Log.w("PlayerViewModel", "loadNextEpisode failed: ${e.message}")
        }
    }

    private suspend fun findNextEpisode(current: MediaItem): MediaItem? {
        val query = buildSearchQuery(current)
        val results = try {
            mediaRepository.search(query)
        } catch (_: Exception) {
            emptyList()
        }

        val currentSeason = current.season ?: 1
        val currentEpisode = current.episode ?: if (current.type == MediaItem.Type.SERIES) 0 else 1
        val nextEpisodeNumber = currentEpisode + 1

        return results
            .filter { it.id != current.id }
            .filter {
                it.type == MediaItem.Type.EPISODE ||
                    it.type == MediaItem.Type.SERIES ||
                    it.title.contains(current.title, ignoreCase = true)
            }
            .firstOrNull { it.season == currentSeason && it.episode == nextEpisodeNumber }
            ?: results.firstOrNull {
                it.id != current.id &&
                    it.title.contains(current.title, ignoreCase = true) &&
                    it.episode == nextEpisodeNumber
            }
    }

    private fun buildSearchQuery(current: MediaItem): String {
        val base = current.title
            .replace(Regex("\\s+S\\d+E\\d+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+Season\\s+\\d+", RegexOption.IGNORE_CASE), "")
            .trim()
        return base.ifBlank { current.title }
    }

    private fun isSeriesLike(mediaItem: MediaItem): Boolean {
        return mediaItem.type == MediaItem.Type.SERIES || mediaItem.type == MediaItem.Type.EPISODE
    }

    fun scrobbleStart(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            if (!isAudioItem(current)) {
                val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
                traktMediaRepository.scrobbleStart(current, progress)
            }
            reportProgress(current, positionMs, durationMs, PlaybackState.PLAYING)
        }
    }

    fun scrobbleStop(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            if (!isAudioItem(current)) {
                val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
                traktMediaRepository.scrobbleStop(current, progress)
            }
            reportProgress(current, positionMs, durationMs, PlaybackState.PAUSED)
        }
    }

    private suspend fun finishPlayback(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        if (isAudioItem(mediaItem)) {
            currentAudioTrack?.let { track ->
                audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
            }
        } else {
            tvLauncherManager.onPlaybackStopped(mediaItem, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStop(mediaItem, progress)
            reportProgress(mediaItem, positionMs, durationMs, PlaybackState.STOPPED)
        }
    }

    private suspend fun savePlaybackProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        if (isAudioItem(mediaItem)) {
            currentAudioTrack?.let { track ->
                audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
            }
        } else {
            tvLauncherManager.updatePlaybackProgress(mediaItem, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobblePause(mediaItem, progress)
        }
    }

    private suspend fun reportProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long, state: PlaybackState) {
        try {
            if (isAudioItem(mediaItem)) {
                if (state == PlaybackState.PLAYING || state == PlaybackState.PAUSED) {
                    currentAudioTrack?.let { track ->
                        audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
                    }
                }
            } else {
                mediaRepository.reportProgress(mediaItem, positionMs, durationMs, state)
            }
        } catch (_: Exception) {
            // Ignore network errors.
        }
    }

    private suspend fun loadMediaItem(mediaId: String): MediaItem? {
        val mediaItem = mediaRepository.byId(mediaId)
        if (mediaItem != null) return mediaItem

        val track = audioRepository.byId(mediaId)
        if (track != null) {
            currentAudioTrack = track
            return track.toMediaItem()
        }
        return null
    }

    private fun isAudioItem(mediaItem: MediaItem): Boolean {
        return mediaItem.type == MediaItem.Type.AUDIO ||
            mediaItem.id.startsWith("deezer:track:") ||
            mediaItem.id.startsWith("podcast:") ||
            mediaItem.id.startsWith("radio:") ||
            mediaItem.id.startsWith("local_audio:") ||
            mediaItem.id.startsWith("subsonic:")
    }

    private fun infoHashFrom(id: String): String = id.substringAfter(":")

    private fun AudioTrack.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        subtitle = artist,
        description = description,
        posterUrl = coverUrl,
        backdropUrl = coverUrl,
        duration = if (durationMs > 0) durationMs.toString() else "",
        videoUrl = streamUrl,
        isLive = isLive,
        type = MediaItem.Type.AUDIO
    )
}
