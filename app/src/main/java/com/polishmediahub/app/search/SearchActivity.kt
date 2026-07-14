package com.polishmediahub.app.search

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.ErrorState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.ShimmerBox
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.theme.TVHubTheme
import com.polishmediahub.app.ui.viewmodel.SearchResultsViewModel
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

        if (query.isNotBlank()) {
            SearchRecentSuggestions(
                this,
                TVHubSearchSuggestionProvider.AUTHORITY,
                TVHubSearchSuggestionProvider.MODE
            ).saveRecentQuery(query, null)
        }

        setContent {
            TVHubTheme {
                SearchResultsScreen(
                    query = query,
                    onResult = { item ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("polishmediahub://detail/${item.id}"))
                            .setClass(this, com.polishmediahub.app.MainActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsScreen(
    query: String,
    onResult: (MediaItem) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(
            text = "${stringResource(id = R.string.search)}: $query",
            style = AppTypography.headline
        )
        Spacer(modifier = Modifier.height(Spacing.md))

        when {
            uiState.isLoading -> {
                repeat(4) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }
            uiState.error != null -> ErrorState(message = uiState.error ?: "", onRetry = { viewModel.search(query) })
            uiState.results.isEmpty() -> EmptyState(message = stringResource(id = R.string.no_featured_content))
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(vertical = Spacing.md)
                ) {
                    items(uiState.results, key = { it.id }) { item ->
                        FocusableSurface(
                            onClick = { onResult(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                Text(item.title, style = AppTypography.title)
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = AppTypography.body, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
