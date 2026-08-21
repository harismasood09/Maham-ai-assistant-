package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisHudBlack,
    primaryContainer = JarvisCyanDark,
    onPrimaryContainer = JarvisTextPrimary,
    secondary = JarvisGold,
    onSecondary = JarvisHudBlack,
    secondaryContainer = JarvisHudCard,
    onSecondaryContainer = JarvisTextPrimary,
    tertiary = JarvisAmber,
    onTertiary = JarvisHudBlack,
    background = JarvisHudBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisHudSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisHudCard,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisHudCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = JarvisColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = JarvisHudBlack.toArgb()
            window.navigationBarColor = JarvisHudBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
