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
    }
}
