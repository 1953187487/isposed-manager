package com.wangucanghai.isposed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Miuix design system for Isposed Manager.
 *
 * The Miuix engine is the source of truth: it drives the app's colors and
 * typography, with optional Material You (Monet) dynamic color via
 * [ThemeController]. Material 3 is bridged on top so legacy `MaterialTheme`
 * usages across the app track Miuix colors and text styles.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun IsposedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val controller = remember {
        if (dynamicColor) {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                keyColor = Color(0xFF3482FF),
                paletteStyle = ThemePaletteStyle.TonalSpot,
            )
        } else {
            ThemeController(colorSchemeMode = ColorSchemeMode.System)
        }
    }
    MiuixTheme(controller = controller) {
        val c = MiuixTheme.colorScheme
        val ts = MiuixTheme.textStyles
        val m3 = if (darkTheme) {
            darkColorScheme(
                primary = c.primary,
                onPrimary = c.onPrimary,
                primaryContainer = c.primaryContainer,
                onPrimaryContainer = c.onPrimaryContainer,
                secondary = c.secondary,
                onSecondary = c.onSecondary,
                secondaryContainer = c.secondaryContainer,
                onSecondaryContainer = c.onSecondaryContainer,
                tertiary = c.primary,
                onTertiary = c.onPrimary,
                background = c.background,
                onBackground = c.onBackground,
                surface = c.surface,
                onSurface = c.onSurface,
                surfaceVariant = c.surfaceVariant,
                onSurfaceVariant = c.onSurfaceContainerVariant,
                outline = c.outline,
                error = c.error,
                onError = c.onError,
                errorContainer = c.errorContainer,
                onErrorContainer = c.onErrorContainer,
            )
        } else {
            lightColorScheme(
                primary = c.primary,
                onPrimary = c.onPrimary,
                primaryContainer = c.primaryContainer,
                onPrimaryContainer = c.onPrimaryContainer,
                secondary = c.secondary,
                onSecondary = c.onSecondary,
                secondaryContainer = c.secondaryContainer,
                onSecondaryContainer = c.onSecondaryContainer,
                tertiary = c.primary,
                onTertiary = c.onPrimary,
                background = c.background,
                onBackground = c.onBackground,
                surface = c.surface,
                onSurface = c.onSurface,
                surfaceVariant = c.surfaceVariant,
                onSurfaceVariant = c.onSurfaceContainerVariant,
                outline = c.outline,
                error = c.error,
                onError = c.onError,
                errorContainer = c.errorContainer,
                onErrorContainer = c.onErrorContainer,
            )
        }
        val typography = Typography(
            headlineSmall = ts.headline2,
            titleLarge = ts.title1,
            titleMedium = ts.title2,
            titleSmall = ts.title3,
            bodyLarge = ts.body1,
            bodyMedium = ts.body2,
            bodySmall = ts.footnote1,
            labelLarge = ts.button,
            labelMedium = ts.footnote2,
        )
        MaterialTheme(
            colorScheme = m3,
            typography = typography,
            content = content,
        )
    }
}
