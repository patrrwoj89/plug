package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SearchHistoryRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        searchHistoryRepository.history
            .onEach { _history.value = it }
            .launchIn(viewModelScope)

        @OptIn(FlowPreview::class)
        _query
            .debounce(300)
            .onEach { query ->
                if (query.isBlank()) {
                    _uiState.update { it.copy(query = query, results = emptyList(), isLoading = false, error = null) }
                    return@onEach
                }
                _uiState.update { it.copy(query = query, isLoading = true, error = null) }
                try {
                    val results = mediaRepository.search(query)
                    _uiState.update { it.copy(results = results, isLoading = false, error = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(results = emptyList(), isLoading = false, error = e.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _query.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(query = query, results = emptyList(), isLoading = false, error = null) }
        } else {
            _uiState.update { it.copy(query = query, isLoading = true, error = null) }
        }
    }

    fun submitSearch(query: String) {
        viewModelScope.launch { searchHistoryRepository.add(query) }
        onQueryChange(query)
    }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val results: List<MediaItem> = emptyList()
)
