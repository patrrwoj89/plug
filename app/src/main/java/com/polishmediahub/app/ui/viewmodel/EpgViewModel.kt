package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.iptv.ChannelWithPrograms
import com.polishmediahub.app.data.iptv.EpgRepository
import com.polishmediahub.app.data.remote.iptv.IptvRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpgViewModel @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val epgRepository: EpgRepository,
    private val iptvRepository: IptvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpgUiState())
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private val _epgUrl = MutableStateFlow("")
    val epgUrl: StateFlow<String> = _epgUrl.asStateFlow()

    private val _m3uUrl = MutableStateFlow("")
    val m3uUrl: StateFlow<String> = _m3uUrl.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _channels = MutableStateFlow<List<MediaItem>>(emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                delay(60_000L)
            }
        }
    }

    fun loadEpg(xmltvUrl: String) {
        _epgUrl.value = xmltvUrl
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                epgRepository.loadEpg(xmltvUrl)
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadChannelsForEpg()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun loadChannels(m3uUrl: String) {
        _m3uUrl.value = m3uUrl
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = fetchChannels(m3uUrl)
                _channels.value = items
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private suspend fun fetchChannels(m3uUrl: String): List<MediaItem> {
        if (m3uUrl.isNotBlank()) apiConfigRepository.setIptvSourceUrls(m3uUrl)
        return iptvRepository.categories().flatMap { it.items }
            .ifEmpty { iptvRepository.featured() }
    }

    private fun loadChannelsForEpg() {
        viewModelScope.launch {
            val epgChannels = epgRepository.observeDistinctChannels(windowStart, windowEnd).first()
            val current = _channels.value
            if (current.isEmpty()) {
                _channels.value = epgChannels.map { ch ->
                    MediaItem(
                        id = "epg:${ch.channelId}",
                        title = ch.channelName ?: ch.channelId,
                        tvgId = ch.channelId,
                        type = MediaItem.Type.CHANNEL
                    )
                }
            }
        }
    }

    private val windowStart: Long
        get() = _now.value - 30 * 60 * 1000L

    private val windowEnd: Long
        get() = _now.value + 4 * 60 * 60 * 1000L

    private val pixelsPerMinute: Float = 4f

    val timelineState: StateFlow<EpgTimelineState> = combine(
        _now,
        _channels
    ) { now, channels ->
        val from = now - 30 * 60 * 1000L
        val to = now + 4 * 60 * 60 * 1000L
        val channelIds = channels.mapNotNull { it.tvgId }.ifEmpty { channels.map { it.id } }
        EpgTimelineState(
            now = now,
            windowStart = from,
            windowEnd = to,
            channelIds = channelIds,
            channels = channels,
            pixelsPerMinute = pixelsPerMinute
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EpgTimelineState())

    val channelsWithPrograms: StateFlow<List<ChannelWithPrograms>> = timelineState
        .flatMapLatest { state ->
            if (state.channelIds.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                epgRepository.observeForChannels(state.channelIds, state.windowStart, state.windowEnd)
                    .map { programsByChannel ->
                        state.channels.map { ch ->
                            val id = ch.tvgId ?: ch.id
                            ChannelWithPrograms(
                                channelId = id,
                                channelName = ch.title,
                                programs = programsByChannel[id] ?: emptyList()
                            )
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class EpgUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EpgTimelineState(
    val now: Long = 0L,
    val windowStart: Long = 0L,
    val windowEnd: Long = 0L,
    val channelIds: List<String> = emptyList(),
    val channels: List<MediaItem> = emptyList(),
    val pixelsPerMinute: Float = 4f
)
