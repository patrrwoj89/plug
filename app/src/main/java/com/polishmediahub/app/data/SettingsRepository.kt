package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    val darkTheme: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_DARK_THEME] ?: true }

    val autoplayTrailers: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AUTOPLAY_TRAILERS] ?: false }

    val saveSearchHistory: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SAVE_SEARCH_HISTORY] ?: true }

    val preferredQuality: Flow<String> = context.dataStore.data
        .map { it[KEY_PREFERRED_QUALITY] ?: "Auto" }

    val spoilerBlurEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SPOILER_BLUR] ?: false }

    val subtitleSize: Flow<Float> = context.dataStore.data
        .map { it[KEY_SUBTITLE_SIZE] ?: 18f }

    val subtitleColor: Flow<String> = context.dataStore.data
        .map { it[KEY_SUBTITLE_COLOR] ?: "White" }

    val subtitleVerticalOffset: Flow<Float> = context.dataStore.data
        .map { it[KEY_SUBTITLE_VERTICAL_OFFSET] ?: 0f }

    val showLoadingStats: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_LOADING_STATS] ?: false }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_FIRST_LAUNCH] ?: true }

    val cinemaMode: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_CINEMA_MODE] ?: false }

    val autoSkipIntro: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AUTO_SKIP_INTRO] ?: true }

    val defaultIntroEndSeconds: Flow<Int> = context.dataStore.data
        .map { it[KEY_INTRO_END_SECONDS] ?: 90 }

    val defaultOutroDurationSeconds: Flow<Int> = context.dataStore.data
        .map { it[KEY_OUTRO_DURATION_SECONDS] ?: 120 }

    val useAlternativePlayer: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_USE_ALTERNATIVE_PLAYER] ?: false }

    val preferredAudioType: Flow<String> = context.dataStore.data
        .map { it[KEY_PREFERRED_AUDIO_TYPE] ?: "lector" }

    val nightModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_NIGHT_MODE_ENABLED] ?: false }

    val dialogueBoostGainmB: Flow<Int> = context.dataStore.data
        .map { it[KEY_DIALOGUE_BOOST_GAIN_MB] ?: 1000 }

    val amoledMode: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AMOLED_MODE] ?: false }

    val pureBlackSurfaces: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_PURE_BLACK_SURFACES] ?: false }

    val tunneledPlaybackEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TUNNELED_PLAYBACK_ENABLED] ?: false }

    val exoplayerParallelConnections: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_PARALLEL_CONNECTIONS] ?: 4 }

    val exoplayerMinBufferMs: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_MIN_BUFFER_MS] ?: 5_000 }

    val exoplayerMaxBufferMs: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_MAX_BUFFER_MS] ?: 50_000 }

    val exoplayerBufferForPlaybackMs: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_MS] ?: 2_500 }

    val exoplayerBufferForPlaybackAfterRebufferMs: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS] ?: 5_000 }

    val exoplayerBackBufferMs: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_BACK_BUFFER_MS] ?: 0 }

    val exoplayerInitialAllocationCount: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_INITIAL_ALLOCATION_COUNT] ?: 0 }

    val exoplayerTargetBufferBytes: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXOPLAYER_TARGET_BUFFER_BYTES] ?: -1 }

    val streamRules: Flow<String> = context.dataStore.data
        .map { it[KEY_STREAM_RULES] ?: "" }

    val bingeGroupingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_BINGE_GROUPING_ENABLED] ?: true }

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[KEY_DARK_THEME] = value }
    suspend fun setAutoplayTrailers(value: Boolean) = context.dataStore.edit { it[KEY_AUTOPLAY_TRAILERS] = value }
    suspend fun setSaveSearchHistory(value: Boolean) = context.dataStore.edit { it[KEY_SAVE_SEARCH_HISTORY] = value }
    suspend fun setPreferredQuality(value: String) = context.dataStore.edit { it[KEY_PREFERRED_QUALITY] = value }
    suspend fun setSpoilerBlur(value: Boolean) = context.dataStore.edit { it[KEY_SPOILER_BLUR] = value }
    suspend fun setSubtitleSize(value: Float) = context.dataStore.edit { it[KEY_SUBTITLE_SIZE] = value }
    suspend fun setSubtitleColor(value: String) = context.dataStore.edit { it[KEY_SUBTITLE_COLOR] = value }
    suspend fun setSubtitleVerticalOffset(value: Float) = context.dataStore.edit { it[KEY_SUBTITLE_VERTICAL_OFFSET] = value }
    suspend fun setShowLoadingStats(value: Boolean) = context.dataStore.edit { it[KEY_SHOW_LOADING_STATS] = value }
    suspend fun setFirstLaunchCompleted() = context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    suspend fun setCinemaMode(value: Boolean) = context.dataStore.edit { it[KEY_CINEMA_MODE] = value }
    suspend fun setAutoSkipIntro(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_SKIP_INTRO] = value }
    suspend fun setDefaultIntroEndSeconds(value: Int) = context.dataStore.edit { it[KEY_INTRO_END_SECONDS] = value }
    suspend fun setDefaultOutroDurationSeconds(value: Int) = context.dataStore.edit { it[KEY_OUTRO_DURATION_SECONDS] = value }
    suspend fun setUseAlternativePlayer(value: Boolean) = context.dataStore.edit { it[KEY_USE_ALTERNATIVE_PLAYER] = value }
    suspend fun setPreferredAudioType(value: String) = context.dataStore.edit { it[KEY_PREFERRED_AUDIO_TYPE] = value }
    suspend fun setNightModeEnabled(value: Boolean) = context.dataStore.edit { it[KEY_NIGHT_MODE_ENABLED] = value }
    suspend fun setDialogueBoostGainmB(value: Int) = context.dataStore.edit { it[KEY_DIALOGUE_BOOST_GAIN_MB] = value }
    suspend fun setAmoledMode(value: Boolean) = context.dataStore.edit { it[KEY_AMOLED_MODE] = value }
    suspend fun setPureBlackSurfaces(value: Boolean) = context.dataStore.edit { it[KEY_PURE_BLACK_SURFACES] = value }
    suspend fun setTunneledPlaybackEnabled(value: Boolean) = context.dataStore.edit { it[KEY_TUNNELED_PLAYBACK_ENABLED] = value }
    suspend fun setExoplayerParallelConnections(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_PARALLEL_CONNECTIONS] = value.coerceIn(1, 16) }
    suspend fun setExoplayerMinBufferMs(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_MIN_BUFFER_MS] = value.coerceAtLeast(1_000) }
    suspend fun setExoplayerMaxBufferMs(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_MAX_BUFFER_MS] = value.coerceAtLeast(1_000) }
    suspend fun setExoplayerBufferForPlaybackMs(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_MS] = value.coerceAtLeast(0) }
    suspend fun setExoplayerBufferForPlaybackAfterRebufferMs(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS] = value.coerceAtLeast(0) }
    suspend fun setExoplayerBackBufferMs(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_BACK_BUFFER_MS] = value.coerceAtLeast(0) }
    suspend fun setExoplayerInitialAllocationCount(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_INITIAL_ALLOCATION_COUNT] = value.coerceAtLeast(0) }
    suspend fun setExoplayerTargetBufferBytes(value: Int) = context.dataStore.edit { it[KEY_EXOPLAYER_TARGET_BUFFER_BYTES] = value }
    suspend fun setStreamRules(value: String) = context.dataStore.edit { it[KEY_STREAM_RULES] = value }
    suspend fun setBingeGroupingEnabled(value: Boolean) = context.dataStore.edit { it[KEY_BINGE_GROUPING_ENABLED] = value }

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AUTOPLAY_TRAILERS = booleanPreferencesKey("autoplay_trailers")
        private val KEY_SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        private val KEY_PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        private val KEY_SPOILER_BLUR = booleanPreferencesKey("spoiler_blur_enabled")
        private val KEY_SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        private val KEY_SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        private val KEY_SUBTITLE_VERTICAL_OFFSET = floatPreferencesKey("subtitle_vertical_offset")
        private val KEY_SHOW_LOADING_STATS = booleanPreferencesKey("show_loading_stats")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_CINEMA_MODE = booleanPreferencesKey("cinema_mode")
        private val KEY_AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        private val KEY_INTRO_END_SECONDS = intPreferencesKey("intro_end_seconds")
        private val KEY_OUTRO_DURATION_SECONDS = intPreferencesKey("outro_duration_seconds")
        private val KEY_USE_ALTERNATIVE_PLAYER = booleanPreferencesKey("use_alternative_player")
        private val KEY_PREFERRED_AUDIO_TYPE = stringPreferencesKey("preferred_audio_type")
        private val KEY_NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
        private val KEY_DIALOGUE_BOOST_GAIN_MB = intPreferencesKey("dialogue_boost_gain_mb")
        private val KEY_AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        private val KEY_PURE_BLACK_SURFACES = booleanPreferencesKey("pure_black_surfaces")
        private val KEY_TUNNELED_PLAYBACK_ENABLED = booleanPreferencesKey("tunneled_playback_enabled")
        private val KEY_EXOPLAYER_PARALLEL_CONNECTIONS = intPreferencesKey("exoplayer_parallel_connections")
        private val KEY_EXOPLAYER_MIN_BUFFER_MS = intPreferencesKey("exoplayer_min_buffer_ms")
        private val KEY_EXOPLAYER_MAX_BUFFER_MS = intPreferencesKey("exoplayer_max_buffer_ms")
        private val KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_MS = intPreferencesKey("exoplayer_buffer_for_playback_ms")
        private val KEY_EXOPLAYER_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = intPreferencesKey("exoplayer_buffer_for_playback_after_rebuffer_ms")
        private val KEY_EXOPLAYER_BACK_BUFFER_MS = intPreferencesKey("exoplayer_back_buffer_ms")
        private val KEY_EXOPLAYER_INITIAL_ALLOCATION_COUNT = intPreferencesKey("exoplayer_initial_allocation_count")
        private val KEY_EXOPLAYER_TARGET_BUFFER_BYTES = intPreferencesKey("exoplayer_target_buffer_bytes")
        private val KEY_STREAM_RULES = stringPreferencesKey("stream_rules")
        private val KEY_BINGE_GROUPING_ENABLED = booleanPreferencesKey("binge_grouping_enabled")
    }
}
