package com.polishmediahub.app.search

import android.app.SearchManager
import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.util.Log
import com.polishmediahub.app.BuildConfig
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.polishmediahub.app.MainActivity
import com.polishmediahub.app.R
import com.polishmediahub.app.data.SearchHistoryRepository
import com.polishmediahub.app.data.SettingsRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var searchHistoryRepository: SearchHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Intent.ACTION_VIEW == intent.action) {
            val deepLink = intent.dataString
            if (!deepLink.isNullOrBlank()) {
                startMainActivity(deepLink)
                finish()
                return
            }
        }

        val query = if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY) ?: ""
        } else {
            ""
        }

        if (query.isNotBlank()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        if (settingsRepository.saveSearchHistory.first()) {
                            searchHistoryRepository.add(query)
                            SearchRecentSuggestions(
                                this@SearchActivity,
                                TVHubSearchSuggestionProvider.AUTHORITY,
                                TVHubSearchSuggestionProvider.MODE
                            ).saveRecentQuery(query, null)
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.w(TAG, "Failed to save search query: ${e.message}")
                    }
                }
            }
        }

        setContent {
            TVHubTheme {
                SearchResultsScreen(
                    query = query,
                    onResult = { item ->
                        startMainActivity("polishmediahub://detail/${item.id}")
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun startMainActivity(deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri())
            .setClass(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "SearchActivity"
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

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            viewModel.search(query)
        }
    }

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
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = { viewModel.search(query) }
            )
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
