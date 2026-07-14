package com.polishmediahub.app.search

import android.content.SearchRecentSuggestionsProvider

class TVHubSearchSuggestionProvider : SearchRecentSuggestionsProvider() {

    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "com.polishmediahub.app.search.TVHubSearchSuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}
