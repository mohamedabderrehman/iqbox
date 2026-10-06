package com.iqbox.app.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.api.*
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    val scope = rememberCoroutineScope()
    
    val tokenManager = remember { TokenManager(context) }
    val apiService = ApiClient.apiService
    
    var referralCode by remember { mutableStateOf("") }
    var referralLink by remember { mutableStateOf("") }
    var referralPercentage by remember { mutableStateOf(0.0) }
    var stats by remember { mutableStateOf<ReferralStats?>(null) }
    var referredUsers by remember { mutableStateOf<List<ReferredUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var copiedMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                // جلب كود الإحالة
                val codeResponse = apiService.getReferralCode("Bearer $token")
                if (codeResponse.isSuccessful && codeResponse.body()?.success == true) {
                    referralCode = codeResponse.body()!!.data.referral_code
                    referralLink = codeResponse.body()!!.data.referral_link
                    referralPercentage = codeResponse.body()!!.data.referral_percentage
                }
                
                // جلب الإحصائيات
                val statsResponse = apiService.getReferralStats("Bearer $token")
                if (statsResponse.isSuccessful && statsResponse.body()?.success == true) {
                    stats = statsResponse.body()!!.data
                }
                
                // جلب المستخدمين المُحالين
                val usersResponse = apiService.getReferredUsers("Bearer $token")
                if (usersResponse.isSuccessful && usersResponse.body()?.success == true) {
                    referredUsers = usersResponse.body()!!.data.referred_users
                }
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }
    
    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        copiedMessage = context.getString(R.string.code_copied)
        scope.launch {
            kotlinx.coroutines.delay(2000)
            copiedMessage = null
        }
    }
    
    fun shareReferralLink() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.referral_share_text, referralLink))
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_referral)))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        // Header
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
                    tint = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(R.string.referral_program), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary)
                Text(stringResource(R.string.earn_from_friends), style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted)
            }
        }

        if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Referral Code Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = context.getString(R.string.your_referral_code),
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = referralCode,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 4.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = stringResource(R.string.get_referral_percent, referralPercentage.toInt()),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { copyToClipboard(referralCode, context.getString(R.string.code_label)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(context.getString(R.string.copy_code))
                                        }
                                        
                                        Button(
                                            onClick = { shareReferralLink() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = null,
                                                tint = Color(0xFF8B5CF6)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.share_short_label),
                                                color = Color(0xFF8B5CF6)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Copied Message
                    copiedMessage?.let { message ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(16.dp),
                                    color = SuccessColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    // Stats Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = stringResource(R.string.total_referrals_label),
                                value = "${stats?.total_referrals ?: 0}",
                                icon = Icons.Default.People,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.total_earnings_label),
                                value = "$${String.format("%.2f", stats?.total_earnings ?: 0.0)}",
                                icon = Icons.Default.AttachMoney,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        StatCard(
                            title = stringResource(R.string.month_earnings_label),
                            value = "$${String.format("%.2f", stats?.month_earnings ?: 0.0)}",
                            icon = Icons.Default.TrendingUp,
                            isDarkMode = isDarkMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Referred Users
                    item {
                        Text(
                            text = stringResource(R.string.referred_users_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                    }
                    
                    if (referredUsers.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkMode) DarkCard else CardBackground
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = if (isDarkMode) DarkTextMuted else TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.no_referred_users),
                                        color = if (isDarkMode) DarkTextMuted else TextMuted
                                    )
                                    Text(
                                        text = stringResource(R.string.share_code_to_earn),
                                        fontSize = 12.sp,
                                        color = if (isDarkMode) DarkTextMuted else TextMuted
                                    )
                                }
                            }
                        }
                    } else {
                        items(referredUsers) { user ->
                            ReferredUserCard(
                                user = user,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentBlue.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = if (isDarkMode) DarkTextSecondary else TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ReferredUserCard(
    user: ReferredUser,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AccentPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    text = stringResource(R.string.joined_date, user.joined_at.take(10)),
                    fontSize = 12.sp,
                    color = if (isDarkMode) DarkTextSecondary else TextSecondary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+$${String.format("%.2f", user.total_earnings)}",
                    fontWeight = FontWeight.Bold,
                    color = SuccessColor
                )
                Text(
                    text = stringResource(R.string.your_earnings_from),
                    fontSize = 10.sp,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }
    }
}
