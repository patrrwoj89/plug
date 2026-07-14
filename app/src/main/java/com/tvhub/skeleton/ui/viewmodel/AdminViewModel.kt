package com.tvhub.skeleton.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvhub.skeleton.data.ApiConfigRepository
import com.tvhub.skeleton.data.remote.debrid.DebridOAuthManager
import com.tvhub.skeleton.data.remote.debrid.DebridProvider
import com.tvhub.skeleton.data.remote.debrid.DeviceCodeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                apiConfigRepository.debridApiKey,
                apiConfigRepository.debridAccessToken,
                apiConfigRepository.debridProvider,
                apiConfigRepository.iptvSourceUrls,
                apiConfigRepository.stremioAddons
            ) { values ->
                AdminUiState(
                    tmdbApiKey = values[0],
                    aniListToken = values[1],
                    traktClientId = values[2],
                    debridApiKey = values[3],
                    debridAccessToken = values[4],
                    debridProvider = values[5],
                    iptvSourceUrls = values[6],
                    stremioAddons = values[7]
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setTmdbApiKey(value: String) = viewModelScope.launch { apiConfigRepository.setTmdbApiKey(value) }
    fun setAniListToken(value: String) = viewModelScope.launch { apiConfigRepository.setAniListToken(value) }
    fun setTraktClientId(value: String) = viewModelScope.launch { apiConfigRepository.setTraktClientId(value) }
    fun setDebridApiKey(value: String) = viewModelScope.launch { apiConfigRepository.setDebridApiKey(value) }
    fun setDebridProvider(value: String) = viewModelScope.launch { apiConfigRepository.setDebridProvider(value) }
    fun setIptvSourceUrls(value: String) = viewModelScope.launch { apiConfigRepository.setIptvSourceUrls(value) }
    fun setStremioAddons(value: String) = viewModelScope.launch { apiConfigRepository.setStremioAddons(value) }

    fun startDebridOAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(debridDeviceCode = null, isLoading = true, error = null)
            try {
                val provider = DebridProvider.entries.find { it.id == _uiState.value.debridProvider } ?: DebridProvider.REAL_DEBRID
                val deviceCode = debridOAuthManager.authorize(provider)
                _uiState.value = _uiState.value.copy(debridDeviceCode = deviceCode, isLoading = false)
                debridOAuthManager.finishAuthorization(deviceCode.deviceCode, provider)
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
    val debridApiKey: String = "",
    val debridAccessToken: String = "",
    val debridProvider: String = DebridProvider.TORBOX.id,
    val iptvSourceUrls: String = "",
    val stremioAddons: String = "",
    val debridDeviceCode: DeviceCodeResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
