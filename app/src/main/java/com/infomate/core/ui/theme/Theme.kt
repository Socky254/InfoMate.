package com.infomate.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val InfomateColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricViolet,
    tertiary = MatrixGreen,
    background = Obsidian,
    surface = DeepSpace,
    onPrimary = Obsidian,
    onSecondary = SilverText,
    onTertiary = Obsidian,
    onBackground = SilverText,
    onSurface = SilverText,
    error = ErrorRed
)

@Composable
fun InfoMateTheme(
    darkTheme: Boolean = true, // Force Dark for Premium feel
    content: @Composable () -> Unit
) {
    val colorScheme = InfomateColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
