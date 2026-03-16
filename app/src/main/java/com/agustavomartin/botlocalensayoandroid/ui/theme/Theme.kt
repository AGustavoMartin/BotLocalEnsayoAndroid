package com.agustavomartin.botlocalensayoandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF47D7D1),
    secondary = Color(0xFFF0A24D),
    tertiary = Color(0xFFF25F5C),
    background = Color(0xFF081018),
    surface = Color(0xFF17212B),
    onPrimary = Color(0xFF03151A),
    onBackground = Color(0xFFF6F3EC),
    onSurface = Color(0xFFF6F3EC)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E948B),
    secondary = Color(0xFF8A4B00),
    tertiary = Color(0xFFB91C1C),
    background = Color(0xFFF5F3EE),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

@Composable
fun BotLocalEnsayoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
