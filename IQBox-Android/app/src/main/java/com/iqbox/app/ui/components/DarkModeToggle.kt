package com.iqbox.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.AccentBlue

/**
 * Dark Mode Toggle Button - زر تبديل الوضع الليلي/النهاري
 * يظهر في نهاية الشاشات
 */
@Composable
fun DarkModeToggle(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "icon_rotation"
    )
    
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                color = if (isDarkMode) MaterialTheme.colorScheme.surface else AccentBlue.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .clickable { onToggle(!isDarkMode) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
            tint = if (isDarkMode) Color(0xFFFFB800) else AccentBlue
        )
    }
}
