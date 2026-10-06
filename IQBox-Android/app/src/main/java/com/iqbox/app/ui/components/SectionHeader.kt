package com.iqbox.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * Section Header Component - عنوان القسم
 */
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    showSeeAll: Boolean = true,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = AccentBlue
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (showSeeAll && onSeeAllClick != null) {
            Row(
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AccentBlue
                )
            }
        }
    }
}
