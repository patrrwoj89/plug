package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.SettingsRepository
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

    val spoilerBlurEnabled: StateFlow<Boolean> = settingsRepository.spoilerBlurEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val subtitleSize: StateFlow<Float> = settingsRepository.subtitleSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 18f)

    val subtitleColor: StateFlow<String> = settingsRepository.subtitleColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "White")

    val subtitleVerticalOffset: StateFlow<Float> = settingsRepository.subtitleVerticalOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 0f)

    val showLoadingStats: StateFlow<Boolean> = settingsRepository.showLoadingStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    fun setDarkTheme(value: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(value) }
    fun setAutoplayTrailers(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoplayTrailers(value) }
    fun setSaveSearchHistory(value: Boolean) = viewModelScope.launch { settingsRepository.setSaveSearchHistory(value) }
    fun setPreferredQuality(value: String) = viewModelScope.launch { settingsRepository.setPreferredQuality(value) }
    fun setSpoilerBlur(value: Boolean) = viewModelScope.launch { settingsRepository.setSpoilerBlur(value) }
    fun setSubtitleSize(value: Float) = viewModelScope.launch { settingsRepository.setSubtitleSize(value) }
    fun setSubtitleColor(value: String) = viewModelScope.launch { settingsRepository.setSubtitleColor(value) }
    fun setSubtitleVerticalOffset(value: Float) = viewModelScope.launch { settingsRepository.setSubtitleVerticalOffset(value) }
    fun setShowLoadingStats(value: Boolean) = viewModelScope.launch { settingsRepository.setShowLoadingStats(value) }
}
