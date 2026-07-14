package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
        observeContinueWatching()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val featured = mediaRepository.featured()
                val categories = mediaRepository.categories()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        featured = featured,
                        categories = categories
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            watchHistoryRepository.observeHistory().collect { history ->
                val items = history
                    .filter { it.second.positionMs > 10_000 }
                    .map { it.first }
                    .take(10)
                _uiState.update { it.copy(continueWatching = items) }
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val featured: List<MediaItem> = emptyList(),
    val continueWatching: List<MediaItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null
)
