package com.tvhub.skeleton.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun TVHubTheme(content: @Composable () -> Unit) {
    // TV is normally used in dark ambient mode.
    CompositionLocalProvider(
        LocalAccentColor provides AppColor.Accent,
        LocalSurfaceColor provides AppColor.Surface,
        LocalOnSurfaceColor provides AppColor.OnSurface
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme(
                primary = AppColor.Accent,
                onPrimary = AppColor.Black,
                secondary = AppColor.AccentVariant,
                background = AppColor.Black,
                surface = AppColor.Surface,
                onSurface = AppColor.OnSurface,
                onSurfaceVariant = AppColor.OnSurfaceVariant,
                error = AppColor.Error
            ),
            typography = androidx.compose.material3.Typography(
                displayLarge = AppTypography.hero,
                displayMedium = AppTypography.headline,
                headlineLarge = AppTypography.titleLarge,
                headlineMedium = AppTypography.title,
                bodyLarge = AppTypography.body,
                bodyMedium = AppTypography.caption,
                labelLarge = AppTypography.button,
                labelSmall = AppTypography.badge
            ),
            content = content
        )
    }
}

val LocalAccentColor = staticCompositionLocalOf { AppColor.Accent }
val LocalSurfaceColor = staticCompositionLocalOf { AppColor.Surface }
val LocalOnSurfaceColor = staticCompositionLocalOf { AppColor.OnSurface }

@Composable
fun accentColor(): Color = LocalAccentColor.current
