package com.iqbox.app.ui.screens.files

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.ads.AdManager
import com.iqbox.app.ui.components.MediumRectangleAdView
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Ad Gate Screen — Shows interstitial ad on entry, then a 5-second countdown
 * with a medium rectangle (300x250) AdMob ad before viewing video/image files.
 */
@Composable
fun AdGateScreen(
    fileName: String,
    fileType: String,
    onCountdownComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    // Show interstitial ad immediately on entry
    var interstitialShown by remember { mutableStateOf(false) }
    var countdownStarted by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        if (activity != null && !interstitialShown) {
            interstitialShown = true
            AdManager.showInterstitial(activity) {
                countdownStarted = true
            }
            // If interstitial wasn't available, start countdown anyway
            if (!AdManager.isInterstitialReady()) {
                countdownStarted = true
            }
            // Preload next interstitial
            AdManager.loadInterstitial(context)
        } else {
            countdownStarted = true
        }
    }

    // Countdown timer
    LaunchedEffect(countdownStarted) {
        if (countdownStarted) {
            while (secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            onCountdownComplete()
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (countdownStarted) secondsRemaining / 5f else 1f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "countdown_progress"
    )

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val fileIcon = when (fileType) {
        "video" -> Icons.Default.Videocam
        "image" -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
    val fileColor = when (fileType) {
        "video" -> Color(0xFFEF4444)
        "image" -> Color(0xFF10B981)
        else -> AccentBlue
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        1.dp,
                        if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight,
                        CircleShape
                    )
                    .background(
                        if (isDarkMode) DarkCard else BackgroundGray,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (countdownStarted) stringResource(R.string.ad_preparing) else stringResource(R.string.ad_please_wait),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Countdown circle with pulsing ring
            Box(contentAlignment = Alignment.Center) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size((130 * pulseScale).dp)
                        .border(
                            2.dp,
                            fileColor.copy(alpha = pulseAlpha),
                            CircleShape
                        )
                )
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(110.dp),
                    strokeWidth = 5.dp,
                    color = fileColor,
                    trackColor = if (isDarkMode) DarkBorder else BorderLight,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // File icon inside circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp), spotColor = fileColor.copy(alpha = 0.2f))
                            .background(
                                Brush.linearGradient(listOf(fileColor.copy(alpha = 0.15f), fileColor.copy(alpha = 0.05f))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(fileIcon, null, tint = fileColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    if (countdownStarted) {
                        Text(
                            text = "$secondsRemaining",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = fileColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.ad_file_opens_after),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkMode) DarkTextMuted else TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Action button
            if (secondsRemaining <= 0 && countdownStarted) {
                Button(
                    onClick = onCountdownComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(fileColor, fileColor.copy(alpha = 0.8f))),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp), tint = Color.White)
                            Text(stringResource(R.string.ad_watch_now), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else if (countdownStarted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) DarkCard else BackgroundGray,
                    border = BorderStroke(
                        0.5.dp,
                        if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight
                    )
                ) {
                    Text(
                        stringResource(R.string.ad_wait_seconds, secondsRemaining),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkMode) DarkTextMuted else TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Medium Rectangle Ad (300x250) — large ad format
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkMode) DarkCard else CardBackground,
                border = BorderStroke(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MediumRectangleAdView(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Navigation bar padding
        Spacer(Modifier.navigationBarsPadding())
    }
}
