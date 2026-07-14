package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.data.WatchHistoryRepository
import com.tvhub.skeleton.data.tv.WatchNextHelper
import com.tvhub.skeleton.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val watchNextHelper: WatchNextHelper
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    private val _resumePosition = MutableStateFlow(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val mediaId = id ?: return@launch
            _item.value = mediaRepository.byId(mediaId)
            watchHistoryRepository.observePosition(mediaId).collect { position ->
                _resumePosition.value = position
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val id = item.value?.id ?: return
        viewModelScope.launch {
            watchHistoryRepository.updatePosition(id, positionMs, durationMs)
            item.value?.let { watchNextHelper.addToWatchNext(it, positionMs, durationMs) }
        }
    }
}
