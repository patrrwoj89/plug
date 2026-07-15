package com.polishmediahub.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.admin.AdminHttpServer
import com.polishmediahub.app.data.admin.NetworkAddressHelper
import com.polishmediahub.app.data.local.PluginEntity
import com.polishmediahub.app.data.legal.LegalSourcesRepository
import com.polishmediahub.app.data.plugin.PluginRepository
import com.polishmediahub.app.data.remote.debrid.DebridOAuthManager
import com.polishmediahub.app.data.remote.debrid.DebridProvider
import com.polishmediahub.app.data.remote.debrid.DeviceCodeResponse
import com.polishmediahub.app.ui.components.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val adminHttpServer: AdminHttpServer,
    private val debridOAuthManager: DebridOAuthManager,
    private val pluginRepository: PluginRepository,
    private val legalSourcesRepository: LegalSourcesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        startAdminServer()
        viewModelScope.launch {
            combine(
                apiConfigRepository.tmdbApiKey,
                apiConfigRepository.aniListToken,
                apiConfigRepository.traktClientId,
                apiConfigRepository.debridApiKey,
                apiConfigRepository.debridAccessToken,
                apiConfigRepository.debridProvider,
                apiConfigRepository.iptvSourceUrls,
                apiConfigRepository.stremioAddons,
                apiConfigRepository.kodiUrl,
                apiConfigRepository.webSourceConfig,
                apiConfigRepository.cloudstreamRepoUrls,
                apiConfigRepository.jellyfinUrl,
                apiConfigRepository.jellyfinToken,
                apiConfigRepository.plexUrl,
                apiConfigRepository.plexToken,
                apiConfigRepository.embyUrl,
                apiConfigRepository.embyToken,
                apiConfigRepository.subsonicUrl,
                apiConfigRepository.subsonicUser,
                apiConfigRepository.subsonicPassword,
                apiConfigRepository.podcastFeeds,
                pluginRepository.plugins
            ) { values ->
                AdminUiState(
                    tmdbApiKey = values[0] as String,
                    aniListToken = values[1] as String,
                    traktClientId = values[2] as String,
                    debridApiKey = values[3] as String,
                    debridAccessToken = values[4] as String,
                    debridProvider = values[5] as String,
                    iptvSourceUrls = values[6] as String,
                    stremioAddons = values[7] as String,
                    kodiUrl = values[8] as String,
                    webSourceConfig = values[9] as String,
                    cloudstreamRepoUrls = values[10] as String,
                    jellyfinUrl = values[11] as String,
                    jellyfinToken = values[12] as String,
                    plexUrl = values[13] as String,
                    plexToken = values[14] as String,
                    embyUrl = values[15] as String,
                    embyToken = values[16] as String,
                    subsonicUrl = values[17] as String,
                    subsonicUser = values[18] as String,
                    subsonicPassword = values[19] as String,
                    podcastFeeds = values[20] as String,
                    plugins = values[21] as? List<PluginEntity> ?: emptyList()
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
    fun setKodiUrl(value: String) = viewModelScope.launch { apiConfigRepository.setKodiUrl(value) }
    fun setWebSourceConfig(value: String) = viewModelScope.launch { apiConfigRepository.setWebSourceConfig(value) }
    fun setCloudstreamRepoUrls(value: String) = viewModelScope.launch { apiConfigRepository.setCloudstreamRepoUrls(value) }
    fun setJellyfinUrl(value: String) = viewModelScope.launch { apiConfigRepository.setJellyfinUrl(value) }
    fun setJellyfinToken(value: String) = viewModelScope.launch { apiConfigRepository.setJellyfinToken(value) }
    fun setPlexUrl(value: String) = viewModelScope.launch { apiConfigRepository.setPlexUrl(value) }
    fun setPlexToken(value: String) = viewModelScope.launch { apiConfigRepository.setPlexToken(value) }
    fun setEmbyUrl(value: String) = viewModelScope.launch { apiConfigRepository.setEmbyUrl(value) }
    fun setEmbyToken(value: String) = viewModelScope.launch { apiConfigRepository.setEmbyToken(value) }
    fun setSubsonicUrl(value: String) = viewModelScope.launch { apiConfigRepository.setSubsonicUrl(value) }
    fun setSubsonicUser(value: String) = viewModelScope.launch { apiConfigRepository.setSubsonicUser(value) }
    fun setSubsonicPassword(value: String) = viewModelScope.launch { apiConfigRepository.setSubsonicPassword(value) }
    fun setPodcastFeeds(value: String) = viewModelScope.launch { apiConfigRepository.setPodcastFeeds(value) }

    fun startDebridOAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(debridDeviceCode = null, isLoading = true, error = null)
            try {
                val provider = DebridProvider.entries.find { it.id == _uiState.value.debridProvider } ?: DebridProvider.REAL_DEBRID
                val deviceCode = debridOAuthManager.authorize(provider)
                _uiState.value = _uiState.value.copy(debridDeviceCode = deviceCode, isLoading = false)

                // Poll in background so the QR code stays visible.
                viewModelScope.launch {
                    try {
                        debridOAuthManager.finishAuthorization(deviceCode.deviceCode, provider)
                        _uiState.value = _uiState.value.copy(debridDeviceCode = null)
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun showQrForApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(
            debridDeviceCode = DeviceCodeResponse(
                deviceCode = apiKey,
                userCode = apiKey,
                verificationUri = apiKey,
                interval = 0,
                expiresIn = 0
            )
        )
    }

    fun addPlugin(url: String) {
        viewModelScope.launch {
            try {
                pluginRepository.addPluginFromUrl(url)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun removePlugin(id: String) = viewModelScope.launch { pluginRepository.removePlugin(id) }

    fun setPluginEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        pluginRepository.setEnabled(id, enabled)
    }

    fun reorderPlugins(orderedIds: List<String>) = viewModelScope.launch {
        pluginRepository.reorder(orderedIds)
    }

    fun checkPluginUpdates() = viewModelScope.launch {
        try {
            val updated = pluginRepository.checkUpdates()
            _uiState.value = _uiState.value.copy(error = "Updated $updated plugin(s)")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    fun loadLegalSamples() {
        viewModelScope.launch {
            val sources = legalSourcesRepository.load() ?: return@launch
            sources.iptv.firstOrNull()?.url?.let { apiConfigRepository.setIptvSourceUrls(it) }
            sources.epg.firstOrNull()?.url?.let { apiConfigRepository.setEpgUrl(it) }
            sources.stremioAddons.firstOrNull()?.url?.let { apiConfigRepository.setStremioAddons(it) }
            _uiState.value = _uiState.value.copy(error = "Loaded legal sample sources")
        }
    }

    private fun startAdminServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val port = adminHttpServer.start()
                val ip = NetworkAddressHelper.getLocalIpAddress()
                if (ip != null) {
                    val url = adminHttpServer.adminUrl(ip)
                    val bitmap = QrCodeGenerator.generate(url, 512)
                    _uiState.value = _uiState.value.copy(adminUrl = url, adminQrBitmap = bitmap)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun stopAdminServer() {
        adminHttpServer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopAdminServer()
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
    val kodiUrl: String = "",
    val webSourceConfig: String = "",
    val cloudstreamRepoUrls: String = "",
    val jellyfinUrl: String = "",
    val jellyfinToken: String = "",
    val plexUrl: String = "",
    val plexToken: String = "",
    val embyUrl: String = "",
    val embyToken: String = "",
    val subsonicUrl: String = "",
    val subsonicUser: String = "",
    val subsonicPassword: String = "",
    val podcastFeeds: String = "",
    val plugins: List<PluginEntity> = emptyList(),
    val debridDeviceCode: DeviceCodeResponse? = null,
    val adminUrl: String? = null,
    val adminQrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
