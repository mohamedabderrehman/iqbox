package com.iqbox.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.iqbox.app.data.AppSettingsManager

/**
 * Light Color Scheme — Indigo primary, clean surfaces
 */
private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = CardBackground,
    primaryContainer = AccentBlueLight,
    onPrimaryContainer = CardBackground,

    secondary = AccentPurple,
    onSecondary = CardBackground,
    tertiary = EarningGreen,
    onTertiary = CardBackground,

    background = BackgroundWhite,
    onBackground = TextPrimary,

    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary,

    error = ErrorColor,
    onError = CardBackground,
    errorContainer = ErrorBackground,
    onErrorContainer = ErrorColor,

    outline = BorderLight,
    outlineVariant = DividerColor
)

/**
 * Dark Color Scheme — Deep navy, premium feel
 */
private val DarkColorScheme = darkColorScheme(
    primary = AccentBlueLight,
    onPrimary = DarkBackground,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = DarkTextPrimary,

    secondary = AccentPurple,
    onSecondary = DarkTextPrimary,
    tertiary = EarningGreenLight,
    onTertiary = DarkBackground,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,

    error = ErrorColor,
    onError = DarkTextPrimary,
    errorContainer = ErrorBackground,
    onErrorContainer = ErrorColor,

    outline = DarkBorder,
    outlineVariant = DarkBorder
)

/**
 * Parse hex color string to Compose Color
 */
fun parseColor(hexColor: String): Color {
    return try {
        val colorString = hexColor.removePrefix("#")
        val colorLong = colorString.toLong(16)
        when (colorString.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> AccentBlue
        }
    } catch (e: Exception) {
        AccentBlue
    }
}

@Composable
fun IQBoxTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Get dynamic colors from AppSettings
    val appSettings by AppSettingsManager.settings.collectAsState()
    val dynamicPrimary = parseColor(appSettings.primary_color)
    val dynamicSecondary = parseColor(appSettings.secondary_color)
    
    // Create dynamic color schemes
    val dynamicLightColorScheme = lightColorScheme(
        primary = dynamicPrimary,
        onPrimary = CardBackground,
        primaryContainer = dynamicPrimary.copy(alpha = 0.2f),
        onPrimaryContainer = CardBackground,
        secondary = dynamicSecondary,
        onSecondary = CardBackground,
        tertiary = EarningGreen,
        onTertiary = CardBackground,
        background = BackgroundWhite,
        onBackground = TextPrimary,
        surface = CardBackground,
        onSurface = TextPrimary,
        surfaceVariant = BackgroundGray,
        onSurfaceVariant = TextSecondary,
        error = ErrorColor,
        onError = CardBackground,
        errorContainer = ErrorBackground,
        onErrorContainer = ErrorColor,
        outline = BorderLight,
        outlineVariant = DividerColor
    )
    
    val dynamicDarkColorScheme = darkColorScheme(
        primary = dynamicPrimary,
        onPrimary = DarkBackground,
        primaryContainer = dynamicPrimary.copy(alpha = 0.3f),
        onPrimaryContainer = DarkTextPrimary,
        secondary = dynamicSecondary,
        onSecondary = DarkTextPrimary,
        tertiary = EarningGreenLight,
        onTertiary = DarkBackground,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkCard,
        onSurfaceVariant = DarkTextSecondary,
        error = ErrorColor,
        onError = DarkTextPrimary,
        errorContainer = ErrorBackground,
        onErrorContainer = ErrorColor,
        outline = DarkBorder,
        outlineVariant = DarkBorder
    )
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> dynamicDarkColorScheme
        else -> dynamicLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Light status bar icons for light theme, dark for dark theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
