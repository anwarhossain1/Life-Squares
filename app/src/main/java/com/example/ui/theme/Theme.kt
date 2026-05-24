package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ImmersiveDarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    background = ImmersiveBackground,
    onBackground = ImmersiveTextBody,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextBody,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveTextMuted,
    outline = ImmersiveOutline,
    outlineVariant = ImmersiveOutline.copy(alpha = 0.5f),
    secondary = ImmersivePrimaryContainer,
    onSecondary = ImmersiveTextBody,
    error = TodayHighlightPink
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark immersive scheme by default to stay premium
    dynamicColor: Boolean = false, // Disable dynamic colors to keep design integrity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ImmersiveDarkColorScheme,
        typography = Typography,
        content = content
    )
}
