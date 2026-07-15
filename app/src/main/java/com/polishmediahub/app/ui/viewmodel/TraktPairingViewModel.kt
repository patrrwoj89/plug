package com.polishmediahub.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.R
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.remote.trakt.TraktAuthManager
import com.polishmediahub.app.data.remote.trakt.TraktDeviceCodeResponse
import com.polishmediahub.app.data.remote.trakt.TraktSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TraktPairingViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val traktAuthManager: TraktAuthManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _clientId = MutableStateFlow("")
    val clientId: StateFlow<String> = _clientId.asStateFlow()

    private val _clientSecret = MutableStateFlow("")
    val clientSecret: StateFlow<String> = _clientSecret.asStateFlow()

    private val _pairingState = MutableStateFlow(TraktPairingUiState())
    val pairingState: StateFlow<TraktPairingUiState> = _pairingState.asStateFlow()

    private var pairingJob: Job? = null

    init {
        viewModelScope.launch {
            apiConfigRepository.traktClientId.collect { _clientId.value = it }
        }
        viewModelScope.launch {
            apiConfigRepository.traktClientSecret.collect { _clientSecret.value = it }
        }
    }

    fun setClientId(value: String) = viewModelScope.launch {
        apiConfigRepository.setTraktClientId(value)
    }

    fun setClientSecret(value: String) = viewModelScope.launch {
        apiConfigRepository.setTraktClientSecret(value)
    }

    fun startPairing() {
        if (_clientId.value.isBlank() || _clientSecret.value.isBlank()) {
            _pairingState.value = TraktPairingUiState(
                error = context.getString(R.string.trakt_pairing_missing_credentials)
            )
            return
        }

        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            _pairingState.value = TraktPairingUiState(isLoading = true)

            try {
                val code = withContext(Dispatchers.IO) {
                    traktAuthManager.startPairing(_clientId.value)
                }

                val expiresAt = System.currentTimeMillis() + (code.expiresIn * 1000L)
                _pairingState.value = TraktPairingUiState(
                    isPairing = true,
                    userCode = code.userCode,
                    verificationUrl = code.verificationUrl,
                    remainingSeconds = code.expiresIn
                )

                val countdownJob = viewModelScope.launch {
                    while (isActive) {
                        val remaining = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        _pairingState.value = _pairingState.value.copy(remainingSeconds = remaining)
                        if (remaining <= 0) {
                            cancelPairing()
                            _pairingState.value = TraktPairingUiState(
                                error = context.getString(R.string.trakt_pairing_expired)
                            )
                            break
                        }
                        delay(1000)
                    }
                }

                val pollJob = viewModelScope.launch {
                    try {
                        val token = withContext(Dispatchers.IO) {
                            traktAuthManager.completePairing(
                                clientId = _clientId.value,
                                clientSecret = _clientSecret.value,
                                deviceCode = code.deviceCode,
                                interval = code.interval
                            )
                        }
                        traktAuthManager.saveTokens(token.accessToken, token.refreshToken)
                        apiConfigRepository.setLastTraktSync(System.currentTimeMillis(), "success")
                        _pairingState.value = TraktPairingUiState()
                        TraktSyncWorker.startImmediate(context)
                    } catch (e: Exception) {
                        _pairingState.value = TraktPairingUiState(
                            error = e.message ?: context.getString(R.string.trakt_pairing_failed)
                        )
                    }
                }

                pollJob.invokeOnCompletion { countdownJob.cancel() }
            } catch (e: Exception) {
                _pairingState.value = TraktPairingUiState(
                    error = e.message ?: context.getString(R.string.trakt_pairing_failed)
                )
            }
        }
    }

    fun cancelPairing() {
        pairingJob?.cancel()
        _pairingState.value = TraktPairingUiState()
    }
}

data class TraktPairingUiState(
    val isLoading: Boolean = false,
    val isPairing: Boolean = false,
    val userCode: String = "",
    val verificationUrl: String = "",
    val remainingSeconds: Int = 0,
    val error: String? = null
)
