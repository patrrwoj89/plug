package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.CustomListsRepository
import com.polishmediahub.app.data.local.CustomListEntity
import com.polishmediahub.app.data.local.CustomListItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomListsViewModel @Inject constructor(
    private val customListsRepository: CustomListsRepository
) : ViewModel() {

    val lists: Flow<List<CustomListEntity>> = customListsRepository.observeLists()

    fun listItems(listId: String): Flow<List<CustomListItemEntity>> =
        customListsRepository.observeListItems(listId)

    fun createList(name: String) = viewModelScope.launch {
        customListsRepository.createList(name)
    }

    fun deleteList(listId: String) = viewModelScope.launch {
        customListsRepository.deleteList(listId)
    }

    fun addItem(listId: String, mediaId: String) = viewModelScope.launch {
        customListsRepository.addItem(listId, mediaId)
    }

    fun removeItem(listId: String, mediaId: String) = viewModelScope.launch {
        customListsRepository.removeItem(listId, mediaId)
    }
}
