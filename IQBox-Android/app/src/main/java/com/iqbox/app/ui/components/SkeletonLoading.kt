package com.iqbox.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * Shimmer Effect for Skeleton Loading
 */
@Composable
fun shimmerBrush(isDarkMode: Boolean = false): Brush {
    val shimmerColors = if (isDarkMode) {
        listOf(
            DarkCard,
            DarkCard.copy(alpha = 0.5f),
            DarkCard
        )
    } else {
        listOf(
            BackgroundGray,
            BackgroundGray.copy(alpha = 0.5f),
            BackgroundGray
        )
    }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 500f, translateAnimation - 500f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * File Card Skeleton
 */
@Composable
fun FileCardSkeleton(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val brush = shimmerBrush(isDarkMode)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkMode) DarkSurface else CardBackground)
    ) {
        // Thumbnail skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(brush)
        )
        
        // Info skeleton
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            // Title line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            // Date
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Compact File Card Skeleton
 */
@Composable
fun CompactFileCardSkeleton(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val brush = shimmerBrush(isDarkMode)
    
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkMode) DarkSurface else CardBackground)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(brush)
        )
        
        // Title
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Section Header Skeleton
 */
@Composable
fun SectionHeaderSkeleton(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val brush = shimmerBrush(isDarkMode)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}

/**
 * Profile Avatar Skeleton
 */
@Composable
fun AvatarSkeleton(
    size: Int = 48,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val brush = shimmerBrush(isDarkMode)
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(brush)
    )
}
