package com.ilya.homewatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WatchColors = Colors(
    primary = Color(0xFF9A7BFF),
    primaryVariant = Color(0xFF6B46D9),
    secondary = Color(0xFF35C2C1),
    secondaryVariant = Color(0xFF1F8E8D),
    background = Color.Black,
    surface = Color(0xFF1C1C22),
    error = Color(0xFFFF5252),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFFB9B9C3),
    onError = Color.Black,
)

@Composable
fun HomeWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WatchColors, content = content)
}
