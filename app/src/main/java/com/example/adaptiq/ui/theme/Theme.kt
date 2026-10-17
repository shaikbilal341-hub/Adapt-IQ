package com.example.adaptiq.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =========================================================================
// Adapt-IQ Unified Research Visual Theme (Deep Navy & Teal Palette)
// =========================================================================

private val AdaptIQDarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = TextDark,
    primaryContainer = SurfaceElevatedVariant,
    onPrimaryContainer = TextPrimary,

    secondary = AccentBlue,
    onSecondary = TextDark,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,

    tertiary = StatusWarning,
    onTertiary = TextDark,
    tertiaryContainer = StatusWarningContainer,
    onTertiaryContainer = StatusWarning,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedVariant,
    onSurfaceVariant = TextSecondary,

    outline = BorderSubtle,
    outlineVariant = BorderSubtle.copy(alpha = 0.5f),

    error = StatusError,
    onError = TextDark,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

@Composable
fun AdaptIQTheme(
    darkTheme: Boolean = true, // Unified scientific research dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = AdaptIQDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BackgroundDark.toArgb()
                window.navigationBarColor = BackgroundDark.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}