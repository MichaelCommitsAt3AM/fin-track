package com.fintrack.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FinTrackGreen,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkBackground,       // Main surface
    onSurface = DarkText,
    surfaceContainer = DarkField,   // Text fields
    surfaceContainerLow = DarkCard, // Card backgrounds
    onSurfaceVariant = DarkAccent,
    outline = DarkBorder,
    error = SpendingBills           // For expense text
)

private val LightColorScheme = lightColorScheme(
    primary = FinTrackGreen,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = LightText,
    surface = LightBackground,      // Main surface
    onSurface = LightText,
    surfaceContainer = LightField,  // Text fields
    surfaceContainerLow = LightCard,// Card backgrounds
    onSurfaceVariant = LightAccent,
    outline = LightBorder,
    error = SpendingBills          // For expense text
)

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // From your Type.kt file
        content = content
    )
}