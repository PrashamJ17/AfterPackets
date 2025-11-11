package com.packethunter.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cybersecurity dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = Color.Black,
    primaryContainer = CyberBlueDark,
    onPrimaryContainer = CyberBlueLight,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = CyberCyanDark,
    onSecondaryContainer = CyberCyanLight,
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedDark,
    onErrorContainer = AlertRedLight,
    background = BackgroundBlack,
    onBackground = TextWhite,
    surface = SurfaceGray,
    onSurface = TextWhite,
    surfaceVariant = SurfaceGrayDark,
    onSurfaceVariant = TextGray,
    outline = BorderGray
)

@Composable
fun MobilePacketHunterTheme(
    darkTheme: Boolean = true, // Always dark theme for cybersecurity aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
