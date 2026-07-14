package com.tvhub.skeleton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhub.skeleton.R
import com.tvhub.skeleton.data.remote.debrid.DebridProvider
import com.tvhub.skeleton.ui.components.FocusableSurface
import com.tvhub.skeleton.ui.components.QrCodeGenerator
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Radius
import com.tvhub.skeleton.ui.theme.Spacing
import com.tvhub.skeleton.ui.viewmodel.AdminViewModel

@Composable
fun AdminScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProvider = DebridProvider.entries.find { it.id == state.debridProvider } ?: DebridProvider.TORBOX

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

        OutlinedTextField(
            value = state.stremioAddons,
            onValueChange = viewModel::setStremioAddons,
            label = { Text(stringResource(id = R.string.admin_stremio_addons)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.kodiUrl,
            onValueChange = viewModel::setKodiUrl,
            label = { Text(stringResource(id = R.string.admin_kodi_url)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.cloudstreamRepoUrls,
            onValueChange = viewModel::setCloudstreamRepoUrls,
            label = { Text(stringResource(id = R.string.admin_cloudstream_repos)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        OutlinedTextField(
            value = state.webSourceConfig,
            onValueChange = viewModel::setWebSourceConfig,
            label = { Text(stringResource(id = R.string.admin_web_sources)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(stringResource(id = R.string.admin_debrid_title), style = AppTypography.titleLarge)

        ProviderSelector(
            selected = currentProvider,
            onSelect = { viewModel.setDebridProvider(it.id) }
        )

        if (currentProvider == DebridProvider.TORBOX) {
            OutlinedTextField(
                value = state.debridApiKey,
                onValueChange = viewModel::setDebridApiKey,
                label = { Text("TorBox API key") },
                modifier = Modifier.fillMaxWidth(0.5f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            if (state.debridApiKey.isNotBlank()) {
                Button(onClick = { viewModel.showQrForApiKey(state.debridApiKey) }) {
                    Text("Show API key QR")
                }
            }
        }

        if (state.debridAccessToken.isNotBlank()) {
            Text(stringResource(id = R.string.admin_debrid_authorized, state.debridProvider), color = AppColor.Accent)
        } else if (currentProvider == DebridProvider.TORBOX && state.debridApiKey.isNotBlank()) {
            Text(stringResource(id = R.string.admin_debrid_authorized, currentProvider.displayName), color = AppColor.Accent)
        } else {
            Text(stringResource(id = R.string.admin_debrid_missing), color = AppColor.OnSurfaceVariant)
        }

        if (currentProvider == DebridProvider.REAL_DEBRID) {
            Button(onClick = viewModel::startDebridOAuth) {
                Text(stringResource(id = R.string.admin_debrid_link))
            }
        }

        state.debridDeviceCode?.let { code ->
            Spacer(modifier = Modifier.height(Spacing.md))
            val qrContent = if (code.verificationUri.startsWith("http")) "${code.verificationUri}?code=${code.userCode}" else code.verificationUri
            Text(stringResource(id = R.string.admin_debrid_go_to), style = AppTypography.title)
            Text(code.verificationUri, style = AppTypography.body)
            Text(stringResource(id = R.string.admin_debrid_enter_code), style = AppTypography.title)
            Text(code.userCode, style = AppTypography.hero)
            Image(
                bitmap = QrCodeGenerator.generate(qrContent, 256).asImageBitmap(),
                contentDescription = "QR code",
                modifier = Modifier.size(256.dp)
            )
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(stringResource(id = R.string.admin_error, error), color = AppColor.Error)
        }
    }
}

@Composable
private fun ProviderSelector(
    selected: DebridProvider,
    onSelect: (DebridProvider) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        DebridProvider.entries.forEach { provider ->
            val isSelected = provider == selected
            FocusableSurface(
                onClick = { onSelect(provider) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
                backgroundColor = if (isSelected) AppColor.Accent else AppColor.SurfaceVariant,
                focusedBackgroundColor = if (isSelected) AppColor.Accent else AppColor.SurfaceHover
            ) {
                Text(
                    text = provider.displayName,
                    style = AppTypography.button,
                    color = if (isSelected) AppColor.Black else AppColor.OnSurface,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }
    }
}
