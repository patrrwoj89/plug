package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SavedMediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.source.FederatedMediaRepository
import kotlinx.coroutines.Dispatchers
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val savedMediaRepository: SavedMediaRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val federatedMediaRepository: FederatedMediaRepository
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val baseItem = id?.let { mediaRepository.byId(it) }
            _item.value = baseItem
            baseItem?.let {
                launch(Dispatchers.IO) {
                    val enriched = federatedMediaRepository.enrichWithFilmweb(it)
                    if (enriched != it) {
                        _item.value = enriched
                    }
                }
            }
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

    val spoilerBlurEnabled: StateFlow<Boolean> = settingsRepository.spoilerBlurEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isWatched: StateFlow<Boolean> = item
        .flatMapLatest { media ->
            media?.id?.let { watchHistoryRepository.isWatched(it) } ?: flowOf(true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val blurDescription: Flow<Boolean> = combine(
        spoilerBlurEnabled,
        isWatched
    ) { enabled, watched -> enabled && !watched }

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
