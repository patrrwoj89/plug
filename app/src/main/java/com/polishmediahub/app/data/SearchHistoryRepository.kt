package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search")

@Singleton
class SearchHistoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    val history: Flow<List<String>> = context.searchDataStore.data
        .map { it[KEY_HISTORY]?.split(DELIMITER)?.filter { entry -> entry.isNotBlank() } ?: emptyList() }

    suspend fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        if (!settingsRepository.saveSearchHistory.first()) return
        context.searchDataStore.edit { prefs ->
            val current = prefs[KEY_HISTORY]?.split(DELIMITER)?.toMutableList() ?: mutableListOf()
            current.remove(trimmed)
            current.add(0, trimmed)
            while (current.size > MAX_ENTRIES) current.removeAt(current.lastIndex)
            prefs[KEY_HISTORY] = current.joinToString(DELIMITER)
        }
    }

    suspend fun clear() {
        context.searchDataStore.edit { it.remove(KEY_HISTORY) }
    }

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("history")
        private const val DELIMITER = "\u001f"
        private const val MAX_ENTRIES = 20
    }
}
