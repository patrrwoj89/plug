package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.PinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinRepository: PinRepository
) : ViewModel() {

    val pinEnabled: StateFlow<Boolean> = pinRepository.pinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pinCode: StateFlow<String> = pinRepository.pinCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    suspend fun isPinCorrect(code: String): Boolean = pinRepository.verifyPin(code)

    fun setPin(code: String, enabled: Boolean) = viewModelScope.launch {
        pinRepository.setPinCode(code)
        pinRepository.setPinEnabled(enabled)
    }
}
