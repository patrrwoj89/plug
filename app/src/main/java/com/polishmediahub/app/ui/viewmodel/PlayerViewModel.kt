@file:Suppress("UnsafeOptInUsageError")

package com.polishmediahub.app.ui.viewmodel

import android.util.Log
import com.polishmediahub.app.BuildConfig
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.WatchHistoryRepository
import com.polishmediahub.app.data.audio.AudioHistoryRepository
import com.polishmediahub.app.data.audio.AudioRepository
import com.polishmediahub.app.data.remote.tmdb.TmdbMediaRepository
import com.polishmediahub.app.data.source.BlackFrameDetector
import com.polishmediahub.app.data.source.FrameSample
import com.polishmediahub.app.data.remote.homeassistant.HomeAssistantWebhookClient
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.ui.player.VideoPipManager
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.tv.TvLauncherManager
import com.polishmediahub.app.model.AudioTrack
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import com.polishmediahub.app.ui.player.ExoPlayerTuningConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val audioRepository: AudioRepository,
    private val audioHistoryRepository: AudioHistoryRepository,
    private val torrentMediaSource: TorrentMediaSource,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tvLauncherManager: TvLauncherManager,
    private val traktMediaRepository: TraktMediaRepository,
    private val tmdbMediaRepository: TmdbMediaRepository,
    private val settingsRepository: SettingsRepository,
    private val homeAssistantWebhookClient: HomeAssistantWebhookClient,
    val videoPipManager: VideoPipManager
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    private val _resumePosition = MutableStateFlow(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _resolvedUrl = MutableStateFlow<String?>(null)
    val resolvedUrl: StateFlow<String?> = _resolvedUrl.asStateFlow()

    private val _torrentStatus = MutableStateFlow<com.polishmediahub.app.data.torrent.TorrentStatus?>(null)
    val torrentStatus: StateFlow<com.polishmediahub.app.data.torrent.TorrentStatus?> = _torrentStatus.asStateFlow()

    private val _torrentBuffering = MutableStateFlow<Int?>(null)
    val torrentBuffering: StateFlow<Int?> = _torrentBuffering.asStateFlow()

    private val _preferredQuality = MutableStateFlow("Auto")
    val preferredQuality: StateFlow<String> = _preferredQuality.asStateFlow()

    private val _nextEpisode = MutableStateFlow<MediaItem?>(null)
    val nextEpisode: StateFlow<MediaItem?> = _nextEpisode.asStateFlow()

    private val _autoPlayCancelled = MutableStateFlow(false)
    val autoPlayCancelled: StateFlow<Boolean> = _autoPlayCancelled.asStateFlow()

    private val _subtitleSize = MutableStateFlow(18f)
    val subtitleSize: StateFlow<Float> = _subtitleSize.asStateFlow()

    private val _subtitleColor = MutableStateFlow("White")
    val subtitleColor: StateFlow<String> = _subtitleColor.asStateFlow()

    private val _subtitleVerticalOffset = MutableStateFlow(0f)
    val subtitleVerticalOffset: StateFlow<Float> = _subtitleVerticalOffset.asStateFlow()

    private val _showLoadingStats = MutableStateFlow(false)
    val showLoadingStats: StateFlow<Boolean> = _showLoadingStats.asStateFlow()

    private val _cinemaMode = MutableStateFlow(false)
    val cinemaMode: StateFlow<Boolean> = _cinemaMode.asStateFlow()

    private val _cinemaInfo = MutableStateFlow(CinemaInfo())
    val cinemaInfo: StateFlow<CinemaInfo> = _cinemaInfo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _useAlternativePlayer = MutableStateFlow(false)
    val useAlternativePlayer: StateFlow<Boolean> = _useAlternativePlayer.asStateFlow()

    private val _preferredAudioType = MutableStateFlow("lector")
    val preferredAudioType: StateFlow<String> = _preferredAudioType.asStateFlow()

    private val _nightModeEnabled = MutableStateFlow(false)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()

    private val _dialogueBoostGainmB = MutableStateFlow(1000)
    val dialogueBoostGainmB: StateFlow<Int> = _dialogueBoostGainmB.asStateFlow()

    private val _skipIntroState = MutableStateFlow(SkipIntroState())
    val skipIntroState: StateFlow<SkipIntroState> = _skipIntroState.asStateFlow()

    val exoPlayerTuningConfig: StateFlow<ExoPlayerTuningConfig> = run {
        val bufferingPrimary = combine(
            settingsRepository.tunneledPlaybackEnabled,
            settingsRepository.exoplayerParallelConnections,
            settingsRepository.exoplayerMinBufferMs,
            settingsRepository.exoplayerMaxBufferMs,
            settingsRepository.exoplayerBufferForPlaybackMs
        ) { tunneled, parallel, minBuffer, maxBuffer, playback ->
            BufferingPrimary(tunneled, parallel, minBuffer, maxBuffer, playback)
        }
        val bufferingSecondary = combine(
            settingsRepository.exoplayerBufferForPlaybackAfterRebufferMs,
            settingsRepository.exoplayerBackBufferMs,
            settingsRepository.exoplayerInitialAllocationCount,
            settingsRepository.exoplayerTargetBufferBytes
        ) { rebuffer, backBuffer, initialAlloc, targetBuffer ->
            BufferingSecondary(rebuffer, backBuffer, initialAlloc, targetBuffer)
        }
        combine(bufferingPrimary, bufferingSecondary) { primary, secondary ->
            ExoPlayerTuningConfig(
                tunneledPlaybackEnabled = primary.tunneled,
                parallelConnections = primary.parallel,
                minBufferMs = primary.minBuffer,
                maxBufferMs = primary.maxBuffer,
                bufferForPlaybackMs = primary.playback,
                bufferForPlaybackAfterRebufferMs = secondary.rebuffer,
                backBufferMs = secondary.backBuffer,
                initialAllocationCount = secondary.initialAlloc,
                targetBufferBytes = secondary.targetBuffer
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExoPlayerTuningConfig())
    }

    private val _streamRulesJson = MutableStateFlow("")
    val streamRulesJson: StateFlow<String> = _streamRulesJson.asStateFlow()

    private val blackFrameDetector = BlackFrameDetector()
    private val _blackFrameState = MutableStateFlow(BlackFrameDetector.State())

    private val _pendingSeekToMs = MutableStateFlow<Long?>(null)
    val pendingSeekToMs: StateFlow<Long?> = _pendingSeekToMs.asStateFlow()

    private var bingeGroupingEnabled = true
    private var bingeProfile: BingeProfile? = null

    private val _forceAutoPlayOverlay = MutableStateFlow(false)
    val forceAutoPlayOverlay: StateFlow<Boolean> = _forceAutoPlayOverlay.asStateFlow()

    private var skipSettings = SkipSettings()

    private val _playerStats = MutableStateFlow(PlayerStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    fun setIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
        currentAudioTrack?.let { audioRepository.setCurrentTrack(it, playing) }
        notifyHomeAssistant(if (playing) "play" else "pause")
    }
    fun setPipMode(inPip: Boolean) { _isInPipMode.value = inPip }
    fun updatePlayerStats(stats: PlayerStats) { _playerStats.value = stats }

    fun toggleNightModeEnabled() {
        viewModelScope.launch {
            settingsRepository.setNightModeEnabled(!_nightModeEnabled.value)
        }
    }

    fun toggleUseAlternativePlayer() {
        viewModelScope.launch {
            settingsRepository.setUseAlternativePlayer(!_useAlternativePlayer.value)
        }
    }

    fun cyclePreferredAudioType() {
        viewModelScope.launch {
            val next = if (_preferredAudioType.value == "dubbing") "lector" else "dubbing"
            settingsRepository.setPreferredAudioType(next)
        }
    }

    private fun notifyHomeAssistant(event: String) {
        viewModelScope.launch {
            try {
                homeAssistantWebhookClient.send(event, item.value?.title)
            } catch (e: Exception) {
                // Best-effort webhook; failures must not interrupt playback.
            }
        }
    }

    fun onFrameSample(sample: FrameSample, positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        if (isAudioItem(current) || current.isLive || durationMs <= 0) return
        if (current.introStartMs != null && current.introEndMs != null) return
        val next = blackFrameDetector.process(sample, positionMs, durationMs)
        _blackFrameState.value = next
    }

    fun updatePosition(positionMs: Long, durationMs: Long) {
        recomputeSkipState(positionMs, durationMs)
    }

    private fun recomputeSkipState(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        if (isAudioItem(current) || current.isLive || durationMs <= 0) {
            _skipIntroState.value = SkipIntroState()
            return
        }

        if (!skipSettings.autoSkipIntro) {
            _skipIntroState.value = SkipIntroState()
            return
        }

        val black = _blackFrameState.value
        val hasExplicitMarkers = current.introStartMs != null && current.introEndMs != null

        val introEnd = when {
            hasExplicitMarkers -> current.introEndMs
            black.showSkipIntro -> black.introEndMs
            else -> (skipSettings.introEndSeconds * 1_000L)
        }

        val outroEnd = current.outroEndMs ?: (if (black.showSkipOutro) durationMs else durationMs)
        val outroStart = when {
            current.outroStartMs != null -> current.outroStartMs
            black.showSkipOutro -> black.outroStartMs.coerceAtLeast(0L)
            else -> (outroEnd - skipSettings.outroDurationSeconds * 1_000L).coerceAtLeast(0L)
        }

        val inIntro = introEnd > 0 && positionMs in 0L until introEnd
        val inOutro = outroStart < outroEnd && positionMs >= outroStart && positionMs < outroEnd

        _skipIntroState.value = SkipIntroState(
            showSkipIntro = inIntro && !inOutro,
            introEndMs = introEnd,
            showSkipOutro = inOutro,
            outroEndMs = outroEnd
        )
    }

    fun onSkipIntro() {
        val state = _skipIntroState.value
        if (state.showSkipIntro) {
            _pendingSeekToMs.value = state.introEndMs
            _skipIntroState.value = state.copy(showSkipIntro = false)
        }
    }

    fun onSkipOutro() {
        val state = _skipIntroState.value
        if (state.showSkipOutro) {
            _forceAutoPlayOverlay.value = true
            _skipIntroState.value = state.copy(showSkipOutro = false)
        }
    }

    fun onSeekHandled() {
        _pendingSeekToMs.value = null
    }

    private var currentAudioTrack: AudioTrack? = null
    private var analyticsListener: PlayerAnalyticsListener? = null

    init {
        viewModelScope.launch {
            val id: String? = savedStateHandle["id"]
            val mediaId = id ?: return@launch

            val mediaItem = loadMediaItem(mediaId)
            _item.value = mediaItem

            if (mediaItem != null && isAudioItem(mediaItem)) {
                val audioTrack = currentAudioTrack ?: audioRepository.byId(mediaId)
                if (audioTrack != null) {
                    val resolvedStream = audioRepository.resolve(audioTrack)
                    if (!resolvedStream.isNullOrBlank()) {
                        _resolvedUrl.value = resolvedStream
                        currentAudioTrack = audioTrack.copy(streamUrl = resolvedStream)
                        _item.value = currentAudioTrack!!.toMediaItem()
                    } else {
                        _resolvedUrl.value = audioTrack.streamUrl
                    }
                    if (mediaItem.isLive.not()) {
                        _resumePosition.value = audioHistoryRepository.getPosition(mediaId)
                    }
                }
            } else if (mediaItem != null) {
                val resolved = mediaRepository.resolveItem(mediaItem)
                _item.value = resolved
                _resolvedUrl.value = resolved.videoUrl
                updateBingeProfile(resolved)
                watchHistoryRepository.observePosition(mediaId).collect { position ->
                    _resumePosition.value = position
                }
            }
        }

        viewModelScope.launch {
            _item.filterNotNull().flatMapLatest { current ->
                if (current.id.startsWith("magnet:") || current.id.startsWith("torrent:")) {
                    combine(
                        torrentMediaSource.statusFlow,
                        torrentMediaSource.bufferingProgress
                    ) { statuses, progress ->
                        statuses[infoHashFrom(current.id)] to progress[infoHashFrom(current.id)]
                    }
                } else {
                    flowOf(null to null)
                }
            }.collect { (status, progress) ->
                _torrentStatus.value = status
                _torrentBuffering.value = progress
            }
        }

        viewModelScope.launch {
            settingsRepository.preferredQuality.collect { _preferredQuality.value = it }
        }

        viewModelScope.launch {
            settingsRepository.subtitleSize.collect { _subtitleSize.value = it }
        }

        viewModelScope.launch {
            settingsRepository.subtitleColor.collect { _subtitleColor.value = it }
        }

        viewModelScope.launch {
            settingsRepository.subtitleVerticalOffset.collect { _subtitleVerticalOffset.value = it }
        }

        viewModelScope.launch {
            settingsRepository.showLoadingStats.collect { _showLoadingStats.value = it }
        }

        viewModelScope.launch {
            settingsRepository.cinemaMode.collect { _cinemaMode.value = it }
        }

        viewModelScope.launch {
            settingsRepository.autoSkipIntro.collect { skipSettings = skipSettings.copy(autoSkipIntro = it) }
        }

        viewModelScope.launch {
            settingsRepository.defaultIntroEndSeconds.collect { skipSettings = skipSettings.copy(introEndSeconds = it) }
        }

        viewModelScope.launch {
            settingsRepository.defaultOutroDurationSeconds.collect { skipSettings = skipSettings.copy(outroDurationSeconds = it) }
        }

        viewModelScope.launch {
            settingsRepository.useAlternativePlayer.collect { _useAlternativePlayer.value = it }
        }

        viewModelScope.launch {
            settingsRepository.preferredAudioType.collect { _preferredAudioType.value = it }
        }

        viewModelScope.launch {
            settingsRepository.nightModeEnabled.collect { _nightModeEnabled.value = it }
        }

        viewModelScope.launch {
            settingsRepository.dialogueBoostGainmB.collect { _dialogueBoostGainmB.value = it }
        }

        viewModelScope.launch {
            settingsRepository.streamRules.collect { _streamRulesJson.value = it }
        }

        viewModelScope.launch {
            settingsRepository.bingeGroupingEnabled.collect { bingeGroupingEnabled = it }
        }

        viewModelScope.launch {
            _item.filterNotNull().collect { current ->
                _cinemaInfo.value = CinemaInfo()
                if (_cinemaMode.value) {
                    loadCinemaInfo(current)
                }
            }
        }

        viewModelScope.launch {
            _item.filterNotNull().collect { current ->
                if (isSeriesLike(current)) {
                    loadNextEpisode(current)
                } else {
                    _nextEpisode.value = null
                }
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            savePlaybackProgress(current, positionMs, durationMs)
        }
    }

    fun reportPlaybackProgress(positionMs: Long, durationMs: Long, state: PlaybackState) {
        val current = item.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reportProgress(current, positionMs, durationMs, state)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "reportPlaybackProgress failed: ${e.message}", e)
            }
        }
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            finishPlayback(current, positionMs, durationMs)
        }
    }

    fun cancelAutoPlay() {
        _autoPlayCancelled.value = true
        _forceAutoPlayOverlay.value = false
    }

    fun resetAutoPlayCancel() {
        _autoPlayCancelled.value = false
    }

    fun playNextEpisode(previousPositionMs: Long, previousDurationMs: Long) {
        val next = _nextEpisode.value ?: return
        val previous = _item.value ?: return
        viewModelScope.launch {
            try {
                finishPlayback(previous, previousPositionMs, previousDurationMs)
                val resolved = mediaRepository.resolveItem(next)
                _item.value = resolved
                _resolvedUrl.value = resolved.videoUrl
                updateBingeProfile(resolved)
                _resumePosition.value = 0L
                _nextEpisode.value = null
                _autoPlayCancelled.value = false
                _forceAutoPlayOverlay.value = false
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "playNextEpisode failed: ${e.message}")
            }
        }
    }

    fun setPlayer(exoPlayer: ExoPlayer?) {
        val previous = analyticsListener
        if (previous != null && exoPlayer == null) {
            // Detaching the previous player from the same listener object is
            // done via the stored ExoPlayer reference.
        }
        analyticsListener?.let { listener ->
            try { currentExoPlayer?.removeAnalyticsListener(listener) } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "removeAnalyticsListener failed: ${e.message}", e)
            }
        }
        analyticsListener = null
        currentExoPlayer = null
        _playerStats.value = PlayerStats()

        val player = exoPlayer ?: return
        currentExoPlayer = player
        val listener = PlayerAnalyticsListener().also { analyticsListener = it }
        player.addAnalyticsListener(listener)
    }

    private var currentExoPlayer: ExoPlayer? = null

    override fun onCleared() {
        analyticsListener?.let { listener ->
            try { currentExoPlayer?.removeAnalyticsListener(listener) } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "removeAnalyticsListener failed: ${e.message}", e)
            }
        }
        analyticsListener = null
        currentExoPlayer = null
    }

    private suspend fun loadNextEpisode(current: MediaItem) {
        _nextEpisode.value = null
        try {
            val next = findNextEpisode(current)
            _nextEpisode.value = next
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "loadNextEpisode failed: ${e.message}")
        }
    }

    private suspend fun findNextEpisode(current: MediaItem): MediaItem? {
        val query = buildSearchQuery(current)
        val results = try {
            mediaRepository.search(query)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "findNextEpisode search failed: ${e.message}", e)
            emptyList()
        }

        val currentSeason = current.season ?: 1
        val currentEpisode = current.episode ?: if (current.type == MediaItem.Type.SERIES) 0 else 1
        val nextEpisodeNumber = currentEpisode + 1

        val candidates = results
            .filter { it.id != current.id }
            .filter {
                it.type == MediaItem.Type.EPISODE ||
                    it.type == MediaItem.Type.SERIES ||
                    it.title.contains(current.title, ignoreCase = true)
            }
            .filter { it.season == currentSeason && it.episode == nextEpisodeNumber }
            .ifEmpty {
                results.filter {
                    it.id != current.id &&
                        it.title.contains(current.title, ignoreCase = true) &&
                        it.episode == nextEpisodeNumber
                }
            }

        val profile = bingeProfile
        return if (bingeGroupingEnabled && profile != null) {
            candidates
                .map { it to scoreForBinge(it, profile) }
                .sortedByDescending { it.second }
                .firstOrNull()?.first
                ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }
    }

    private fun buildSearchQuery(current: MediaItem): String {
        val base = current.title
            .replace(Regex("\\s+S\\d+E\\d+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+Season\\s+\\d+", RegexOption.IGNORE_CASE), "")
            .trim()
        return base.ifBlank { current.title }
    }

    private fun isSeriesLike(mediaItem: MediaItem): Boolean {
        return mediaItem.type == MediaItem.Type.SERIES || mediaItem.type == MediaItem.Type.EPISODE
    }

    private data class BingeProfile(
        val resolution: String?,
        val audioTag: String?,
        val videoTags: Set<String>,
        val sourceKeywords: Set<String>
    )

    private data class BufferingPrimary(
        val tunneled: Boolean,
        val parallel: Int,
        val minBuffer: Int,
        val maxBuffer: Int,
        val playback: Int
    )

    private data class BufferingSecondary(
        val rebuffer: Int,
        val backBuffer: Int,
        val initialAlloc: Int,
        val targetBuffer: Int
    )

    private fun updateBingeProfile(mediaItem: MediaItem) {
        if (isSeriesLike(mediaItem)) {
            bingeProfile = extractBingeProfile(mediaItem)
        }
    }

    private fun extractBingeProfile(mediaItem: MediaItem): BingeProfile {
        val text = buildString {
            append(mediaItem.title)
            if (mediaItem.subtitle.isNotBlank()) append(" ").append(mediaItem.subtitle)
            if (mediaItem.description.isNotBlank()) append(" ").append(mediaItem.description)
        }.lowercase()
        val resolution = resolutionRegex.find(text)?.groupValues?.get(1)
            ?.let { resolveResolution(it) }
        val audioTag = audioTagRegex.find(text)?.groupValues?.get(1)
            ?.let { resolveAudioTag(it) }
        val videoTags = videoTagRegex.findAll(text).map { resolveVideoTag(it.groupValues[1]) }.toSet()
        val sourceKeywords = sourceRegex.findAll(text).map { it.value }.toSet()
        return BingeProfile(resolution, audioTag, videoTags, sourceKeywords)
    }

    private fun scoreForBinge(item: MediaItem, profile: BingeProfile): Int {
        val text = buildString {
            append(item.title)
            if (item.subtitle.isNotBlank()) append(" ").append(item.subtitle)
            if (item.description.isNotBlank()) append(" ").append(item.description)
        }.lowercase()
        var score = 0
        profile.resolution?.let { if (text.contains(it, ignoreCase = true)) score += 10 }
        profile.audioTag?.let { if (text.contains(it, ignoreCase = true)) score += 8 }
        profile.videoTags.forEach { tag -> if (text.contains(tag, ignoreCase = true)) score += 5 }
        profile.sourceKeywords.forEach { keyword -> if (text.contains(keyword, ignoreCase = true)) score += 6 }
        return score
    }

    private val resolutionRegex = Regex("\\b(4k|uhd|2160p|1080p|1080i|720p|720i|480p)\\b")
    private val audioTagRegex = Regex("\\b(atmos|dts[-:]?x|dts[-]?hd|dts|truehd|ddp5\\.1|dd\\+|eac3|ac3|aac|lector|dubbing)\\b")
    private val videoTagRegex = Regex("\\b(hdr10\\+|hdr10|hdr|dolby\\s*vision|dv|dovi|sdr)\\b")
    private val sourceRegex = Regex("\\b(torbox|real.?debrid|alldebrid|premiumize|stremio|torrent|web.?dl|webrip|bluray|brrip)\\b")

    private fun resolveResolution(token: String): String = when (token.lowercase()) {
        "4k", "uhd", "2160p" -> "4k"
        "1080p", "1080i" -> "1080p"
        "720p", "720i" -> "720p"
        "480p" -> "480p"
        else -> token
    }

    private fun resolveAudioTag(token: String): String = when (token.lowercase().replace(Regex("[-:]", RegexOption.IGNORE_CASE), "")) {
        "atmos" -> "atmos"
        "dtsx", "dts:x" -> "dtsx"
        "dtshd", "dts-hd" -> "dtshd"
        "dts" -> "dts"
        "truehd" -> "truehd"
        "ddp51", "ddp5.1", "dd+" -> "dd+"
        "eac3" -> "eac3"
        "ac3" -> "ac3"
        "aac" -> "aac"
        "lector" -> "lector"
        "dubbing" -> "dubbing"
        else -> token.lowercase()
    }

    private fun resolveVideoTag(token: String): String = when (token.lowercase().replace(" ", "")) {
        "hdr10+", "hdr10plus" -> "hdr10+"
        "hdr10" -> "hdr10"
        "hdr" -> "hdr"
        "dolbyvision", "dv", "dovi" -> "dolby vision"
        "sdr" -> "sdr"
        else -> token.lowercase()
    }

    fun scrobbleStart(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            if (!isAudioItem(current)) {
                val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
                traktMediaRepository.scrobbleStart(current, progress)
            }
            reportProgress(current, positionMs, durationMs, PlaybackState.PLAYING)
        }
    }

    fun scrobblePause(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            if (!isAudioItem(current)) {
                val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
                traktMediaRepository.scrobblePause(current, progress)
            }
            reportProgress(current, positionMs, durationMs, PlaybackState.PAUSED)
        }
    }

    fun scrobbleStop(positionMs: Long, durationMs: Long) {
        val current = item.value ?: return
        viewModelScope.launch {
            if (!isAudioItem(current)) {
                val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
                traktMediaRepository.scrobbleStop(current, progress)
            }
            reportProgress(current, positionMs, durationMs, PlaybackState.STOPPED)
        }
    }

    private suspend fun loadCinemaInfo(mediaItem: MediaItem) {
        try {
            val cast = tmdbMediaRepository.credits(mediaItem).take(5)
            _cinemaInfo.value = CinemaInfo(
                title = mediaItem.title,
                description = mediaItem.description,
                genres = mediaItem.genres,
                cast = cast
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "loadCinemaInfo failed: ${e.message}", e)
            _cinemaInfo.value = CinemaInfo(
                title = mediaItem.title,
                description = mediaItem.description,
                genres = mediaItem.genres,
                cast = emptyList()
            )
        }
    }

    private suspend fun finishPlayback(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        notifyHomeAssistant("stop")
        if (isAudioItem(mediaItem)) {
            currentAudioTrack?.let { track ->
                audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
                audioRepository.setCurrentTrack(track, false)
            }
        } else {
            tvLauncherManager.onPlaybackStopped(mediaItem, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobbleStop(mediaItem, progress)
            reportProgress(mediaItem, positionMs, durationMs, PlaybackState.STOPPED)
        }
    }

    private suspend fun savePlaybackProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        if (isAudioItem(mediaItem)) {
            currentAudioTrack?.let { track ->
                audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
            }
        } else {
            tvLauncherManager.updatePlaybackProgress(mediaItem, positionMs, durationMs)
            val progress = if (durationMs > 0) (positionMs * 100f / durationMs).coerceIn(0f, 100f) else 0f
            traktMediaRepository.scrobblePause(mediaItem, progress)
        }
    }

    private suspend fun reportProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long, state: PlaybackState) {
        try {
            if (isAudioItem(mediaItem)) {
                if (state == PlaybackState.PLAYING || state == PlaybackState.PAUSED) {
                    currentAudioTrack?.let { track ->
                        audioHistoryRepository.save(track.copy(durationMs = durationMs), positionMs)
                    }
                }
            } else {
                mediaRepository.reportProgress(mediaItem, positionMs, durationMs, state)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("PlayerViewModel", "reportProgress failed: ${e.message}", e)
        }
    }

    private suspend fun loadMediaItem(mediaId: String): MediaItem? {
        val mediaItem = mediaRepository.byId(mediaId)
        if (mediaItem != null) return mediaItem

        val track = audioRepository.byId(mediaId)
        if (track != null) {
            currentAudioTrack = track
            return track.toMediaItem()
        }
        return null
    }

    private fun isAudioItem(mediaItem: MediaItem): Boolean {
        return mediaItem.type == MediaItem.Type.AUDIO ||
            mediaItem.id.startsWith("deezer:track:") ||
            mediaItem.id.startsWith("podcast:") ||
            mediaItem.id.startsWith("radio:") ||
            mediaItem.id.startsWith("local_audio:") ||
            mediaItem.id.startsWith("subsonic:")
    }

    private fun infoHashFrom(id: String): String = id.substringAfter(":")

    private fun AudioTrack.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        subtitle = artist,
        description = description,
        posterUrl = coverUrl,
        backdropUrl = coverUrl,
        duration = if (durationMs > 0) durationMs.toString() else "",
        videoUrl = streamUrl,
        isLive = isLive,
        type = MediaItem.Type.AUDIO
    )

    data class CinemaInfo(
        val title: String = "",
        val description: String = "",
        val genres: List<String> = emptyList(),
        val cast: List<String> = emptyList()
    )

    data class PlayerStats(
        val resolution: String = "--",
        val frameRate: Float = 0f,
        val videoCodec: String = "--",
        val audioCodec: String = "--",
        val currentBitrateMbps: Float = 0f,
        val droppedFrames: Int = 0,
        val jankFrames: Int = 0
    )

    data class SkipIntroState(
        val showSkipIntro: Boolean = false,
        val introEndMs: Long = 0L,
        val showSkipOutro: Boolean = false,
        val outroEndMs: Long = 0L
    )

    private data class SkipSettings(
        val autoSkipIntro: Boolean = true,
        val introEndSeconds: Int = 90,
        val outroDurationSeconds: Int = 120
    )

    private inner class PlayerAnalyticsListener : AnalyticsListener {
        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            val stats = _playerStats.value
            _playerStats.value = stats.copy(
                videoCodec = format.toCodecName(),
                frameRate = if (format.frameRate > 0) format.frameRate else stats.frameRate
            )
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            _playerStats.value = _playerStats.value.copy(audioCodec = format.toCodecName())
        }

        override fun onVideoSizeChanged(
            eventTime: AnalyticsListener.EventTime,
            videoSize: VideoSize
        ) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _playerStats.value = _playerStats.value.copy(
                    resolution = "${videoSize.width}x${videoSize.height}"
                )
            }
        }

        override fun onBandwidthEstimate(
            eventTime: AnalyticsListener.EventTime,
            totalLoadTimeMs: Int,
            totalBytesLoaded: Long,
            bitrateEstimate: Long
        ) {
            val mbps = if (bitrateEstimate > 0) bitrateEstimate / 1_000_000f else 0f
            _playerStats.value = _playerStats.value.copy(currentBitrateMbps = mbps)
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            val current = _playerStats.value.droppedFrames
            _playerStats.value = _playerStats.value.copy(droppedFrames = current + droppedFrames)
        }

        override fun onVideoFrameProcessingOffset(
            eventTime: AnalyticsListener.EventTime,
            totalProcessingOffsetUs: Long,
            frameCount: Int
        ) {
            if (frameCount > 0) {
                val avgOffsetUs = totalProcessingOffsetUs / frameCount
                if (avgOffsetUs > 50_000) {
                    val current = _playerStats.value.jankFrames
                    _playerStats.value = _playerStats.value.copy(jankFrames = current + 1)
                }
            }
        }

        private fun Format.toCodecName(): String {
            val codecsString = codecs
            if (!codecsString.isNullOrBlank()) {
                val base = codecsString.substringBefore(".")
                return when {
                    base.contains("avc", ignoreCase = true) -> "h264"
                    base.contains("hev", ignoreCase = true) || base.contains("hvc", ignoreCase = true) -> "hevc"
                    base.contains("av1", ignoreCase = true) -> "av1"
                    base.contains("vp9", ignoreCase = true) -> "vp9"
                    base.contains("mp4a", ignoreCase = true) -> "aac"
                    base.contains("ac-3", ignoreCase = true) || base.equals("ac3", ignoreCase = true) -> "ac3"
                    base.contains("ec-3", ignoreCase = true) || base.equals("eac3", ignoreCase = true) -> "eac3"
                    base.contains("opus", ignoreCase = true) -> "opus"
                    base.contains("dts", ignoreCase = true) -> "dts"
                    base.contains("truehd", ignoreCase = true) || base.contains("mlp", ignoreCase = true) -> "truehd/atmos"
                    else -> base.lowercase()
                }
            }
            return sampleMimeType?.let { mime ->
                when {
                    mime.startsWith("video/avc") -> "h264"
                    mime.startsWith("video/hevc") -> "hevc"
                    mime.startsWith("video/av01") -> "av1"
                    mime.startsWith("video/vp9") -> "vp9"
                    mime.startsWith("audio/mp4a") -> "aac"
                    mime.startsWith("audio/ac3") -> "ac3"
                    mime.startsWith("audio/eac3") -> "eac3"
                    mime.startsWith("audio/opus") -> "opus"
                    mime.startsWith("audio/vnd.dts") -> "dts"
                    mime.startsWith("audio/true-hd") -> "truehd/atmos"
                    else -> mime.substringAfter("/", "--")
                }
            } ?: "--"
        }
    }
}
