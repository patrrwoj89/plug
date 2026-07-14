package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.data.SavedMediaRepository
import com.tvhub.skeleton.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val savedMediaRepository: SavedMediaRepository
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            _item.value = id?.let { mediaRepository.byId(it) }
        }
    }

    val isInLibrary: StateFlow<Boolean> = item
        .flatMapLatest { media ->
            media?.id?.let { savedMediaRepository.isInLibrary(it) } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isInWatchlist: StateFlow<Boolean> = item
        .flatMapLatest { media ->
            media?.id?.let { savedMediaRepository.isInWatchlist(it) } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleLibrary() {
        val current = item.value ?: return
        viewModelScope.launch {
            if (isInLibrary.value) {
                savedMediaRepository.removeFromLibrary(current.id)
            } else {
                savedMediaRepository.addToLibrary(current)
            }
        }
    }

    fun toggleWatchlist() {
        val current = item.value ?: return
        viewModelScope.launch {
            if (isInWatchlist.value) {
                savedMediaRepository.removeFromWatchlist(current.id)
            } else {
                savedMediaRepository.addToWatchlist(current)
            }
        }
    }
}
