package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CricketColorScheme = darkColorScheme(
    primary = MpsGold,
    onPrimary = MpsBlack,
    primaryContainer = MpsGoldDark,
    onPrimaryContainer = MpsWhite,
    secondary = MpsGoldLight,
    onSecondary = MpsBlack,
    background = MpsBlack,
    onBackground = MpsWhite,
    surface = MpsDarkGray,
    onSurface = MpsWhite,
    surfaceVariant = MpsMediumGray,
    onSurfaceVariant = MpsWhite,
    error = MpsRed,
    onError = MpsWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the premium look
    dynamicColor: Boolean = false, // Keep colors unified
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CricketColorScheme,
        typography = Typography,
        content = content
    )
}
