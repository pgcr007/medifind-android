package com.medifind.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BottleGreen,
    onPrimary = Color.White,
    primaryContainer = BottleGreenLight,
    onPrimaryContainer = OnBottleGreenLight,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = OnAmberLight,
    tertiary = Rust,
    onTertiary = Color.White,
    tertiaryContainer = RustLight,
    onTertiaryContainer = OnRustLight,
    background = WarmPaper,
    onBackground = OnPaperText,
    surface = WarmPaper,
    onSurface = OnPaperText,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = Rust,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenAccentDark,
    onPrimary = OnGreenAccentDark,
    primaryContainer = BottleGreen,
    onPrimaryContainer = BottleGreenLight,
    secondary = AmberAccentDark,
    onSecondary = OnAmberAccentDark,
    secondaryContainer = Amber,
    onSecondaryContainer = AmberLight,
    tertiary = RustAccentDark,
    onTertiary = OnRustAccentDark,
    tertiaryContainer = Rust,
    onTertiaryContainer = RustLight,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = RustAccentDark,
    onError = OnRustAccentDark
)

@Composable
fun MediFindAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) is intentionally OFF by default — the whole point
    // of the apothecary ledger rebrand is a consistent brand identity across the
    // Android app and both web dashboards, which a wallpaper-derived palette would break.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}