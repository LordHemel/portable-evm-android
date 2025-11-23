package com.example.portableevm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = White,
    background = White,
    surface = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryLight,
    onPrimary = White,
    primaryContainer = BluePrimary,
    onPrimaryContainer = White,
)

@Composable
fun PortableEvmTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
