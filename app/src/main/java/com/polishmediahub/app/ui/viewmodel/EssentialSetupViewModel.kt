package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.legal.LegalSourcesRepository
import com.polishmediahub.app.data.source.WebSourceConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class EssentialSetupViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val legalSourcesRepository: LegalSourcesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EssentialSetupUiState())
    val uiState: StateFlow<EssentialSetupUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun applySelectedSources(
        iptv: Boolean,
        music: Boolean,
        web: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sources = legalSourcesRepository.load() ?: throw IllegalStateException("Could not load sample sources")

                if (iptv) {
                    val iptvUrls = sources.iptv.map { it.url }.filter { it.isNotBlank() }.joinToString("\n")
                    if (iptvUrls.isNotBlank()) {
                        apiConfigRepository.setIptvSourceUrls(iptvUrls)
                    }
                    sources.epg.firstOrNull { it.url.isNotBlank() }?.url?.let {
                        apiConfigRepository.setEpgUrl(it)
                    }
                }

                if (music) {
                    val podcastUrls = sources.podcastFeeds.map { it.url }.filter { it.isNotBlank() }.joinToString("\n")
                    if (podcastUrls.isNotBlank()) {
                        apiConfigRepository.setPodcastFeeds(podcastUrls)
                    }
                    sources.deezerProxy?.url?.takeIf { it.isNotBlank() }?.let {
                        apiConfigRepository.setDeezerProxyUrl(it)
                    }
                }

                if (web) {
                    val addonUrls = sources.stremioAddons.map { it.url }.filter { it.isNotBlank() }.joinToString("\n")
                    if (addonUrls.isNotBlank()) {
                        apiConfigRepository.setStremioAddons(addonUrls)
                    }
                    val webConfig = json.encodeToString(
                        ListSerializer(WebSourceConfig.serializer()),
                        sources.webSources
                    )
                    apiConfigRepository.setWebSourceConfig(webConfig)
                }

                settingsRepository.setFirstLaunchCompleted()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    data class EssentialSetupUiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
