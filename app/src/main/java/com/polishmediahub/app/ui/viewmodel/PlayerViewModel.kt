package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.WatchNextHelper
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val watchNextHelper: WatchNextHelper,
    private val traktMediaRepository: TraktMediaRepository
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

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val mediaId = id ?: return@launch
            _item.value = mediaRepository.byId(mediaId)
            val current = _item.value
            if (current != null) {
                _resolvedUrl.value = mediaRepository.resolve(current)
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
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val id = item.value?.id ?: return
        val current = item.value ?: return
        viewModelScope.launch {
            watchHistoryRepository.updatePosition(id, positionMs, durationMs)
            watchNextHelper.addToWatchNext(current, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobblePause(current, progress)
        }
    }

    fun scrobbleStart(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStart(current, progress)
        }
    }

    fun scrobbleStop(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStop(current, progress)
        }
    }
}
