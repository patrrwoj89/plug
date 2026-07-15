package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.CustomListsRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customListsRepository: CustomListsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val listId: String = savedStateHandle["listId"] ?: ""

    private val _uiState = MutableStateFlow(CustomListDetailUiState())
    val uiState: StateFlow<CustomListDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customListsRepository.observeLists().collect { lists ->
                val name = lists.find { it.listId == listId }?.name
                _uiState.update { it.copy(listName = name ?: it.listName) }
            }
        }

        viewModelScope.launch {
            customListsRepository.observeListItems(listId).collect { entities ->
                if (entities.isEmpty()) {
                    _uiState.update { it.copy(items = emptyList(), isLoading = false) }
                    return@collect
                }
                _uiState.update { it.copy(isLoading = true) }
                val resolved = withContext(Dispatchers.IO) {
                    entities.mapNotNull { mediaRepository.byId(it.mediaId) }
                }
                _uiState.update { it.copy(items = resolved, isLoading = false) }
            }
        }
    }

    data class CustomListDetailUiState(
        val listName: String = "",
        val items: List<MediaItem> = emptyList(),
        val isLoading: Boolean = true
    )
}
