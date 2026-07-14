package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val darkTheme: StateFlow<Boolean> = settingsRepository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val autoplayTrailers: StateFlow<Boolean> = settingsRepository.autoplayTrailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val saveSearchHistory: StateFlow<Boolean> = settingsRepository.saveSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val preferredQuality: StateFlow<String> = settingsRepository.preferredQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "Auto")

    fun setDarkTheme(value: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(value) }
    fun setAutoplayTrailers(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoplayTrailers(value) }
    fun setSaveSearchHistory(value: Boolean) = viewModelScope.launch { settingsRepository.setSaveSearchHistory(value) }
    fun setPreferredQuality(value: String) = viewModelScope.launch { settingsRepository.setPreferredQuality(value) }
}
