package com.iqbox.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * Premium Button Component - تصميم احترافي مع Gradient وأيقونة
 */
@Composable
fun IQButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: ButtonVariant = ButtonVariant.Primary,
    showArrow: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animation ناعمة عند الضغط
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "button_scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .then(
                if (variant == ButtonVariant.Primary && enabled) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = AccentBlue.copy(alpha = 0.4f)
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AccentBlue,
                                    Color(0xFF06B6D4) // Cyan
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(
                            enabled = !isLoading,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                } else if (variant == ButtonVariant.Primary && !enabled) {
                    Modifier
                        .background(
                            color = TextDisabled,
                            shape = RoundedCornerShape(16.dp)
                        )
                } else {
                    Modifier
                        .border(
                            width = 1.5.dp,
                            brush = if (enabled) {
                                Brush.horizontalGradient(
                                    colors = listOf(AccentBlue, Color(0xFF06B6D4))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(BorderLight, BorderLight)
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(
                            enabled = enabled && !isLoading,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (variant == ButtonVariant.Primary) BackgroundWhite else AccentBlue,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        variant == ButtonVariant.Primary -> BackgroundWhite
                        !enabled -> TextDisabled
                        else -> AccentBlue
                    }
                )
                
                if (showArrow && variant == ButtonVariant.Primary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BackgroundWhite
                    )
                }
            }
        }
    }
}

/**
 * Text Button - للروابط
 */
@Composable
fun IQTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = AccentBlue
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AccentBlue
        )
    }
}

enum class ButtonVariant {
    Primary,
    Secondary
}
