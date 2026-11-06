package com.docs.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DocsDark = darkColorScheme(
    primary = Indigo400,
    onPrimary = IndigoContainer,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = Color(0xFFE0E1FF),
    secondary = Indigo500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF252742),
    onSecondaryContainer = Color(0xFFC9CBFC),
    tertiary = Success,
    onTertiary = Color.White,
    background = Slate950,
    onBackground = InkTextDark,
    surface = Slate850,
    onSurface = InkTextDark,
    surfaceVariant = Slate800,
    onSurfaceVariant = InkTextSecondaryDark,
    surfaceContainer = Slate900,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate750,
    outline = OutlineDark,
    outlineVariant = Color(0xFF2E313B),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFF3B2223),
    onErrorContainer = Color(0xFFFECACA)
)

private val DocsLight = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Indigo700,
    secondary = Indigo500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = SuccessDark,
    onTertiary = Color.White,
    background = Paper50,
    onBackground = InkTextLight,
    surface = Color.White,
    onSurface = InkTextLight,
    surfaceVariant = Paper100,
    onSurfaceVariant = InkTextSecondaryLight,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Paper100,
    surfaceContainerHighest = Paper200,
    outline = OutlineLight,
    outlineVariant = Color(0xFFEDEFF3),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

/**
 * Docs design system theme. Pass [darkTheme] explicitly to honor the
 * in-app appearance setting (system / light / dark).
 */
@Composable
fun DocsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DocsDark else DocsLight,
        typography = DocsTypography,
        shapes = DocsShapes,
        content = content
    )
}
