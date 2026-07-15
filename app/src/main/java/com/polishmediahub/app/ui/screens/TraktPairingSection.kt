package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.components.QrCodeGenerator
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.components.TvTextButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.TraktPairingUiState
import com.polishmediahub.app.ui.viewmodel.TraktPairingViewModel

@Composable
fun TraktPairingSection(
    viewModel: TraktPairingViewModel,
    modifier: Modifier = Modifier
) {
    val clientId by viewModel.clientId.collectAsStateWithLifecycle()
    val clientSecret by viewModel.clientSecret.collectAsStateWithLifecycle()
    val pairing by viewModel.pairingState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxWidth(0.5f),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        TvOutlinedTextField(
            value = clientId,
            onValueChange = viewModel::setClientId,
            label = { Text(stringResource(id = R.string.settings_trakt_client_id)) },
            placeholder = { Text(stringResource(id = R.string.settings_trakt_client_id_hint)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        TvOutlinedTextField(
            value = clientSecret,
            onValueChange = viewModel::setClientSecret,
            label = { Text(stringResource(id = R.string.settings_trakt_client_secret)) },
            placeholder = { Text(stringResource(id = R.string.settings_trakt_client_secret_hint)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        if (pairing.isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(
                    text = stringResource(id = R.string.loading),
                    style = AppTypography.body,
                    modifier = Modifier.padding(start = Spacing.md)
                )
            }
        } else if (pairing.isPairing) {
            TraktPairingPanel(
                pairing = pairing,
                onCancel = viewModel::cancelPairing
            )
        } else {
            TvButton(onClick = viewModel::startPairing) {
                Text(
                    text = stringResource(id = R.string.trakt_login_button),
                    color = AppColor.Black,
                    style = AppTypography.button
                )
            }
        }

        pairing.error?.let { error ->
            Text(
                text = error,
                style = AppTypography.caption,
                color = AppColor.Error
            )
        }
    }
}

@Composable
private fun TraktPairingPanel(
    pairing: TraktPairingUiState,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(id = R.string.trakt_pairing_title),
            style = AppTypography.titleLarge
        )

        Text(
            text = stringResource(id = R.string.trakt_pairing_go_to),
            style = AppTypography.body,
            color = AppColor.OnSurfaceVariant
        )
        Text(
            text = pairing.verificationUrl,
            style = AppTypography.body
        )

        Text(
            text = stringResource(id = R.string.trakt_pairing_enter_code),
            style = AppTypography.body,
            color = AppColor.OnSurfaceVariant
        )
        Text(
            text = pairing.userCode,
            style = AppTypography.hero
        )

        val qrContent = remember(pairing.userCode, pairing.verificationUrl) {
            "${pairing.verificationUrl}?code=${pairing.userCode}"
        }
        Image(
            bitmap = QrCodeGenerator.generate(qrContent, 256).asImageBitmap(),
            contentDescription = stringResource(id = R.string.admin_qr_code),
            modifier = Modifier.size(256.dp)
        )

        Text(
            text = stringResource(id = R.string.trakt_pairing_expires_in, pairing.remainingSeconds),
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        TvTextButton(
            text = stringResource(id = R.string.trakt_pairing_cancel),
            onClick = onCancel
        )
    }
}
