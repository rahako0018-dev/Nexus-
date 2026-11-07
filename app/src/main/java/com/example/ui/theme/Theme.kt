package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NexusDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = NexusDarkBg,
    primaryContainer = NexusSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = NexusDarkBg,
    secondaryContainer = NexusSurfaceCard,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonCrimson,
    onTertiary = NexusDarkBg,
    background = NexusDarkBg,
    onBackground = TextPrimary,
    surface = NexusSurface,
    onSurface = TextPrimary,
    surfaceVariant = NexusSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NexusCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Gaming app defaults to sleek dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = NexusDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = NexusDarkBg.toArgb()
                window.navigationBarColor = NexusDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
