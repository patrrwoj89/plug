package com.polishmediahub.app.search

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.SearchRecentSuggestionsProvider
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.CompositeMediaRepository
import com.polishmediahub.app.data.SearchHistoryRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SearchSuggestionProviderEntryPoint {
    fun compositeMediaRepository(): CompositeMediaRepository
    fun searchHistoryRepository(): SearchHistoryRepository
    fun settingsRepository(): SettingsRepository
}

class TVHubSearchSuggestionProvider : ContentProvider() {

    private val entryPoint: SearchSuggestionProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            checkNotNull(context?.applicationContext),
            SearchSuggestionProviderEntryPoint::class.java
        )
    }

    private val compositeMediaRepository by lazy { entryPoint.compositeMediaRepository() }
    private val searchHistoryRepository by lazy { entryPoint.searchHistoryRepository() }
    private val settingsRepository by lazy { entryPoint.settingsRepository() }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val query = selectionArgs?.firstOrNull()?.trim()
            ?: uri.lastPathSegment?.trim()
            ?: return null
        if (query.isBlank()) return null

        return runBlocking(Dispatchers.IO) {
            try {
                val results = compositeMediaRepository.search(query).take(SUGGESTION_LIMIT)
                buildSuggestionCursor(results)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Search failed for '$query': ${e.message}", e)
                null
            }
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val query = values?.getAsString(SearchManager.SUGGEST_COLUMN_QUERY) ?: return null
        runBlocking(Dispatchers.IO) {
            try {
                if (settingsRepository.saveSearchHistory.first()) {
                    searchHistoryRepository.add(query)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to save search history: ${e.message}", e)
            }
        }
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun getType(uri: Uri): String? = SearchManager.SUGGEST_MIME_TYPE

    private fun buildSuggestionCursor(results: List<MediaItem>): Cursor {
        val cursor = MatrixCursor(COLUMNS, results.size)
        results.forEachIndexed { index, item ->
            cursor.newRow()
                .add(BaseColumns._ID, index.toLong())
                .add(SearchManager.SUGGEST_COLUMN_TEXT_1, item.title)
                .add(SearchManager.SUGGEST_COLUMN_TEXT_2, buildSubtitle(item))
                .add(SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, item.posterUrl)
                .add(SearchManager.SUGGEST_COLUMN_INTENT_DATA, "polishmediahub://detail/${item.id}")
        }
        return cursor
    }

    private fun buildSubtitle(item: MediaItem): String {
        val parts = mutableListOf<String>()
        if (item.year.isNotBlank()) parts.add(item.year)
        if (item.genres.isNotEmpty()) parts.add(item.genres.take(3).joinToString(", "))
        if (parts.isEmpty() && item.subtitle.isNotBlank()) parts.add(item.subtitle)
        return parts.joinToString(" · ")
    }

    companion object {
        const val AUTHORITY = "com.polishmediahub.app.search.TVHubSearchSuggestionProvider"
        const val MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_2LINES
        private const val SUGGESTION_LIMIT = 15
        private const val TAG = "TVHubSuggestionProvider"

        private val COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
        )
    }
}
