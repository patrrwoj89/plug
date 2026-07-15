package com.polishmediahub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = AppTypography.body,
            color = AppColor.Error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )

        onRetry?.let { retry ->
            FocusableSurface(
                onClick = retry,
                modifier = Modifier.padding(top = Spacing.lg),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md),
                backgroundColor = AppColor.SurfaceVariant,
                focusedBackgroundColor = AppColor.SurfaceHover
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.retry),
                        tint = AppColor.OnSurface
                    )
                    Text(
                        text = stringResource(id = R.string.retry),
                        style = AppTypography.button,
                        color = AppColor.OnSurface,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
        }
    }
}
