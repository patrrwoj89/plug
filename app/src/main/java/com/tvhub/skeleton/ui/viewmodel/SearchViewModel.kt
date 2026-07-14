package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.data.SearchHistoryRepository
import com.tvhub.skeleton.model.MediaItem
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
                val results = try { mediaRepository.search(query) } catch (_: Exception) { emptyList() }
                _uiState.update { it.copy(query = query, results = results, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(isLoading = query.isNotBlank()) }
        _query.value = query
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
    val results: List<MediaItem> = emptyList()
)
