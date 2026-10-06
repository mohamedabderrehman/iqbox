package com.iqbox.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.iqbox.app.ui.theme.*

/**
 * Floating Decorative Blobs - أشكال ضبابية للخلفية
 * تضيف عمق وحيوية للصفحة
 */
@Composable
fun FloatingBlobs(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Blob 1 - Top Right (Blue)
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDarkMode) {
                    listOf(
                        AccentBlue.copy(alpha = 0.15f),
                        AccentBlue.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        AccentBlue.copy(alpha = 0.12f),
                        AccentBlue.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                },
                center = Offset(width * 0.85f, height * 0.1f),
                radius = width * 0.5f
            ),
            center = Offset(width * 0.85f, height * 0.1f),
            radius = width * 0.5f
        )
        
        // Blob 2 - Bottom Left (Cyan/Teal)
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.12f),
                        Color(0xFF06B6D4).copy(alpha = 0.04f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.10f),
                        Color(0xFF06B6D4).copy(alpha = 0.03f),
                        Color.Transparent
                    )
                },
                center = Offset(width * 0.1f, height * 0.85f),
                radius = width * 0.45f
            ),
            center = Offset(width * 0.1f, height * 0.85f),
            radius = width * 0.45f
        )
        
        // Blob 3 - Center Left (Purple)
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.10f),
                        Color(0xFF8B5CF6).copy(alpha = 0.03f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.08f),
                        Color(0xFF8B5CF6).copy(alpha = 0.02f),
                        Color.Transparent
                    )
                },
                center = Offset(width * 0.15f, height * 0.35f),
                radius = width * 0.35f
            ),
            center = Offset(width * 0.15f, height * 0.35f),
            radius = width * 0.35f
        )
    }
}
