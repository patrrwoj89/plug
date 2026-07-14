package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.MockDataSource
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataSource: MockDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val featured = dataSource.featured()
            val categories = dataSource.categories()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    featured = featured,
                    categories = categories
                )
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val featured: List<MediaItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null
)
