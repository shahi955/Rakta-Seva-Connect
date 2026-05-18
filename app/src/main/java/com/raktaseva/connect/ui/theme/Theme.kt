package com.raktaseva.connect.ui.theme

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

private val LightColors = lightColorScheme(
    primary = BloodRed,
    onPrimary = ClinicWhite,
    primaryContainer = AccentRose,
    onPrimaryContainer = BloodRedDark,
    secondary = BloodRedDark,
    onSecondary = ClinicWhite,
    background = ClinicWhite,
    onBackground = BloodRedDark,
    surface = ClinicSurface,
    onSurface = BloodRedDark,
    error = BloodRedDark,
    onError = ClinicWhite
)

private val DarkColors = darkColorScheme(
    primary = BloodRedNight,
    onPrimary = BackgroundNight,
    primaryContainer = BloodRedDark,
    onPrimaryContainer = ClinicWhite,
    secondary = AccentRose,
    onSecondary = BackgroundNight,
    background = BackgroundNight,
    onBackground = ClinicWhite,
    surface = SurfaceNight,
    onSurface = ClinicWhite,
    error = AccentRose,
    onError = BackgroundNight
)

@Composable
fun RaktaSevaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RaktaTypography,
        content = content
    )
}
