package com.iqbox.app.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.theme.*

@Composable
fun AppDrawerContent(
    username: String,
    email: String,
    subscriptionStatus: String,
    storageUsed: Long,
    storageLimit: Long,
    isPremium: Boolean,
    isDarkMode: Boolean,
    onFilesClick: () -> Unit,
    onWalletClick: () -> Unit,
    onReferralClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = if (isDarkMode) DarkSurface else Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            // Account header
            DrawerAccountHeader(
                username = username,
                email = email,
                subscriptionStatus = subscriptionStatus,
                isDarkMode = isDarkMode,
                onProfileClick = onProfileClick
            )

            // Storage bar
            DrawerStorageBar(
                storageUsed = storageUsed,
                storageLimit = storageLimit,
                isPremium = isPremium,
                isDarkMode = isDarkMode
            )

            // Premium upgrade banner (only for free users)
            if (subscriptionStatus == "free") {
                DrawerPremiumBanner(
                    isDarkMode = isDarkMode,
                    onUpgradeClick = onSubscriptionClick
                )
            }

            Spacer(Modifier.height(4.dp))
            Divider(
                color = if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))

            // Main menu items
            DrawerMenuItem(
                icon = Icons.Outlined.FolderOpen,
                label = stringResource(R.string.drawer_my_files),
                color = AccentBlue,
                isDarkMode = isDarkMode,
                onClick = onFilesClick
            )
            DrawerMenuItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = stringResource(R.string.drawer_wallet),
                color = EarningGreen,
                isDarkMode = isDarkMode,
                onClick = onWalletClick
            )
            DrawerMenuItem(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.drawer_referrals),
                color = AccentPurple,
                isDarkMode = isDarkMode,
                onClick = onReferralClick
            )
            DrawerMenuItem(
                icon = Icons.Outlined.WorkspacePremium,
                label = stringResource(R.string.drawer_subscription),
                color = PremiumGold,
                isDarkMode = isDarkMode,
                onClick = onSubscriptionClick
            )

            Spacer(Modifier.height(4.dp))
            Divider(
                color = if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))

            // Footer items
            DrawerMenuItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.drawer_settings),
                color = if (isDarkMode) DarkTextSecondary else TextSecondary,
                isDarkMode = isDarkMode,
                onClick = onProfileClick
            )
            DrawerMenuItem(
                icon = Icons.Outlined.HelpOutline,
                label = stringResource(R.string.drawer_help),
                color = if (isDarkMode) DarkTextSecondary else TextSecondary,
                isDarkMode = isDarkMode,
                onClick = onHelpClick
            )
            DrawerMenuItem(
                icon = Icons.Outlined.Logout,
                label = stringResource(R.string.drawer_logout),
                color = ErrorColor,
                isDarkMode = isDarkMode,
                onClick = onLogoutClick
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerAccountHeader(
    username: String,
    email: String,
    subscriptionStatus: String,
    isDarkMode: Boolean,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    if (isDarkMode) listOf(DarkCard, DarkSurface)
                    else listOf(BackgroundGray, Color.White)
                )
            )
            .statusBarsPadding()
            .clickable { onProfileClick() }
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(4.dp, CircleShape, spotColor = AccentBlue.copy(alpha = 0.15f))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(GradientPrimary),
                        CircleShape
                    )
                    .background(
                        if (isDarkMode) DarkCardElevated else BackgroundGray,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username.ifBlank { "---" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = email.ifBlank { "---" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Subscription badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (subscriptionStatus == "premium") PremiumGold.copy(alpha = 0.12f)
            else AccentBlue.copy(alpha = 0.08f),
            border = BorderStroke(
                0.5.dp,
                if (subscriptionStatus == "premium") PremiumGold.copy(alpha = 0.2f)
                else AccentBlue.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (subscriptionStatus == "premium") Icons.Default.WorkspacePremium
                    else Icons.Default.Person,
                    null,
                    tint = if (subscriptionStatus == "premium") PremiumGold else AccentBlue,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (subscriptionStatus == "premium") stringResource(R.string.premium_label) else stringResource(R.string.free_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (subscriptionStatus == "premium") PremiumGold else AccentBlue
                )
            }
        }
    }
}

@Composable
private fun DrawerStorageBar(
    storageUsed: Long,
    storageLimit: Long,
    isPremium: Boolean,
    isDarkMode: Boolean
) {
    val percentage = if (storageLimit > 0) (storageUsed.toFloat() / storageLimit.toFloat()).coerceIn(0f, 1f) else 0f
    val usedStr = formatStorageSize(storageUsed)
    val limitStr = formatStorageSize(storageLimit)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$usedStr / $limitStr",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
            Text(
                stringResource(R.string.drawer_manage_storage),
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isDarkMode) DarkBorder else BorderLight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (percentage > 0.8f) listOf(ErrorColor, Color(0xFFEF4444))
                            else if (percentage > 0.6f) listOf(WarningColor, PremiumGold)
                            else GradientPrimary
                        )
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.drawer_available, formatStorageSize(storageLimit - storageUsed)),
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkMode) DarkTextMuted else TextMuted
        )
    }
}

@Composable
private fun DrawerPremiumBanner(
    isDarkMode: Boolean,
    onUpgradeClick: () -> Unit
) {
    Surface(
        onClick = onUpgradeClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(GradientHeroDark),
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.drawer_premium_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.drawer_premium_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PremiumGold
                ) {
                    Text(
                        stringResource(R.string.drawer_upgrade),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    color: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight, null,
                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes >= 1073741824L -> String.format("%.1f GB", bytes / 1073741824.0)
        bytes >= 1048576L -> String.format("%.1f MB", bytes / 1048576.0)
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
