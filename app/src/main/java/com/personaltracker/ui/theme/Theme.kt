package com.personaltracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
    secondary = SecondaryTeal,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = TertiaryAmber,
    background = SurfaceLight,
    surface = CardLight,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
    error = ExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0D47A1),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1565C0),
    secondary = androidx.compose.ui.graphics.Color(0xFF80CBC4),
    tertiary = TertiaryAmber,
    background = SurfaceDark,
    surface = CardDark,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
    error = androidx.compose.ui.graphics.Color(0xFFEF9A9A)
)

@Composable
fun SuryaWorldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
        typography = Typography,
        content = content
    )
}
