package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[KEY_DARK_THEME] = value }
    suspend fun setAutoplayTrailers(value: Boolean) = context.dataStore.edit { it[KEY_AUTOPLAY_TRAILERS] = value }
    suspend fun setSaveSearchHistory(value: Boolean) = context.dataStore.edit { it[KEY_SAVE_SEARCH_HISTORY] = value }
    suspend fun setPreferredQuality(value: String) = context.dataStore.edit { it[KEY_PREFERRED_QUALITY] = value }
    suspend fun setSpoilerBlur(value: Boolean) = context.dataStore.edit { it[KEY_SPOILER_BLUR] = value }

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AUTOPLAY_TRAILERS = booleanPreferencesKey("autoplay_trailers")
        private val KEY_SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        private val KEY_PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        private val KEY_SPOILER_BLUR = booleanPreferencesKey("spoiler_blur_enabled")
    }
}
