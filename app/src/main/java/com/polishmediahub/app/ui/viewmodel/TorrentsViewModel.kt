package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.torrent.TorrentStatus
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentsViewModel @Inject constructor(
    private val torrentMediaSource: TorrentMediaSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorrentsUiState())
    val uiState: StateFlow<TorrentsUiState> = _uiState.asStateFlow()

    fun addMagnet(magnetUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val hash = torrentMediaSource.addMagnet(magnetUri)
                if (hash.isBlank()) {
                    _uiState.value = _uiState.value.copy(error = "Could not add magnet", isLoading = false)
                } else {
                    observeTorrent(hash)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun observeTorrent(hash: String) {
        torrentMediaSource.observeStatus(hash) { status ->
            _uiState.value = _uiState.value.copy(
                torrents = _uiState.value.torrents.filter { it.id != status.infoHash } + status.toMediaItem()
            )
        }
    }

    private fun TorrentStatus.toMediaItem() = MediaItem(
        id = infoHash,
        title = name.ifBlank { infoHash },
        type = MediaItem.Type.MOVIE
    )
}

data class TorrentsUiState(
    val torrents: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
