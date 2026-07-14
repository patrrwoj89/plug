package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.ApiConfigRepository
import com.tvhub.skeleton.data.remote.debrid.DebridOAuthManager
import com.tvhub.skeleton.data.remote.debrid.DeviceCodeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val debridOAuthManager: DebridOAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                apiConfigRepository.tmdbApiKey,
                apiConfigRepository.aniListToken,
                apiConfigRepository.traktClientId,
                apiConfigRepository.debridAccessToken,
                apiConfigRepository.debridProvider,
                apiConfigRepository.iptvSourceUrls
            ) { values ->
                AdminUiState(
                    tmdbApiKey = values[0],
                    aniListToken = values[1],
                    traktClientId = values[2],
                    debridAccessToken = values[3],
                    debridProvider = values[4],
                    iptvSourceUrls = values[5]
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setTmdbApiKey(value: String) = viewModelScope.launch { apiConfigRepository.setTmdbApiKey(value) }
    fun setAniListToken(value: String) = viewModelScope.launch { apiConfigRepository.setAniListToken(value) }
    fun setTraktClientId(value: String) = viewModelScope.launch { apiConfigRepository.setTraktClientId(value) }
    fun setIptvSourceUrls(value: String) = viewModelScope.launch { apiConfigRepository.setIptvSourceUrls(value) }

    fun startDebridOAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(debridDeviceCode = null, isLoading = true, error = null)
            try {
                val deviceCode = debridOAuthManager.authorize()
                _uiState.value = _uiState.value.copy(debridDeviceCode = deviceCode, isLoading = false)
                debridOAuthManager.finishAuthorization(deviceCode.deviceCode)
                _uiState.value = _uiState.value.copy(debridDeviceCode = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

data class AdminUiState(
    val tmdbApiKey: String = "",
    val aniListToken: String = "",
    val traktClientId: String = "",
    val debridAccessToken: String = "",
    val debridProvider: String = "",
    val iptvSourceUrls: String = "",
    val debridDeviceCode: DeviceCodeResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
