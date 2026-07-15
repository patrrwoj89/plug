package com.polishmediahub.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.iptv.ChannelWithPrograms
import com.polishmediahub.app.data.iptv.EpgRepository
import com.polishmediahub.app.data.local.ChannelDao
import com.polishmediahub.app.data.local.ChannelEntity
import com.polishmediahub.app.data.local.EpgChannel
import com.polishmediahub.app.data.remote.iptv.IptvUpdateWorker
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val channelDao: ChannelDao,
    @param:ApplicationContext private val appContext: Context
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

    private val _lastEpgSync = MutableStateFlow(LastEpgSyncState())
    val lastEpgSync: StateFlow<LastEpgSyncState> = _lastEpgSync.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                delay(60_000L)
            }
        }

        viewModelScope.launch {
            apiConfigRepository.iptvSourceUrls.collect { _m3uUrl.value = it }
        }

        viewModelScope.launch {
            apiConfigRepository.epgUrl.collect { _epgUrl.value = it }
        }

        viewModelScope.launch {
            combine(
                apiConfigRepository.lastEpgSyncAt,
                apiConfigRepository.lastEpgSyncStatus,
                apiConfigRepository.lastEpgSyncError
            ) { at, status, error ->
                LastEpgSyncState(at, status, error)
            }.collect { _lastEpgSync.value = it }
        }

        viewModelScope.launch {
            combine(
                channelDao.observeAll(),
                epgRepository.observeDistinctChannels(0L, Long.MAX_VALUE)
            ) { cachedChannels, epgChannels ->
                if (cachedChannels.isNotEmpty()) {
                    cachedChannels.map { it.toMediaItem() }
                } else {
                    epgChannels.map { it.toMediaItem() }
                }
            }.collect { _channels.value = it }
        }
    }

    fun loadChannels(m3uUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            apiConfigRepository.setIptvSourceUrls(m3uUrl)
        }
    }

    fun loadEpg(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            apiConfigRepository.setEpgUrl(url)
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            IptvUpdateWorker.startImmediate(appContext)
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
                flowOf(emptyList())
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

    private fun ChannelEntity.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = name,
        subtitle = groupTitle ?: "",
        description = "IPTV channel",
        posterUrl = logoUrl,
        videoUrl = streamUrl,
        tvgId = tvgId,
        channelNumber = channelNumber,
        genres = groupTitle?.let { listOf(it) } ?: emptyList(),
        isLive = true,
        type = MediaItem.Type.CHANNEL
    )

    private fun EpgChannel.toMediaItem(): MediaItem = MediaItem(
        id = "epg:${channelId}",
        title = channelName ?: channelId,
        tvgId = channelId,
        isLive = true,
        type = MediaItem.Type.CHANNEL
    )
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

data class LastEpgSyncState(
    val at: Long = 0L,
    val status: String = "",
    val error: String? = null
)
