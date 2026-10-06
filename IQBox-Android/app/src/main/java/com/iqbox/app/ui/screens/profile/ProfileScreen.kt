package com.iqbox.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbox.app.R
import com.iqbox.app.data.AppSettingsManager
import com.iqbox.app.ui.components.PremiumBadge
import com.iqbox.app.ui.theme.*
import com.iqbox.app.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay

/**
 * Profile Screen — Premium cinematic redesign
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    val viewModel = remember { ProfileViewModel(context) }
    val userProfile by viewModel.userProfile.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val user = userProfile?.let {
        User(
            username = it.username,
            email = it.email,
            avatarUrl = null,
            filesCount = userStats?.files_count ?: it.videos_count,
            totalViews = userStats?.total_views ?: 0,
            subscriptionStatus = if (isPremium) "Premium" else "Free",
            subscriptionExpiry = it.subscription_expiry
        )
    } ?: User("...", "", null, 0, 0, "Free", null)

    // Staggered entrance animations
    var showStats by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showEarnings by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150); showStats = true
        delay(100); showAccount = true
        delay(100); showEarnings = true
        delay(100); showSupport = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Cinematic Header with Avatar ──
        ProfileHeroHeader(
            user = user,
            isPremium = isPremium,
            onBackClick = onBackClick,
            onEditProfileClick = onEditProfileClick,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Stats Row ──
        AnimatedVisibility(
            visible = showStats,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            StatsRow(
                filesCount = user.filesCount,
                totalViews = user.totalViews,
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Account Section ──
        AnimatedVisibility(
            visible = showAccount,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            Column {
                SectionLabel(stringResource(R.string.profile_title), AccentBlue, isDarkMode)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsGroup(isDarkMode) {
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        iconGradient = listOf(AccentBlue, AccentPurple),
                        title = stringResource(R.string.edit_profile),
                        subtitle = "Change username & avatar",
                        isDarkMode = isDarkMode,
                        onClick = onEditProfileClick
                    )
                    SettingsDivider(isDarkMode)
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        iconGradient = listOf(AccentBlue, AccentCyan),
                        title = stringResource(R.string.notifications),
                        subtitle = "Manage preferences",
                        isDarkMode = isDarkMode,
                        onClick = onNotificationsClick
                    )
                    SettingsDivider(isDarkMode)
                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        iconGradient = listOf(AccentPurple, Color(0xFF9333EA)),
                        title = stringResource(R.string.dark_mode),
                        subtitle = if (isDarkMode) "Enabled" else "Disabled",
                        isDarkMode = isDarkMode,
                        onClick = { themeState.toggleTheme() },
                        trailing = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { themeState.toggleTheme() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentBlue,
                                    checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                                )
                            )
                        }
                    )
                    SettingsDivider(isDarkMode)
                    run {
                        val localeManager = remember { com.iqbox.app.data.local.LocaleManager(context) }
                        val isArabic = localeManager.isArabic()
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            iconGradient = listOf(Color(0xFF00BCD4), Color(0xFF0097A7)),
                            title = "Language / اللغة",
                            subtitle = if (isArabic) stringResource(R.string.arabic_label) else "English",
                            isDarkMode = isDarkMode,
                            onClick = {
                                localeManager.toggleLanguage()
                                (context as? android.app.Activity)?.recreate()
                            },
                            trailing = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00BCD4).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (isArabic) "EN" else "ع",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00BCD4),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Earnings Section ──
        AnimatedVisibility(
            visible = showEarnings,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            Column {
                SectionLabel(stringResource(R.string.earnings), EarningGreen, isDarkMode)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsGroup(isDarkMode) {
                    SettingsRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        iconGradient = listOf(EarningGreen, Color(0xFF34D399)),
                        title = stringResource(R.string.wallet),
                        subtitle = stringResource(R.string.earnings),
                        isDarkMode = isDarkMode,
                        onClick = onWalletClick
                    )
                    SettingsDivider(isDarkMode)
                    SettingsRow(
                        icon = Icons.Outlined.WorkspacePremium,
                        iconGradient = listOf(PremiumGold, Color(0xFFF97316)),
                        title = stringResource(R.string.subscription),
                        subtitle = if (isPremium) stringResource(R.string.premium_member) else stringResource(R.string.free_member),
                        isDarkMode = isDarkMode,
                        onClick = onSubscriptionClick,
                        trailing = {
                            if (isPremium) {
                                PremiumBadge(small = true)
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.free_member),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Support & Logout Section ──
        AnimatedVisibility(
            visible = showSupport,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            Column {
                val appSettings by AppSettingsManager.settings.collectAsState()

                SectionLabel(stringResource(R.string.help_support), AccentBlue, isDarkMode)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsGroup(isDarkMode) {
                    SettingsRow(
                        icon = Icons.Outlined.HelpOutline,
                        iconGradient = listOf(AccentBlue, AccentCyan),
                        title = stringResource(R.string.help_support),
                        subtitle = stringResource(R.string.faq),
                        isDarkMode = isDarkMode,
                        onClick = onHelpClick
                    )

                    if (!appSettings.support_telegram.isNullOrBlank()) {
                        SettingsDivider(isDarkMode)
                        SettingsRow(
                            icon = Icons.Outlined.Send,
                            iconGradient = listOf(Color(0xFF0088CC), Color(0xFF00BCD4)),
                            title = "Telegram Support",
                            subtitle = appSettings.support_telegram ?: "",
                            isDarkMode = isDarkMode,
                            onClick = {
                                val telegramUrl = if (appSettings.support_telegram!!.startsWith("@")) {
                                    "https://t.me/${appSettings.support_telegram!!.removePrefix("@")}"
                                } else if (appSettings.support_telegram!!.startsWith("http")) {
                                    appSettings.support_telegram!!
                                } else {
                                    "https://t.me/${appSettings.support_telegram}"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logout — separate card for visual distinction
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDarkMode) ErrorColor.copy(alpha = 0.08f) else ErrorColor.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.15f))
                ) {
                    SettingsRow(
                        icon = Icons.Outlined.Logout,
                        iconGradient = listOf(ErrorColor, Color(0xFFEF4444)),
                        title = stringResource(R.string.logout),
                        subtitle = "",
                        isDarkMode = isDarkMode,
                        onClick = onLogoutClick,
                        titleColor = ErrorColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════════════════
// HERO HEADER — Dark cinematic with glowing avatar
// ═══════════════════════════════════════════════════════
@Composable
private fun ProfileHeroHeader(
    user: User,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    isDarkMode: Boolean
) {
    // Always dark cinematic — matches splash/login identity
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120),
                        Color(0xFF131C2E),
                        Color(0xFF1A2540)
                    )
                )
            )
    ) {
        // Ambient blue glow behind avatar
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .offset(y = 20.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.12f),
                            AccentPurple.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        radius = 300f
                    )
                )
        )

        // Subtle top-right accent glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.your_profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Outlined.Edit, "Edit", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact horizontal layout: Avatar + Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with glowing gradient ring
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .shadow(12.dp, CircleShape, spotColor = AccentBlue.copy(alpha = 0.4f))
                            .background(
                                Brush.linearGradient(listOf(AccentBlue, AccentPurple, AccentCyan)),
                                CircleShape
                            )
                            .padding(2.5.dp)
                            .background(Color(0xFF0B1120), CircleShape)
                            .padding(2.5.dp)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F1B2D))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person, null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .shadow(6.dp, CircleShape, spotColor = PremiumGold.copy(alpha = 0.5f))
                                .background(Brush.linearGradient(GradientPremium), CircleShape)
                                .border(2.dp, Color(0xFF0B1120), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Name, email, status badge
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isPremium) PremiumBadge(small = true)
                    }
                    if (user.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val creatorStatus = when {
                        user.totalViews >= 10000 -> Triple("⭐ " + stringResource(R.string.rising_publisher), Color(0xFFFFD700), Color(0xFFF97316))
                        user.totalViews >= 1000 -> Triple(stringResource(R.string.rising_publisher), Color(0xFF34D399), EarningGreen)
                        user.filesCount >= 5 -> Triple(stringResource(R.string.active_publisher), Color(0xFF60A5FA), AccentBlue)
                        user.filesCount >= 1 -> Triple(stringResource(R.string.new_publisher), Color(0xFF34D399), EarningGreen)
                        else -> Triple(stringResource(R.string.start_your_journey), Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.4f))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(creatorStatus.second.copy(alpha = 0.4f), creatorStatus.third.copy(alpha = 0.2f))))
                    ) {
                        Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(creatorStatus.second.copy(alpha = 0.12f), creatorStatus.third.copy(alpha = 0.06f))))) {
                            Text(
                                creatorStatus.first,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = creatorStatus.second,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════
// STATS ROW — gradient icons, accent bar, bolder numbers
// ═══════════════════════════════════════════════════════
@Composable
private fun StatsRow(filesCount: Int, totalViews: Int, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill(
            icon = Icons.Default.Folder,
            value = filesCount.toString(),
            label = stringResource(R.string.stat_files_label),
            gradient = listOf(AccentBlue, AccentPurple),
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.Visibility,
            value = formatViewsCount(totalViews),
            label = stringResource(R.string.stat_views_label),
            gradient = listOf(EarningGreen, Color(0xFF34D399)),
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    gradient: List<Color>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = gradient.first().copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Colored accent bar on the side
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(gradient),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(gradient.map { it.copy(alpha = 0.15f) }),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = gradient.first(), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary
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
}

// ═══════════════════════════════════════════════════════
// SETTINGS COMPONENTS — polished with gradient accents
// ═══════════════════════════════════════════════════════
@Composable
private fun SectionLabel(text: String, accentColor: Color, isDarkMode: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gradient accent dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.5f))),
                    CircleShape
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) DarkTextSecondary else TextSecondary
        )
    }
}

@Composable
private fun SettingsGroup(
    isDarkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AccentBlue.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.6f) else BorderLight)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    titleColor: Color = if (isDarkMode) DarkTextPrimary else TextPrimary,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gradient icon container
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    Brush.linearGradient(iconGradient.map { it.copy(alpha = 0.12f) }),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconGradient.first(), modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }
        trailing?.invoke() ?: Icon(
            Icons.Default.ChevronRight,
            null,
            tint = if (isDarkMode) DarkTextMuted.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDivider(isDarkMode: Boolean) {
    Divider(
        modifier = Modifier.padding(horizontal = 70.dp),
        thickness = 0.5.dp,
        color = if (isDarkMode) DarkBorder.copy(alpha = 0.5f) else DividerColor
    )
}

// ═══════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════
private fun formatViewsCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

data class User(
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val filesCount: Int,
    val totalViews: Int,
    val subscriptionStatus: String,
    val subscriptionExpiry: String?
)
