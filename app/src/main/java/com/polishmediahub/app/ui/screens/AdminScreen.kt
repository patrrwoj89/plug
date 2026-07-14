package com.polishmediahub.app.ui.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.data.remote.debrid.DebridProvider
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.QrCodeGenerator
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.AdminViewModel
import com.polishmediahub.app.ui.viewmodel.PinViewModel
import com.polishmediahub.app.navigation.Screen

@Composable
fun AdminScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = hiltViewModel(),
    pinViewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProvider = DebridProvider.entries.find { it.id == state.debridProvider } ?: DebridProvider.TORBOX
    val pinEnabled by pinViewModel.pinEnabled.collectAsStateWithLifecycle()
    val pinCode by pinViewModel.pinCode.collectAsStateWithLifecycle()
    var pinVerified by remember { mutableStateOf(false) }

    if (pinEnabled && pinCode.isNotBlank() && !pinVerified) {
        PinScreen(
            onPinEntered = { entered ->
                if (entered == pinCode) pinVerified = true
            },
            onCancel = { onNavigate(Screen.Home) }
        )
        return
    }

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

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(stringResource(id = R.string.admin_media_servers_title), style = AppTypography.titleLarge)

        ServerConfigFields(
            url = state.jellyfinUrl,
            token = state.jellyfinToken,
            urlLabel = stringResource(id = R.string.admin_jellyfin_url),
            tokenLabel = stringResource(id = R.string.admin_jellyfin_token),
            onUrlChange = viewModel::setJellyfinUrl,
            onTokenChange = viewModel::setJellyfinToken,
            onShowQr = { viewModel.showQrForApiKey(state.jellyfinToken) }
        )

        ServerConfigFields(
            url = state.plexUrl,
            token = state.plexToken,
            urlLabel = stringResource(id = R.string.admin_plex_url),
            tokenLabel = stringResource(id = R.string.admin_plex_token),
            onUrlChange = viewModel::setPlexUrl,
            onTokenChange = viewModel::setPlexToken,
            onShowQr = { viewModel.showQrForApiKey(state.plexToken) }
        )

        ServerConfigFields(
            url = state.embyUrl,
            token = state.embyToken,
            urlLabel = stringResource(id = R.string.admin_emby_url),
            tokenLabel = stringResource(id = R.string.admin_emby_token),
            onUrlChange = viewModel::setEmbyUrl,
            onTokenChange = viewModel::setEmbyToken,
            onShowQr = { viewModel.showQrForApiKey(state.embyToken) }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(stringResource(id = R.string.music_title), style = AppTypography.titleLarge)

        OutlinedTextField(
            value = state.subsonicUrl,
            onValueChange = viewModel::setSubsonicUrl,
            label = { Text(stringResource(id = R.string.admin_subsonic_url)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = state.subsonicUser,
                onValueChange = viewModel::setSubsonicUser,
                label = { Text(stringResource(id = R.string.admin_subsonic_user)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            OutlinedTextField(
                value = state.subsonicPassword,
                onValueChange = viewModel::setSubsonicPassword,
                label = { Text(stringResource(id = R.string.admin_subsonic_password)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
        OutlinedTextField(
            value = state.podcastFeeds,
            onValueChange = viewModel::setPodcastFeeds,
            label = { Text(stringResource(id = R.string.admin_podcast_feeds)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(stringResource(id = R.string.admin_plugins_title), style = AppTypography.titleLarge)

        var pluginUrl by remember { mutableStateOf("") }
        OutlinedTextField(
            value = pluginUrl,
            onValueChange = { pluginUrl = it },
            label = { Text(stringResource(id = R.string.admin_plugin_url)) },
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Button(onClick = { viewModel.addPlugin(pluginUrl); pluginUrl = "" }) {
                Text(stringResource(id = R.string.admin_plugin_add))
            }
            Button(onClick = { viewModel.checkPluginUpdates() }) {
                Text(stringResource(id = R.string.check_updates))
            }
            Button(onClick = { viewModel.loadLegalSamples() }) {
                Text(stringResource(id = R.string.load_legal_samples))
            }
        }

        state.plugins.forEachIndexed { index, plugin ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.padding(vertical = Spacing.sm)
            ) {
                Text(plugin.name, style = AppTypography.body, modifier = Modifier.weight(1f))
                FocusableSurface(
                    onClick = {
                        val reordered = state.plugins.toMutableList().apply {
                            val target = (index - 1).coerceAtLeast(0)
                            if (index != target) add(target, removeAt(index))
                        }
                        viewModel.reorderPlugins(reordered.map { it.pluginId })
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                ) {
                    Text("↑", modifier = Modifier.padding(Spacing.sm))
                }
                FocusableSurface(
                    onClick = {
                        val reordered = state.plugins.toMutableList().apply {
                            val target = (index + 1).coerceAtMost(lastIndex)
                            if (index != target) add(target, removeAt(index))
                        }
                        viewModel.reorderPlugins(reordered.map { it.pluginId })
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                ) {
                    Text("↓", modifier = Modifier.padding(Spacing.sm))
                }
                FocusableSurface(
                    onClick = { viewModel.removePlugin(plugin.pluginId) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                ) {
                    Text(stringResource(id = R.string.admin_plugin_remove), modifier = Modifier.padding(Spacing.sm))
                }
            }
        }

        Button(onClick = {
            viewModel.showQrForApiKey(buildPluginConfigQr(state))
        }) {
            Text(stringResource(id = R.string.admin_plugin_show_qr))
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(stringResource(id = R.string.admin_error, error), color = AppColor.Error)
        }
    }
}

private fun buildPluginConfigQr(state: com.polishmediahub.app.ui.viewmodel.AdminUiState): String {
    val sources = buildString {
        appendLine("""{ "type": "stremio", "id": "stremio", "name": "Stremio", "config": { "urls": "${state.stremioAddons}" } },""")
        appendLine("""{ "type": "iptv", "id": "iptv", "name": "IPTV", "config": { "urls": "${state.iptvSourceUrls}" } },""")
        appendLine("""{ "type": "kodi", "id": "kodi", "name": "Kodi", "config": { "url": "${state.kodiUrl}" } },""")
        appendLine("""{ "type": "cloudstream", "id": "cloudstream", "name": "Cloudstream", "config": { "repos": "${state.cloudstreamRepoUrls}" } }""")
    }
    return """{
        "id": "shared-config",
        "name": "Shared config",
        "version": "1.0",
        "sources": [ $sources ]
    }""".trimIndent()
}

@Composable
private fun ServerConfigFields(
    url: String,
    token: String,
    urlLabel: String,
    tokenLabel: String,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onShowQr: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(urlLabel) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text(tokenLabel) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            if (token.isNotBlank()) {
                Button(onClick = onShowQr) {
                    Text("QR")
                }
            }
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
