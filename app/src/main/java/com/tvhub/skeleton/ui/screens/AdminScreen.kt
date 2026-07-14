package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.tvhub.skeleton.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.AdminViewModel

@Composable
fun AdminScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(stringResource(id = R.string.admin_title), style = AppTypography.headline)

        OutlinedTextField(
            value = state.tmdbApiKey,
            onValueChange = viewModel::setTmdbApiKey,
            label = { Text(stringResource(id = R.string.admin_tmdb_key)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.aniListToken,
            onValueChange = viewModel::setAniListToken,
            label = { Text(stringResource(id = R.string.admin_anilist_token)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.traktClientId,
            onValueChange = viewModel::setTraktClientId,
            label = { Text(stringResource(id = R.string.admin_trakt_id)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.iptvSourceUrls,
            onValueChange = viewModel::setIptvSourceUrls,
            label = { Text(stringResource(id = R.string.admin_iptv_urls)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(stringResource(id = R.string.admin_debrid_title), style = AppTypography.titleLarge)

        if (state.debridAccessToken.isNotBlank()) {
            Text(stringResource(id = R.string.admin_debrid_authorized, state.debridProvider), color = AppColor.Accent)
        } else {
            Text(stringResource(id = R.string.admin_debrid_missing), color = AppColor.OnSurfaceVariant)
        }

        Button(onClick = viewModel::startDebridOAuth) {
            Text(stringResource(id = R.string.admin_debrid_link))
        }

        state.debridDeviceCode?.let { code ->
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(stringResource(id = R.string.admin_debrid_go_to), style = AppTypography.title)
            Text(code.verificationUri, style = AppTypography.body)
            Text(stringResource(id = R.string.admin_debrid_enter_code), style = AppTypography.title)
            Text(code.userCode, style = AppTypography.hero)
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(stringResource(id = R.string.admin_error, error), color = AppColor.Error)
        }
    }
}
