package com.polishmediahub.app.search

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.polishmediahub.app.ui.theme.TVHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY) ?: ""
        } else {
            ""
        }

        setContent {
            TVHubTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search: $query")
                }
            }
        }

        if (query.isNotBlank()) {
            SearchRecentSuggestions(this, TVHubSearchSuggestionProvider.AUTHORITY, TVHubSearchSuggestionProvider.MODE)
                .saveRecentQuery(query, null)
        }
    }
}
