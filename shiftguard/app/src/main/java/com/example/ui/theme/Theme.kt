package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ============================================================
// DESIGN TOKENS - Synced from Web Portal index.html :root
// ============================================================
// Update these color values when web CSS :root changes, then rebuild Android.

// Light Color Scheme - matches web portal light theme
private val LightColorScheme = lightColorScheme(
    primary = Primary,              // --primary: #a6ce39
    onPrimary = TextOnPrimary,      // --wa-green dark text
    primaryContainer = Background,  // card bg
    onPrimaryContainer = TextPrimary,
    secondary = Warning,            // --warning: #f59e0b
    onSecondary = DarkTextOnPrimary,
    secondaryContainer = LightSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = Success,             // --success: #22c55e
    onTertiary = DarkTextOnPrimary,
    error = AlertRed,               // --danger: #ef4444
    background = Background,        // --bg: #f5f7fb
    surface = Card,                 // --card: #ffffff
    surfaceVariant = CardElevated,  // elevated card bg
    onBackground = TextPrimary,     // --text: #1e293b
    onSurface = TextPrimary,        // text on white cards
    onSurfaceVariant = TextSecondary,
    outline = Border                // --border: #e5e9f0
)

// Dark Color Scheme - matches web portal when in dark mode
private val DarkColorScheme = darkColorScheme(
    primary = WaGreen,              // --wa-green: #25d366
    onPrimary = DarkTextOnPrimary,  // text on green
    primaryContainer = DarkSurface, // card bg dark
    onPrimaryContainer = TextPrimary,
    secondary = Warning,            // --warning: #f59e0b
    onSecondary = DarkTextOnPrimary,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Success,             // --success: #22c55e
    onTertiary = DarkTextOnPrimary,
    error = AlertRed,               // --danger: #ef4444
    background = DarkBackground,    // --ink: #0f172a
    surface = DarkSurface,          // --ink: #0f172a
    surfaceVariant = DarkSurfaceElevated,
    onBackground = DarkTextPrimary, // inverse of bg
    onSurface = DarkTextPrimary,    // text on dark surfaces
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder            // --border: #1e2d42
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}