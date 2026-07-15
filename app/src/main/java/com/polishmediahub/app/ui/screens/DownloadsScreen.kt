package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(stringResource(id = R.string.downloads_title), style = AppTypography.titleLarge)

        if (downloads.isEmpty()) {
            Text(stringResource(id = R.string.downloads_empty), color = AppColor.OnSurfaceVariant)
        } else {
            downloads.forEach { download ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(download.title, style = AppTypography.title)
                    Text("Status: ${download.status}", style = AppTypography.body)
                    if (download.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { download.bytesDownloaded.toFloat() / download.totalBytes },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    TvButton(onClick = { viewModel.delete(download.downloadId) }) {
                        Text(stringResource(id = R.string.downloads_delete))
                    }
                }
            }
        }
    }
}
