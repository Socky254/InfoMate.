package com.infomate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricViolet,
    tertiary = MatrixGreen,
    background = Obsidian,
    surface = DeepSpace,
    error = ErrorRed
)

@Composable
fun InfoMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
