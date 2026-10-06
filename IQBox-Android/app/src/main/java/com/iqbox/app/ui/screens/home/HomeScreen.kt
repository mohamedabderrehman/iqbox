@file:OptIn(ExperimentalMaterial3Api::class)

package com.iqbox.app.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.AppSettingsManager
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FileItem
import com.iqbox.app.data.api.StorageInfo
import com.iqbox.app.data.api.UserProfile
import com.iqbox.app.data.api.UserStats
import com.iqbox.app.data.api.WalletData
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.components.*
import com.iqbox.app.ui.theme.*
import com.iqbox.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home Screen — Premium Earning Platform Dashboard
 */
@Composable
fun HomeScreen(
    onUploadClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onFileClick: (FileItem) -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onReferralClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    val viewModel = remember { HomeViewModel(context) }
    val userStats by viewModel.userStats.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentNavItem by remember { mutableStateOf(BottomNavItem.Home) }
    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showUploadSheet by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Load user profile data for drawer
    val tokenManager = remember { TokenManager(context) }
    val username by tokenManager.username.collectAsState(initial = null)
    val userEmail by tokenManager.userEmail.collectAsState(initial = null)
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                val storageResp = ApiClient.apiService.getStorageInfo("Bearer $token")
                if (storageResp.isSuccessful) storageInfo = storageResp.body()?.data?.storage
                val profileResp = ApiClient.apiService.getUserProfile("Bearer $token")
                if (profileResp.isSuccessful) userProfile = profileResp.body()?.data?.user
            }
        } catch (_: Exception) {}
    }
    
    // Periodic ad popup for free users (every 3-5 minutes)
    var showAdPopup by remember { mutableStateOf(false) }
    val isFreeUser = true // TODO: check actual subscription status
    
    LaunchedEffect(isFreeUser) {
        if (isFreeUser) {
            while (true) {
                delay((3 * 60 * 1000L) + (Math.random() * 2 * 60 * 1000L).toLong()) // 3-5 min
                showAdPopup = true
            }
        }
    }
    
    // Ad popup dialog
    if (showAdPopup && isFreeUser) {
        AdPopupDialog(
            onDismiss = { showAdPopup = false },
            onRemoveAds = {
                showAdPopup = false
                onSubscriptionClick()
            }
        )
    }

    // Upload BottomSheet
    if (showUploadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUploadSheet = false },
            containerColor = if (isDarkMode) DarkCard else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = context.getString(R.string.upload_file),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Upload File
                Surface(
                    onClick = {
                        showUploadSheet = false
                        onUploadClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkMode) DarkCardElevated else BackgroundGray
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
                                .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                context.getString(R.string.upload_file),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                            Text(
                                context.getString(R.string.choose_file),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) DarkTextMuted else TextMuted
                            )
                        }
                    }
                }
                // Create Folder
                Surface(
                    onClick = {
                        showUploadSheet = false
                        onFilesClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkMode) DarkCardElevated else BackgroundGray
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
                                .background(EarningGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, tint = EarningGreen, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                context.getString(R.string.new_folder),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                            Text(
                                context.getString(R.string.folders),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) DarkTextMuted else TextMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Wallet data
    var walletData by remember { mutableStateOf<WalletData?>(null) }
    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                val resp = ApiClient.apiService.getWallet("Bearer $token")
                if (resp.isSuccessful) walletData = resp.body()?.data
            }
        } catch (_: Exception) {}
    }

    // Delete dialog
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(context.getString(R.string.delete), fontWeight = FontWeight.Bold) },
            text = { Text(context.getString(R.string.delete_file_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteFile(file.id); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorColor)
                ) { Text(context.getString(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(context.getString(R.string.cancel)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Staggered entrance animation states
    var headerVisible by remember { mutableStateOf(false) }
    var heroVisible by remember { mutableStateOf(false) }
    var statsVisible by remember { mutableStateOf(false) }
    var ctaVisible by remember { mutableStateOf(false) }
    var filesVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        delay(100)
        heroVisible = true
        delay(100)
        statsVisible = true
        delay(100)
        ctaVisible = true
        delay(100)
        filesVisible = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                username = userProfile?.username ?: username ?: "",
                email = userProfile?.email ?: userEmail ?: "",
                subscriptionStatus = userProfile?.subscription_status ?: "free",
                storageUsed = storageInfo?.used ?: 0L,
                storageLimit = storageInfo?.limit ?: (10L * 1024 * 1024 * 1024),
                isPremium = storageInfo?.is_premium ?: false,
                isDarkMode = isDarkMode,
                onFilesClick = { scope.launch { drawerState.close() }; onFilesClick() },
                onWalletClick = { scope.launch { drawerState.close() }; onWalletClick() },
                onReferralClick = { scope.launch { drawerState.close() }; onReferralClick() },
                onSubscriptionClick = { scope.launch { drawerState.close() }; onSubscriptionClick() },
                onProfileClick = { scope.launch { drawerState.close() }; onProfileClick() },
                onHelpClick = { scope.launch { drawerState.close() }; onHelpClick() },
                onLogoutClick = { scope.launch { drawerState.close() }; onLogoutClick() }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            containerColor = if (isDarkMode) DarkBackground else BackgroundWhite,
            bottomBar = {
                BottomNavBar(
                    currentRoute = currentNavItem,
                    onItemClick = { item ->
                        currentNavItem = item
                        when (item) {
                            BottomNavItem.Search -> onSearchClick()
                            BottomNavItem.Files -> onFilesClick()
                            BottomNavItem.Profile -> onProfileClick()
                            else -> {}
                        }
                    },
                    onUploadClick = { showUploadSheet = true },
                    isDarkMode = isDarkMode
                )
            }
        ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header ──
            item {
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    )
                ) {
                    HomeHeader(isDarkMode, onProfileClick, onMenuClick = { scope.launch { drawerState.open() } })
                }
            }

            // ── Hero Earnings Card ──
            item {
                AnimatedVisibility(
                    visible = heroVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    HeroEarningsCard(
                        walletData = walletData,
                        isLoading = isLoading,
                        isDarkMode = isDarkMode
                    )
                }
            }

            // ── Quick Actions Grid ──
            item {
                AnimatedVisibility(
                    visible = statsVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    QuickActionsGrid(
                        isDarkMode = isDarkMode,
                        onUploadClick = onUploadClick,
                        onFilesClick = onFilesClick,
                        onWalletClick = onWalletClick,
                        onReferralClick = onReferralClick,
                        onSearchClick = onSearchClick,
                        onSubscriptionClick = onSubscriptionClick
                    )
                }
            }

            // ── Quick Stats Row ──
            item {
                AnimatedVisibility(
                    visible = statsVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    QuickStatsRow(
                        stats = userStats,
                        isLoading = isLoading,
                        isDarkMode = isDarkMode
                    )
                }
            }

            // ── Tips Carousel ──
            item {
                AnimatedVisibility(
                    visible = ctaVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    TipsCarousel(isDarkMode = isDarkMode)
                }
            }

            // ── Call To Action ──
            item {
                AnimatedVisibility(
                    visible = ctaVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    CTASection(
                        onUploadClick = onUploadClick,
                        isDarkMode = isDarkMode
                    )
                }
            }

            // ── Banner Ad (free users only) ──
            if (isFreeUser) {
                item {
                    AnimatedVisibility(
                        visible = ctaVisible,
                        enter = fadeIn(tween(400))
                    ) {
                        BannerAdView(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ── Recent Files ──
            item {
                AnimatedVisibility(
                    visible = filesVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                ) {
                    SectionTitle(
                        title = context.getString(R.string.recent_files),
                        isDarkMode = isDarkMode,
                        actionText = context.getString(R.string.view_all),
                        onAction = onLibraryClick,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            when {
                isLoading -> {
                    items(3) { SkeletonFileCard(isDarkMode = isDarkMode) }
                }
                recentFiles.isEmpty() -> {
                    item {
                        AnimatedVisibility(
                            visible = filesVisible,
                            enter = fadeIn(tween(600)) + slideInVertically(
                                initialOffsetY = { 80 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        ) {
                            EmptyFilesState(onUploadClick, isDarkMode)
                        }
                    }
                }
                else -> {
                    itemsIndexed(recentFiles) { index, file ->
                        AnimatedVisibility(
                            visible = filesVisible,
                            enter = fadeIn(tween(400, delayMillis = index * 80)) + slideInVertically(
                                initialOffsetY = { 60 },
                                animationSpec = tween(400, delayMillis = index * 80, easing = FastOutSlowInEasing)
                            )
                        ) {
                            ModernFileCard(
                                file = file,
                                onClick = { onFileClick(file) },
                                onCopyLink = { file.share_url?.let { copyToClipboard(context, it) } },
                                onShare = { shareFileLink(context, file) },
                                onDelete = { showDeleteDialog = file },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

// ═══════════════════════════════════════════════════════
// HEADER — polished with gradient logo glow + bordered profile
// ═══════════════════════════════════════════════════════
@Composable
private fun HomeHeader(
    isDarkMode: Boolean,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val appSettings by AppSettingsManager.settings.collectAsState()
    
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> stringResource(R.string.greeting_morning)
        currentHour < 17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    val motivationalHint = when {
        currentHour < 12 -> stringResource(R.string.hint_morning)
        currentHour < 17 -> stringResource(R.string.hint_afternoon)
        else -> stringResource(R.string.hint_evening)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        1.dp,
                        if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight,
                        RoundedCornerShape(12.dp)
                    )
                    .background(if (isDarkMode) DarkCard else BackgroundGray, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    Icons.Default.Menu, "Menu",
                    tint = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Logo with glow shadow
            if (!appSettings.app_logo_url.isNullOrBlank()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(appSettings.app_logo_url),
                    contentDescription = appSettings.app_name,
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, RoundedCornerShape(13.dp), spotColor = AccentBlue.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, RoundedCornerShape(13.dp), spotColor = AccentBlue.copy(alpha = 0.3f))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1E40AF), AccentBlue, AccentCyan)
                            ),
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)),
                                RoundedCornerShape(13.dp)
                            )
                    )
                    Text(
                        text = appSettings.app_name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    text = motivationalHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }

        // Profile button with gradient border + online dot
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.5f), AccentPurple.copy(alpha = 0.3f))),
                        CircleShape
                    )
                    .clip(CircleShape)
                    .background(if (isDarkMode) DarkCard else BackgroundGray)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = if (isDarkMode) DarkTextPrimary else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Online indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = (-2).dp, y = 2.dp)
                    .border(2.dp, if (isDarkMode) DarkBackground else BackgroundWhite, CircleShape)
                    .background(Color(0xFF22C55E), CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// HERO EARNINGS CARD - Your earning journey status
// ═══════════════════════════════════════════════════════
@Composable
private fun HeroEarningsCard(
    walletData: WalletData?,
    isLoading: Boolean,
    isDarkMode: Boolean
) {
    val balance = walletData?.wallet?.balance ?: 0.0
    val totalEarned = walletData?.wallet?.total_earned ?: 0.0
    val todayEarnings = walletData?.earnings_breakdown?.views_earnings ?: 0.0
    
    // Determine user's earning status and emotional message
    val (statusMessage, statusColor) = when {
        balance >= 100 -> Pair(stringResource(R.string.balance_excellent), Color(0xFFFFD700))
        balance >= 50 -> Pair(stringResource(R.string.balance_great), Color(0xFF34D399))
        balance >= 10 -> Pair(stringResource(R.string.balance_strong_start), Color(0xFF60A5FA))
        balance > 0 -> Pair(stringResource(R.string.balance_started), Color(0xFF34D399))
        else -> Pair(stringResource(R.string.balance_start_earning), Color.White)
    }
    
    // Progress towards next milestone
    val nextMilestone = when {
        balance < 1 -> 1.0
        balance < 10 -> 10.0
        balance < 50 -> 50.0
        balance < 100 -> 100.0
        balance < 500 -> 500.0
        else -> 1000.0
    }
    val progressToMilestone = (balance / nextMilestone).coerceIn(0.0, 1.0)
    
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        HeroGradientCard(isDarkMode = isDarkMode) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Top row: emotional status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.your_earnings_journey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance with context
                if (isLoading) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(150.dp)
                            .height(40.dp),
                        isDarkMode = true
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", balance)}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 36.sp,
                                letterSpacing = (-1).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (todayEarnings > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF34D399).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "+$${String.format("%.2f", todayEarnings)} اليوم",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Progress to next milestone
                if (balance < 1000 && !isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.next_goal, nextMilestone.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${(progressToMilestone * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progressToMilestone.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-stats row with context
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroSubStat(
                        label = stringResource(R.string.views_earnings),
                        value = "$${String.format("%.2f", walletData?.earnings_breakdown?.views_earnings ?: 0.0)}",
                        icon = Icons.Default.Visibility
                    )
                    HeroSubStat(
                        label = stringResource(R.string.total_journey),
                        value = "$${String.format("%.2f", totalEarned)}",
                        icon = Icons.Default.TrendingUp
                    )
                    HeroSubStat(
                        label = stringResource(R.string.from_referrals),
                        value = "$${String.format("%.2f", walletData?.earnings_breakdown?.referral_earnings ?: 0.0)}",
                        icon = Icons.Default.Group
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSubStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

// ═══════════════════════════════════════════════════════
// QUICK STATS ROW — accent bars, gradient icons, shadows
// ═══════════════════════════════════════════════════════
@Composable
private fun QuickStatsRow(
    stats: UserStats?,
    isLoading: Boolean,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            icon = Icons.Default.Visibility,
            value = if (isLoading) "..." else formatNumber(stats?.total_views ?: 0),
            label = stringResource(R.string.stat_views),
            gradient = listOf(AccentBlue, AccentPurple),
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Folder,
            value = if (isLoading) "..." else (stats?.files_count ?: 0).toString(),
            label = stringResource(R.string.stat_files),
            gradient = listOf(EarningGreen, Color(0xFF34D399)),
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Storage,
            value = if (isLoading) "..." else formatStorage(stats?.total_storage ?: 0),
            label = stringResource(R.string.stat_storage),
            gradient = listOf(PremiumGold, Color(0xFFF97316)),
            isDarkMode = isDarkMode,
            modifier = Modifier.weight(1f)
        )
    }
}

// ═══════════════════════════════════════════════════════
// CTA SECTION — gradient border, glowing icon, premium button
// ═══════════════════════════════════════════════════════
@Composable
private fun CTASection(
    onUploadClick: () -> Unit,
    isDarkMode: Boolean
) {
    val motivationalMessages = listOf(
        Pair(stringResource(R.string.cta_msg1_title), stringResource(R.string.cta_msg1_sub)),
        Pair(stringResource(R.string.cta_msg2_title), stringResource(R.string.cta_msg2_sub)),
        Pair(stringResource(R.string.cta_msg3_title), stringResource(R.string.cta_msg3_sub))
    )
    val currentMessage = motivationalMessages[(System.currentTimeMillis() / 10000 % 3).toInt()]
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AccentBlue.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    AccentBlue.copy(alpha = if (isDarkMode) 0.3f else 0.2f),
                    AccentPurple.copy(alpha = if (isDarkMode) 0.15f else 0.1f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUploadClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Upload icon with gradient + glow shadow
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = AccentBlue.copy(alpha = 0.3f))
                    .background(
                        brush = Brush.linearGradient(listOf(AccentBlue, AccentPurple)),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)),
                            RoundedCornerShape(14.dp)
                        )
                )
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMessage.first,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentMessage.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
            // Gradient upload button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(AccentBlue, AccentPurple)))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.upload_btn_short),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// MODERN FILE CARD — gradient icon, colored shadow, polished actions
// ═══════════════════════════════════════════════════════
@Composable
private fun ModernFileCard(
    file: FileItem,
    onClick: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    isDarkMode: Boolean
) {
    val (fileIcon, fileColor) = getFileIconAndColor(file.file_type)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = fileColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            0.5.dp,
            if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Gradient accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(fileColor, fileColor.copy(alpha = 0.4f))),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient icon background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(fileColor.copy(alpha = 0.12f), fileColor.copy(alpha = 0.05f))
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fileIcon, null, tint = fileColor, modifier = Modifier.size(26.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File size with color
                        Text(
                            text = formatStorage(file.file_size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = fileColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, null,
                                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                                modifier = Modifier.size(13.dp))
                            Text(
                                formatNumber(file.views_count),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkMode) DarkTextMuted else TextMuted
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, null,
                                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                                modifier = Modifier.size(13.dp))
                            Text(
                                formatNumber(file.downloads_count),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkMode) DarkTextMuted else TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Copy button with gradient tint
                        Surface(
                            onClick = onCopyLink,
                            shape = RoundedCornerShape(10.dp),
                            color = AccentBlue.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = AccentBlue)
                                Text(stringResource(R.string.copy_short), style = MaterialTheme.typography.labelSmall, color = AccentBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                        Surface(
                            onClick = onShare,
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkMode) DarkCardElevated else BackgroundGray,
                            border = BorderStroke(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.padding(6.dp).size(16.dp),
                                tint = if (isDarkMode) DarkTextSecondary else TextSecondary)
                        }
                        Surface(
                            onClick = onDelete,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, null, Modifier.padding(6.dp).size(16.dp),
                                tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}

private fun getFileIconAndColor(fileType: String): Pair<ImageVector, Color> {
    return when (fileType) {
        "video" -> Icons.Default.Videocam to Color(0xFFEF4444)
        "image" -> Icons.Default.Image to Color(0xFF10B981)
        "audio" -> Icons.Default.MusicNote to Color(0xFF8B5CF6)
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFF59E0B)
        "document" -> Icons.Default.Description to Color(0xFF3B82F6)
        "archive" -> Icons.Default.FolderZip to Color(0xFF6366F1)
        else -> Icons.Default.InsertDriveFile to Color(0xFF6B7280)
    }
}

// ═══════════════════════════════════════════════════════
// EMPTY STATE - Welcoming first-time experience
// ═══════════════════════════════════════════════════════
@Composable
private fun EmptyFilesState(
    onUploadClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            if (isDarkMode) GradientHeroDark.map { it.copy(alpha = 0.5f) }
                            else GradientPrimary.map { it.copy(alpha = 0.12f) }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Text(
                text = stringResource(R.string.empty_journey_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
            
            Text(
                text = stringResource(R.string.empty_journey_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) DarkTextMuted else TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepItem(
                    number = "1",
                    text = stringResource(R.string.step_upload),
                    isDarkMode = isDarkMode
                )
                StepItem(
                    number = "2",
                    text = stringResource(R.string.step_share),
                    isDarkMode = isDarkMode
                )
                StepItem(
                    number = "3",
                    text = stringResource(R.string.step_earn),
                    isDarkMode = isDarkMode,
                    isHighlighted = true
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(GradientPrimary))
                    .clickable { onUploadClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp), tint = Color.White)
                    Text(stringResource(R.string.start_journey_now), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    text: String,
    isDarkMode: Boolean,
    isHighlighted: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isHighlighted) EarningGreen else AccentBlue.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) Color.White else AccentBlue
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) EarningGreen else if (isDarkMode) DarkTextSecondary else TextSecondary,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════════
// QUICK ACTIONS GRID — 2x3 icon grid for fast navigation
// ═══════════════════════════════════════════════════════

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsGrid(
    isDarkMode: Boolean,
    onUploadClick: () -> Unit,
    onFilesClick: () -> Unit,
    onWalletClick: () -> Unit,
    onReferralClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSubscriptionClick: () -> Unit
) {
    val actions = listOf(
        QuickAction(Icons.Default.CloudUpload, stringResource(R.string.action_upload), listOf(AccentBlue, AccentCyan), onUploadClick),
        QuickAction(Icons.Default.FolderOpen, stringResource(R.string.action_my_files), listOf(AccentPurple, AccentBlue), onFilesClick),
        QuickAction(Icons.Default.AccountBalanceWallet, stringResource(R.string.action_earnings), listOf(EarningGreen, Color(0xFF34D399)), onWalletClick),
        QuickAction(Icons.Default.Share, stringResource(R.string.action_referrals), listOf(Color(0xFFEC4899), AccentPurple), onReferralClick),
        QuickAction(Icons.Default.BarChart, stringResource(R.string.action_statistics), listOf(AccentCyan, AccentBlue), onSearchClick),
        QuickAction(Icons.Default.Payments, stringResource(R.string.action_withdraw), listOf(PremiumGold, Color(0xFFF97316)), onWalletClick)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in actions.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (action in row) {
                    QuickActionCell(
                        action = action,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCell(
    action: QuickAction,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = action.onClick,
        modifier = modifier
            .shadow(
                3.dp,
                RoundedCornerShape(16.dp),
                spotColor = action.gradient.first().copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            0.5.dp,
            if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(
                            action.gradient.map { it.copy(alpha = 0.12f) }
                        ),
                        RoundedCornerShape(13.dp)
                    )
                    .border(
                        0.5.dp,
                        Brush.linearGradient(
                            action.gradient.map { it.copy(alpha = 0.15f) }
                        ),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon, null,
                    tint = action.gradient.first(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                action.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) DarkTextSecondary else TextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// TIPS CAROUSEL — swipeable earning tips
// ═══════════════════════════════════════════════════════

private data class TipItem(
    val icon: ImageVector,
    val textRes: Int,
    val subtitleRes: Int,
    val gradient: List<Color>
)

private val earningTips = listOf(
    TipItem(Icons.Default.CloudUpload, R.string.tip1_text, R.string.tip1_sub, listOf(AccentBlue, AccentCyan)),
    TipItem(Icons.Default.TrendingUp, R.string.tip2_text, R.string.tip2_sub, listOf(EarningGreen, Color(0xFF34D399))),
    TipItem(Icons.Default.Share, R.string.tip3_text, R.string.tip3_sub, listOf(AccentPurple, Color(0xFFEC4899))),
    TipItem(Icons.Default.Visibility, R.string.tip4_text, R.string.tip4_sub, listOf(PremiumGold, Color(0xFFF97316))),
    TipItem(Icons.Default.Group, R.string.tip5_text, R.string.tip5_sub, listOf(Color(0xFFEC4899), AccentPurple)),
    TipItem(Icons.Default.Videocam, R.string.tip6_text, R.string.tip6_sub, listOf(Color(0xFFEF4444), Color(0xFFF97316))),
    TipItem(Icons.Default.Edit, R.string.tip7_text, R.string.tip7_sub, listOf(AccentCyan, AccentBlue)),
    TipItem(Icons.Default.AccountBalanceWallet, R.string.tip8_text, R.string.tip8_sub, listOf(EarningGreen, Color(0xFF059669)))
)

@Composable
private fun TipsCarousel(isDarkMode: Boolean) {
    Column {
        SectionTitle(
            title = stringResource(R.string.tips_title),
            isDarkMode = isDarkMode,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(earningTips) { index, tip ->
                TipCard(
                    tip = tip,
                    index = index,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
private fun TipCard(
    tip: TipItem,
    index: Int,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .shadow(
                3.dp,
                RoundedCornerShape(18.dp),
                spotColor = tip.gradient.first().copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            0.5.dp,
            if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(tip.gradient),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    tip.gradient.map { it.copy(alpha = 0.12f) }
                                ),
                                RoundedCornerShape(11.dp)
                            )
                            .border(
                                0.5.dp,
                                Brush.linearGradient(
                                    tip.gradient.map { it.copy(alpha = 0.15f) }
                                ),
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tip.icon, null,
                            tint = tip.gradient.first(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tip.gradient.first().copy(alpha = 0.08f)
                    ) {
                        Text(
                            "#${index + 1}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tip.gradient.first()
                        )
                    }
                }
                Text(
                    stringResource(tip.textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    maxLines = 2
                )
                Text(
                    stringResource(tip.subtitleRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                    maxLines = 2
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════
private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}

private fun formatStorage(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1fGB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1fMB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.0fKB", bytes / 1_024.0)
        else -> "${bytes}B"
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("File Link", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
}

private fun shareFileLink(context: Context, file: FileItem) {
    val url = file.share_url ?: return
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
        putExtra(android.content.Intent.EXTRA_TEXT, "${file.name}\n$url")
    }
    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.share_file)))
}

