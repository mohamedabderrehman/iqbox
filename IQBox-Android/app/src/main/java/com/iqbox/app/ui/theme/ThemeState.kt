package com.iqbox.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Theme State Management - persists dark/light preference via SharedPreferences.
 * Default = dark mode.
 */
class ThemeState(context: Context) {
    private val prefs = context.getSharedPreferences("iqbox_theme", Context.MODE_PRIVATE)

    var isDarkMode by mutableStateOf(prefs.getBoolean("is_dark_mode", true))
        private set

    fun toggleTheme() {
        isDarkMode = !isDarkMode
        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
    }
}

val LocalThemeState = compositionLocalOf<ThemeState> { error("No ThemeState provided") }

@Composable
fun rememberThemeState(): ThemeState {
    val context = LocalContext.current
    return remember { ThemeState(context) }
}
