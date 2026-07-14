package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.theme.Spacing

@Composable
fun PinScreen(
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.enter_pin),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Text(
                    text = if (pin.length > index) "●" else "○",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        if (error) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(stringResource(id = R.string.pin_incorrect), color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "")
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (row in keys.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    row.forEach { key ->
                        if (key.isNotEmpty()) {
                            FocusableSurface(
                                onClick = {
                                    if (pin.length < 4) pin += key
                                    if (pin.length == 4) {
                                        onPinEntered(pin)
                                        pin = ""
                                    }
                                },
                                modifier = Modifier.width(64.dp).height(48.dp)
                            ) {
                                Text(key, style = MaterialTheme.typography.titleLarge)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(64.dp).height(48.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Button(onClick = { pin = "" }) {
                Text(stringResource(id = R.string.clear))
            }
            Button(onClick = onCancel) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    }
}
