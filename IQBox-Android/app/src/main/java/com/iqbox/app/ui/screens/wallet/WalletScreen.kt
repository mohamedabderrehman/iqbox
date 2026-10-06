package com.iqbox.app.ui.screens.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.api.*
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.components.*
import com.iqbox.app.ui.theme.*

/**
 * Wallet Screen — Financial dashboard redesign
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
    onReferralClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    val tokenManager = remember { TokenManager(context) }
    val apiService = ApiClient.apiService

    var wallet by remember { mutableStateOf<Wallet?>(null) }
    var earningsBreakdown by remember { mutableStateOf<EarningsBreakdown?>(null) }
    var transactions by remember { mutableStateOf<List<WalletTransaction>>(emptyList()) }
    var earningsStats by remember { mutableStateOf<EarningsStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                val walletResponse = apiService.getWallet("Bearer $token")
                if (walletResponse.isSuccessful && walletResponse.body()?.success == true) {
                    wallet = walletResponse.body()!!.data.wallet
                    earningsBreakdown = walletResponse.body()!!.data.earnings_breakdown
                    transactions = walletResponse.body()!!.data.recent_transactions
                }
                val earningsResponse = apiService.getEarningsStats("Bearer $token")
                if (earningsResponse.isSuccessful && earningsResponse.body()?.success == true) {
                    earningsStats = earningsResponse.body()!!.data
                }
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isDarkMode) DarkCard else BackgroundGray, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, "Back",
                    tint = if (isDarkMode) DarkTextPrimary else TextPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(R.string.wallet_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    stringResource(R.string.wallet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Hero Balance ──
                item { BalanceHeroCard(wallet, onWithdrawClick, isDarkMode) }

                // ── Quick Actions ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletActionCard(
                            icon = Icons.Outlined.Group,
                            label = stringResource(R.string.drawer_referrals),
                            color = AccentPurple,
                            onClick = onReferralClick,
                            isDarkMode = isDarkMode,
                            modifier = Modifier.weight(1f)
                        )
                        WalletActionCard(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = context.getString(R.string.withdraw),
                            color = EarningGreen,
                            onClick = onWithdrawClick,
                            isDarkMode = isDarkMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Earnings Breakdown ──
                item { EarningsBreakdownCard(earningsStats, earningsBreakdown, isDarkMode) }

                // ── Transactions ──
                item {
                    SectionTitle(
                        title = context.getString(R.string.transactions),
                        isDarkMode = isDarkMode,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDarkMode) DarkCard else CardBackground,
                            border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isDarkMode) DarkTextMuted else TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.no_transactions_yet),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.empty_state_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(transactions) { tx ->
                        TransactionRow(tx, isDarkMode)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// BALANCE HERO CARD
// ═══════════════════════════════════════════════════════
@Composable
private fun BalanceHeroCard(wallet: Wallet?, onWithdrawClick: () -> Unit, isDarkMode: Boolean) {
    val context = LocalContext.current
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        HeroGradientCard(isDarkMode = isDarkMode) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(context.getString(R.string.available_balance), style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null,
                                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.wallet_screen_title), style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "$${String.format("%.2f", wallet?.balance ?: 0.0)}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 38.sp, letterSpacing = (-1).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(18.dp))

                // Sub stats
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BalanceSubStat(context.getString(R.string.total_earnings), "$${String.format("%.2f", wallet?.total_earned ?: 0.0)}")
                    BalanceSubStat(context.getString(R.string.withdraw), "$${String.format("%.2f", wallet?.total_withdrawn ?: 0.0)}")
                    BalanceSubStat(context.getString(R.string.pending), "$${String.format("%.2f", wallet?.pending_withdrawal ?: 0.0)}")
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onWithdrawClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(context.getString(R.string.withdraw), color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BalanceSubStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ═══════════════════════════════════════════════════════
// QUICK ACTION CARD
// ═══════════════════════════════════════════════════════
@Composable
private fun WalletActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// EARNINGS BREAKDOWN
// ═══════════════════════════════════════════════════════
@Composable
private fun EarningsBreakdownCard(
    stats: EarningsStats?,
    breakdown: EarningsBreakdown?,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                context.getString(R.string.earnings_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )

            Spacer(Modifier.height(16.dp))

            // Time-based stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EarningTimeStat(stringResource(R.string.today_earnings), "$${String.format("%.2f", stats?.today ?: 0.0)}", isDarkMode)
                EarningTimeStat(stringResource(R.string.week_earnings), "$${String.format("%.2f", stats?.this_week ?: 0.0)}", isDarkMode)
                EarningTimeStat(stringResource(R.string.month_earnings), "$${String.format("%.2f", stats?.this_month ?: 0.0)}", isDarkMode)
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = if (isDarkMode) DarkBorder else DividerColor)
            Spacer(Modifier.height(16.dp))

            // Breakdown
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownChip(
                    icon = Icons.Default.Visibility,
                    label = stringResource(R.string.views_income),
                    value = "$${String.format("%.2f", breakdown?.views_earnings ?: 0.0)}",
                    color = EarningGreen,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
                BreakdownChip(
                    icon = Icons.Default.Group,
                    label = stringResource(R.string.referral_income),
                    value = "$${String.format("%.2f", breakdown?.referral_earnings ?: 0.0)}",
                    color = AccentPurple,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // CPM info
            Surface(
                color = EarningGreenBg.copy(alpha = if (isDarkMode) 0.15f else 1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, null, tint = EarningGreen, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.cpm_rate, String.format("%.2f", stats?.earning_rate_per_1000 ?: 0.0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = EarningGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningTimeStat(label: String, value: String, isDarkMode: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isDarkMode) DarkTextMuted else TextMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary)
    }
}

@Composable
private fun BreakdownChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = if (isDarkMode) 0.1f else 0.06f)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Text(value, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// TRANSACTION ROW
// ═══════════════════════════════════════════════════════
@Composable
private fun TransactionRow(transaction: WalletTransaction, isDarkMode: Boolean) {
    val isPositive = transaction.amount > 0
    val viewsIncomeLabel = stringResource(R.string.views_income)
    val referralIncomeLabel = stringResource(R.string.referral_income)
    val withdrawalLabel = stringResource(R.string.withdrawal_label)
    val (typeLabel, typeIcon, typeColor) = when (transaction.type) {
        "view_earning" -> Triple(viewsIncomeLabel, Icons.Default.Visibility, EarningGreen)
        "referral_earning" -> Triple(referralIncomeLabel, Icons.Default.Group, AccentPurple)
        "withdrawal" -> Triple(withdrawalLabel, Icons.Default.ArrowUpward, ErrorColor)
        else -> Triple(transaction.type, Icons.Default.SwapHoriz, AccentBlue)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(typeColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(20.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    typeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    transaction.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                    maxLines = 1
                )
            }

            Text(
                "${if (isPositive) "+" else ""}$${String.format("%.4f", transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) EarningGreen else ErrorColor
            )
        }
    }
}
