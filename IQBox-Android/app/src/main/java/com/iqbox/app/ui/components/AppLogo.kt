package com.iqbox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iqbox.app.data.AppSettingsManager
import com.iqbox.app.ui.theme.*

/**
 * App Logo Component - شعار التطبيق مع Gradient
 * يستخدم اللوجو واسم التطبيق من الـ API إذا كانا متاحين
 */
@Composable
fun AppLogo(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Large
) {
    val settings by AppSettingsManager.settings.collectAsState()
    val appName = settings.app_name
    val logoUrl = settings.app_logo_url
    
    val logoSize = when (size) {
        LogoSize.Small -> 56.dp
        LogoSize.Medium -> 72.dp
        LogoSize.Large -> 88.dp
    }
    
    val cornerRadius = when (size) {
        LogoSize.Small -> 14.dp
        LogoSize.Medium -> 18.dp
        LogoSize.Large -> 22.dp
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Logo - من الـ API أو الافتراضي
        if (!logoUrl.isNullOrBlank()) {
            // استخدام اللوجو من الـ API
            AsyncImage(
                model = logoUrl,
                contentDescription = appName,
                modifier = Modifier
                    .size(logoSize)
                    .shadow(
                        elevation = if (isDarkMode) 8.dp else 12.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = AccentBlue.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Fit
            )
        } else {
            // Default logo — layered gradient with subtle inner glow
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .shadow(
                        elevation = if (isDarkMode) 12.dp else 16.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = AccentBlue.copy(alpha = 0.4f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E40AF),
                                AccentBlue,
                                Color(0xFF06B6D4)
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner glow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                )
                Text(
                    text = appName.take(2).uppercase(),
                    style = when (size) {
                        LogoSize.Small -> MaterialTheme.typography.titleLarge
                        LogoSize.Medium -> MaterialTheme.typography.headlineMedium
                        LogoSize.Large -> MaterialTheme.typography.displaySmall
                    },
                    color = BackgroundWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Name - من الـ API
        Text(
            text = appName,
            style = MaterialTheme.typography.displaySmall,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

enum class LogoSize {
    Small,
    Medium,
    Large
}
