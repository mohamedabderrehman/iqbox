package com.iqbox.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbox.app.data.AppSettingsManager
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.components.AppLogo
import com.iqbox.app.ui.components.LogoSize
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Splash Screen — cinematic entrance with orbital rings and glow
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var startAnimation by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (showTagline) 1f else 0f,
        animationSpec = tween(600),
        label = "tagline"
    )

    // Orbital ring rotation
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "ring"
    )
    val ring2Rotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "ring2"
    )
    // Pulsing glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(500)
        showTagline = true

        AppSettingsManager.loadSettings()
        delay(1800)

        val isLoggedIn = tokenManager.isLoggedIn.first()
        if (isLoggedIn) onNavigateToHome() else onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120),
                        Color(0xFF131C2E),
                        Color(0xFF0B1120)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(glowScale)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.25f),
                            AccentPurple.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Orbital rings
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .alpha(alphaAnim * 0.5f)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Ring 1 — dashed arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccentBlue.copy(alpha = 0.6f),
                        Color.Transparent,
                        AccentPurple.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                startAngle = ringRotation,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Ring 2 — outer, opposite direction
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        AccentCyan.copy(alpha = 0.3f),
                        Color.Transparent,
                        AccentBlue.copy(alpha = 0.2f)
                    )
                ),
                startAngle = ring2Rotation,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(-20f, -20f),
                size = androidx.compose.ui.geometry.Size(size.width + 40f, size.height + 40f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )

            // Orbiting dot on ring 1
            val dotAngle = Math.toRadians((ringRotation + 60).toDouble())
            val dotX = center.x + (radius - 1) * cos(dotAngle).toFloat()
            val dotY = center.y + (radius - 1) * sin(dotAngle).toFloat()
            drawCircle(
                color = AccentBlue,
                radius = 4f,
                center = Offset(dotX, dotY)
            )
        }

        // Logo + text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim)
        ) {
            AppLogo(
                isDarkMode = true,
                size = LogoSize.Large
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cloud Storage & Earning Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkTextMuted,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }

        // Bottom loading bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .alpha(alphaAnim)
        ) {
            PulseLoadingBar()
        }

        // Version text
        Text(
            text = "v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = DarkTextMuted.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(taglineAlpha)
        )
    }
}

@Composable
private fun PulseLoadingBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = Modifier
            .width(120.dp)
            .height(3.dp)
    ) {
        // Track
        drawRoundRect(
            color = DarkBorder,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        // Moving indicator
        val indicatorWidth = size.width * 0.35f
        val x = (size.width - indicatorWidth) * progress
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(AccentBlue, AccentCyan),
                startX = x,
                endX = x + indicatorWidth
            ),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(indicatorWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
    }
}
