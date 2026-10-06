package com.iqbox.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbox.app.ui.theme.*

/**
 * Shimmer effect for skeleton loading
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val shimmerColors = if (isDarkMode) {
        listOf(
            DarkCard,
            DarkCardElevated,
            DarkCard
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

/**
 * Glass-morphism style card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) DarkCard.copy(alpha = 0.7f) else CardBackground,
        shadowElevation = if (isDarkMode) 0.dp else 2.dp,
        border = if (isDarkMode) {
            androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.5f))
        } else null
    ) {
        Column(content = content)
    }
}

/**
 * Gradient hero card for balance/earnings
 */
@Composable
fun HeroGradientCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (isDarkMode) GradientHeroDark else GradientHero
                )
            )
    ) {
        // Subtle pattern overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(100f, 50f),
                        radius = 300f
                    )
                )
        )
        content()
    }
}

/**
 * Stat chip with icon + value — gradient accent bar + shadow
 */
@Composable
fun StatChip(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color = Color.Unspecified,
    gradient: List<Color> = emptyList(),
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val chipGradient = gradient.ifEmpty { listOf(color, color.copy(alpha = 0.7f)) }
    val primaryColor = chipGradient.first()

    Surface(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = primaryColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isDarkMode) DarkBorder.copy(alpha = 0.5f) else BorderLight
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Colored accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(chipGradient),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(chipGradient.map { it.copy(alpha = 0.12f) }),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }
    }
}

/**
 * Premium badge
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (small) 6.dp else 8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(GradientPremium),
                    shape = RoundedCornerShape(if (small) 6.dp else 8.dp)
                )
                .padding(
                    horizontal = if (small) 6.dp else 10.dp,
                    vertical = if (small) 2.dp else 4.dp
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (small) 12.dp else 16.dp)
                )
                Text(
                    text = "Premium",
                    style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Action card with icon, title, subtitle, and optional trailing content
 */
@Composable
fun ActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    isDarkMode: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = if (isDarkMode) {
            androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) DarkTextMuted else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isDarkMode) DarkTextMuted else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Section title with accent dot + optional action
 */
@Composable
fun SectionTitle(
    title: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gradient accent dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentBlue, AccentPurple)),
                        CircleShape
                    )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
        }
        if (actionText != null) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                color = AccentBlue.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton card for loading state
 */
@Composable
fun SkeletonFileCard(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = if (isDarkMode) {
            androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Thumbnail skeleton
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                isDarkMode = isDarkMode
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Title
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(16.dp),
                isDarkMode = isDarkMode
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Subtitle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp),
                    isDarkMode = isDarkMode
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp),
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}
