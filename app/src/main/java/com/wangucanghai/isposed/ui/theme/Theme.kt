package com.wangucanghai.isposed.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 design system for Isposed Manager.
 *
 * Copyright (C) 2026 万古沧海
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4FC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1A1040),
    secondary = Color(0xFF00696E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF0F2),
    onSecondaryContainer = Color(0xFF001F21),
    tertiary = Color(0xFF3D5FD9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEE1FF),
    onTertiaryContainer = Color(0xFF00155D),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1D1B2A),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1D1B2A),
    surfaceVariant = Color(0xFFE7E1F5),
    onSurfaceVariant = Color(0xFF494552),
    outline = Color(0xFF7A7582),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8C0FF),
    onPrimary = Color(0xFF2D2552),
    primaryContainer = Color(0xFF443A78),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFF7FD3D8),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF004F53),
    onSecondaryContainer = Color(0xFFBFF0F2),
    tertiary = Color(0xFFB9C3FF),
    onTertiary = Color(0xFF1A2B7A),
    tertiaryContainer = Color(0xFF3D5FD9),
    onTertiaryContainer = Color(0xFFDEE1FF),
    background = Color(0xFF131222),
    onBackground = Color(0xFFE5E1F0),
    surface = Color(0xFF131222),
    onSurface = Color(0xFFE5E1F0),
    surfaceVariant = Color(0xFF494552),
    onSurfaceVariant = Color(0xFFCBC5D5),
    outline = Color(0xFF948FA0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun IsposedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
