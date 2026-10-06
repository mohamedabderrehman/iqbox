package com.iqbox.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * App Footer - يظهر في نهاية الشاشات
 * يحتوي على Copyright و Dark Mode Toggle
 */
@Composable
fun AppFooter(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dark Mode Toggle
        DarkModeToggle(
            isDarkMode = isDarkMode,
            onToggle = { onToggleTheme() }
        )
        
        // Copyright
        Text(
            text = "2026 IQBox. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkMode) DarkTextMuted else TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
