package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndBlueLight,
    onPrimary = IndDarkBg,
    primaryContainer = IndBlueSecondary,
    onPrimaryContainer = IndLightBg,
    secondary = IndSteelSlate,
    onSecondary = IndLightBg,
    tertiary = IndAlertOrange,
    background = IndDarkBg,
    surface = IndDarkSurface,
    onBackground = IndLightBg,
    onSurface = IndLightBg,
    error = IndAlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = IndBluePrimary,
    onPrimary = IndLightSurface,
    primaryContainer = IndBlueLight,
    onPrimaryContainer = IndBluePrimary,
    secondary = IndSteelSlate,
    onSecondary = IndLightSurface,
    tertiary = IndAlertOrange,
    background = IndLightBg,
    surface = IndLightSurface,
    onBackground = IndDarkBg,
    onSurface = IndDarkBg,
    error = IndAlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color to strictly maintain our industrial blue & steel visual identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
