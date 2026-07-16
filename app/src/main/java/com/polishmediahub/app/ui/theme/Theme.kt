package com.polishmediahub.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun TVHubTheme(
    darkTheme: Boolean = true,
    amoledMode: Boolean = false,
    pureBlackSurfaces: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveAmoled = darkTheme && amoledMode
    val backgroundColor = if (effectiveAmoled) Color.Black else if (darkTheme) AppColor.Black else Color(0xFFF5F5F5)
    val surfaceColor = when {
        effectiveAmoled && pureBlackSurfaces -> Color.Black
        darkTheme -> AppColor.Surface
        else -> Color.White
    }
    val surfaceVariantColor = when {
        effectiveAmoled && pureBlackSurfaces -> Color.Black
        darkTheme -> AppColor.SurfaceVariant
        else -> Color(0xFFF5F5F5)
    }
    val onSurfaceColor = if (darkTheme) AppColor.OnSurface else Color.Black
    val onSurfaceVariantColor = if (darkTheme) AppColor.OnSurfaceVariant else Color.DarkGray

    val colorScheme = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = AppColor.Accent,
            onPrimary = AppColor.Black,
            secondary = AppColor.AccentVariant,
            background = backgroundColor,
            surface = surfaceColor,
            surfaceVariant = surfaceVariantColor,
            onSurface = onSurfaceColor,
            onSurfaceVariant = onSurfaceVariantColor,
            error = AppColor.Error
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = AppColor.Accent,
            onPrimary = Color.White,
            secondary = AppColor.AccentVariant,
            background = backgroundColor,
            surface = surfaceColor,
            surfaceVariant = surfaceVariantColor,
            onSurface = onSurfaceColor,
            onSurfaceVariant = onSurfaceVariantColor,
            error = AppColor.Error
        )
    }

    CompositionLocalProvider(
        LocalAmoledMode provides effectiveAmoled,
        LocalPureBlackSurfaces provides (effectiveAmoled && pureBlackSurfaces),
        LocalAccentColor provides AppColor.Accent,
        LocalSurfaceColor provides surfaceColor,
        LocalSurfaceVariantColor provides surfaceVariantColor,
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
val LocalSurfaceVariantColor = staticCompositionLocalOf { AppColor.SurfaceVariant }
val LocalOnSurfaceColor = staticCompositionLocalOf { AppColor.OnSurface }
val LocalAmoledMode = staticCompositionLocalOf { false }
val LocalPureBlackSurfaces = staticCompositionLocalOf { false }

@Composable
fun accentColor(): Color = LocalAccentColor.current
