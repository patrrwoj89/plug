package com.polishmediahub.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun TVHubTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val surfaceColor = if (darkTheme) AppColor.Surface else Color.White
    val onSurfaceColor = if (darkTheme) AppColor.OnSurface else Color.Black

    val colorScheme = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = AppColor.Accent,
            onPrimary = AppColor.Black,
            secondary = AppColor.AccentVariant,
            background = AppColor.Black,
            surface = AppColor.Surface,
            onSurface = AppColor.OnSurface,
            onSurfaceVariant = AppColor.OnSurfaceVariant,
            error = AppColor.Error
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = AppColor.Accent,
            onPrimary = Color.White,
            secondary = AppColor.AccentVariant,
            background = Color(0xFFF5F5F5),
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color.DarkGray,
            error = AppColor.Error
        )
    }

    CompositionLocalProvider(
        LocalAccentColor provides AppColor.Accent,
        LocalSurfaceColor provides surfaceColor,
        LocalOnSurfaceColor provides onSurfaceColor
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = colorScheme,
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
