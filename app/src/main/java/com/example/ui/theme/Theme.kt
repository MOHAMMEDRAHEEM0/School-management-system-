package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AcademicLightColorScheme = lightColorScheme(
    primary = GovNavyPrimary,
    onPrimary = PureWhite,
    primaryContainer = GovNavyContainer,
    onPrimaryContainer = GovNavyDark,
    secondary = GovGreen,
    onSecondary = PureWhite,
    secondaryContainer = GovGreenLight,
    onSecondaryContainer = GovGreen,
    tertiary = GovSaffron,
    onTertiary = PureWhite,
    tertiaryContainer = GovSaffronLight,
    onTertiaryContainer = GovSaffron,
    background = GovPageBg,
    onBackground = GovSlateDark,
    surface = GovCardSurface,
    onSurface = GovSlateDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = GovSlateText,
    outline = GovBorderGrey
)

private val AcademicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = GovNavyDark,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF090D16),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AcademicDarkColorScheme else AcademicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
