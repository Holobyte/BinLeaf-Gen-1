package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RealEstateDarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = Color.White,
    secondary = CopperGold,
    onSecondary = Color.Black,
    tertiary = EmeraldSuccess,
    background = SlateDark800,
    onBackground = GeoTextDark,
    surface = SlateCard,
    onSurface = GeoTextDark,
    surfaceVariant = SlateCardLight,
    onSurfaceVariant = GeoTextDark,
    error = Color(0xFFEF4444)
)

private val RealEstateLightColorScheme = darkColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightTertiary,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextMuted,
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable Android dynamic colors to preserve our tailored branding
    content: @Composable () -> Unit,
) {
    // Both default to our elegant Geometric Balance light theme to make sure the brand is consistent
    val colorScheme = RealEstateDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
