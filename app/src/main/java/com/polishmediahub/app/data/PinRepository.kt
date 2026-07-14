package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pinDataStore by preferencesDataStore(name = "pin_settings")

@Singleton
class PinRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.pinDataStore

    val pinEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: false }
    val pinCode: Flow<String> = dataStore.data.map { it[KEY_CODE].orEmpty() }

    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setPinCode(code: String) {
        dataStore.edit { it[KEY_CODE] = code }
    }

    suspend fun verifyPin(code: String): Boolean {
        return dataStore.data.map { it[KEY_CODE].orEmpty() }.first() == code
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("pin_enabled")
        private val KEY_CODE = stringPreferencesKey("pin_code")
    }
}
