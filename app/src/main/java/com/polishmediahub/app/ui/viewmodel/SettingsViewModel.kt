package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.remote.health.HealthCheckWorker
import com.polishmediahub.app.data.remote.health.HealthStatus
import com.polishmediahub.app.data.remote.trakt.TraktSyncWorker
import kotlinx.serialization.json.Json
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val apiConfigRepository: ApiConfigRepository,
    @param:ApplicationContext private val context: Context
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

    val cinemaMode: StateFlow<Boolean> = settingsRepository.cinemaMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val autoSkipIntro: StateFlow<Boolean> = settingsRepository.autoSkipIntro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val defaultIntroEndSeconds: StateFlow<Int> = settingsRepository.defaultIntroEndSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 90)

    val defaultOutroDurationSeconds: StateFlow<Int> = settingsRepository.defaultOutroDurationSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 120)

    val useAlternativePlayer: StateFlow<Boolean> = settingsRepository.useAlternativePlayer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val preferredAudioType: StateFlow<String> = settingsRepository.preferredAudioType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "lector")

    val nightModeEnabled: StateFlow<Boolean> = settingsRepository.nightModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val dialogueBoostGainmB: StateFlow<Int> = settingsRepository.dialogueBoostGainmB
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 1000)

    val isFirstLaunch: StateFlow<Boolean?> = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    val lastEpgSync: StateFlow<LastEpgSyncState> = combine(
        apiConfigRepository.lastEpgSyncAt,
        apiConfigRepository.lastEpgSyncStatus,
        apiConfigRepository.lastEpgSyncError
    ) { at, status, error -> LastEpgSyncState(at, status, error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = LastEpgSyncState())

    val mdbListApiKey: StateFlow<String> = apiConfigRepository.mdbListApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "")

    val traktClientId: StateFlow<String> = apiConfigRepository.traktClientId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "")

    val traktAccessToken: StateFlow<String> = apiConfigRepository.traktAccessToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "")

    val lastTraktSync: StateFlow<LastTraktSyncState> = combine(
        apiConfigRepository.lastTraktSyncAt,
        apiConfigRepository.lastTraktSyncStatus,
        apiConfigRepository.lastTraktSyncError
    ) { at, status, error -> LastTraktSyncState(at, status, error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = LastTraktSyncState())

    val sourceHealth: StateFlow<HealthStatus?> = apiConfigRepository.healthStatuses
        .map { jsonString ->
            if (jsonString.isBlank()) return@map null
            try {
                Json.decodeFromString(HealthStatus.serializer(), jsonString)
            } catch (e: Exception) {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    fun setDarkTheme(value: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(value) }
    fun setAutoplayTrailers(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoplayTrailers(value) }
    fun setSaveSearchHistory(value: Boolean) = viewModelScope.launch { settingsRepository.setSaveSearchHistory(value) }
    fun setPreferredQuality(value: String) = viewModelScope.launch { settingsRepository.setPreferredQuality(value) }
    fun setSpoilerBlur(value: Boolean) = viewModelScope.launch { settingsRepository.setSpoilerBlur(value) }
    fun setSubtitleSize(value: Float) = viewModelScope.launch { settingsRepository.setSubtitleSize(value) }
    fun setSubtitleColor(value: String) = viewModelScope.launch { settingsRepository.setSubtitleColor(value) }
    fun setSubtitleVerticalOffset(value: Float) = viewModelScope.launch { settingsRepository.setSubtitleVerticalOffset(value) }
    fun setShowLoadingStats(value: Boolean) = viewModelScope.launch { settingsRepository.setShowLoadingStats(value) }
    fun setCinemaMode(value: Boolean) = viewModelScope.launch { settingsRepository.setCinemaMode(value) }
    fun setAutoSkipIntro(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoSkipIntro(value) }
    fun setDefaultIntroEndSeconds(value: Int) = viewModelScope.launch { settingsRepository.setDefaultIntroEndSeconds(value) }
    fun setDefaultOutroDurationSeconds(value: Int) = viewModelScope.launch { settingsRepository.setDefaultOutroDurationSeconds(value) }
    fun setUseAlternativePlayer(value: Boolean) = viewModelScope.launch { settingsRepository.setUseAlternativePlayer(value) }
    fun setPreferredAudioType(value: String) = viewModelScope.launch { settingsRepository.setPreferredAudioType(value) }
    fun setNightModeEnabled(value: Boolean) = viewModelScope.launch { settingsRepository.setNightModeEnabled(value) }
    fun setDialogueBoostGainmB(value: Int) = viewModelScope.launch { settingsRepository.setDialogueBoostGainmB(value) }

    fun setMdbListApiKey(value: String) = viewModelScope.launch { apiConfigRepository.setMdbListApiKey(value) }
    fun setTraktClientId(value: String) = viewModelScope.launch { apiConfigRepository.setTraktClientId(value) }
    fun setTraktAccessToken(value: String) = viewModelScope.launch { apiConfigRepository.setTraktAccessToken(value) }

    fun syncTraktNow() = viewModelScope.launch { TraktSyncWorker.startImmediate(context) }

    fun checkSourceHealthNow() = viewModelScope.launch { HealthCheckWorker.startImmediate(context) }
}

data class LastTraktSyncState(
    val at: Long = 0L,
    val status: String = "",
    val error: String? = null
)
