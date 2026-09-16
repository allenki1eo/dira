package com.dira.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B7A57)
private val GreenDark = Color(0xFF0B3D2E)
private val Cream = Color(0xFFF4F7F5)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = GreenDark,
    background = Cream,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5FBF95),
    onPrimary = GreenDark,
    secondary = Color(0xFF9FD9BC),
    background = Color(0xFF0A1210),
    surface = Color(0xFF132019),
)

@Composable
fun DiraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
