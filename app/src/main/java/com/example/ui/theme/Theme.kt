package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = NeonCyan,
    tertiary = CyberPurple,
    background = ObsidianBg,
    surface = SurfaceDark,
    surfaceVariant = SurfaceLightDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode by default for breathtaking premium styling
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored Cyber Obsidian palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
