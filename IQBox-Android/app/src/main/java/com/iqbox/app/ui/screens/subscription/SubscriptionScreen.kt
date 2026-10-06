package com.iqbox.app.ui.screens.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.components.PremiumBadge
import com.iqbox.app.ui.theme.*
import com.iqbox.app.ui.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    
    val viewModel = remember { SubscriptionViewModel(context) }
    val isPremium by viewModel.isPremium.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val weeklyDetails by viewModel.weeklyDetails.collectAsState()
    val monthlyDetails by viewModel.monthlyDetails.collectAsState()
    val purchaseMessage by viewModel.purchaseMessage.collectAsState()
    
    var selectedPlan by remember { mutableStateOf("monthly") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                Spacer(modifier = Modifier.width(14.dp))
                Text(stringResource(R.string.subscription_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary)
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Premium banner
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
                                    brush = Brush.linearGradient(GradientPremium),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(28.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.WorkspacePremium, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                if (isPremium) {
                                    Text(
                                        stringResource(R.string.already_subscribed),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        stringResource(R.string.enjoy_no_ads),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.iqbox_premium),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        stringResource(R.string.premium_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Message
                purchaseMessage?.let { msg ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.contains("نجاح")) SuccessColor.copy(alpha = 0.1f)
                                else WarningColor.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (msg.contains("نجاح")) Icons.Default.CheckCircle else Icons.Default.Info,
                                    null,
                                    tint = if (msg.contains("نجاح")) SuccessColor else WarningColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(msg, style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearMessage() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                                        tint = if (isDarkMode) DarkTextMuted else TextMuted)
                                }
                            }
                        }
                    }
                }
                
                // Features
                if (!isPremium) {
                    item {
                        Text(
                            stringResource(R.string.subscription_features),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                    }
                    
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) DarkCard else CardBackground,
                            border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                FeatureRow(
                                    icon = Icons.Default.Block,
                                    text = stringResource(R.string.feature_no_ads),
                                    isDarkMode = isDarkMode
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                FeatureRow(
                                    icon = Icons.Default.Download,
                                    text = stringResource(R.string.feature_download),
                                    isDarkMode = isDarkMode
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                FeatureRow(
                                    icon = Icons.Default.Speed,
                                    text = stringResource(R.string.feature_fast),
                                    isDarkMode = isDarkMode
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                FeatureRow(
                                    icon = Icons.Default.Star,
                                    text = stringResource(R.string.feature_support),
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                    
                    // Plans
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.choose_plan),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                    }
                    
                    // Weekly Plan
                    item {
                        PlanCard(
                            title = stringResource(R.string.weekly),
                            price = weeklyDetails?.subscriptionOfferDetails?.firstOrNull()
                                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$1.99",
                            period = stringResource(R.string.per_week),
                            isSelected = selectedPlan == "weekly",
                            isBestValue = false,
                            onClick = { selectedPlan = "weekly" },
                            isDarkMode = isDarkMode
                        )
                    }
                    
                    // Monthly Plan
                    item {
                        PlanCard(
                            title = stringResource(R.string.monthly),
                            price = monthlyDetails?.subscriptionOfferDetails?.firstOrNull()
                                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99",
                            period = stringResource(R.string.per_month),
                            isSelected = selectedPlan == "monthly",
                            isBestValue = true,
                            onClick = { selectedPlan = "monthly" },
                            isDarkMode = isDarkMode
                        )
                    }
                    
                    // Subscribe button
                    item {
                        Button(
                            onClick = {
                                activity?.let { act ->
                                    if (selectedPlan == "weekly") viewModel.purchaseWeekly(act)
                                    else viewModel.purchaseMonthly(act)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isPremium
                        ) {
                            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.subscribe_google_pay),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    // Note
                    item {
                        Text(
                            stringResource(R.string.subscription_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Subscription Terms
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = if (isDarkMode) DarkBorder else BorderLight)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                R.string.subscription_note_1,
                                R.string.subscription_note_2,
                                R.string.subscription_note_3,
                                R.string.subscription_note_4
                            ).forEach { noteRes ->
                                Text(
                                    text = stringResource(noteRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                                    lineHeight = 18.sp
                                )
                            }
                            
                            // Note 5 with clickable Terms & Privacy links
                            val note5Text = buildAnnotatedString {
                                append("5. By subscribing, you agree to IqBox ")
                                pushStringAnnotation(tag = "TOS", annotation = "https://iqbox.site/termsofuse.html")
                                withStyle(SpanStyle(color = AccentBlue, textDecoration = TextDecoration.Underline)) {
                                    append(stringResource(R.string.terms_of_service))
                                }
                                pop()
                                append(" and ")
                                pushStringAnnotation(tag = "PRIVACY", annotation = "https://iqbox.site/privacy_policy.html")
                                withStyle(SpanStyle(color = AccentBlue, textDecoration = TextDecoration.Underline)) {
                                    append(stringResource(R.string.privacy_policy))
                                }
                                pop()
                                append(".")
                            }
                            ClickableText(
                                text = note5Text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                                    lineHeight = 18.sp
                                ),
                                onClick = { offset ->
                                    note5Text.getStringAnnotations("TOS", offset, offset).firstOrNull()?.let {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                                    }
                                    note5Text.getStringAnnotations("PRIVACY", offset, offset).firstOrNull()?.let {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                                    }
                                }
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = AccentBlue.copy(alpha = 0.3f)
                ) else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            if (isDarkMode) AccentBlue.copy(alpha = 0.08f) else AccentBlue.copy(alpha = 0.04f)
        } else {
            if (isDarkMode) DarkCard else CardBackground
        },
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AccentBlue else if (isDarkMode) DarkBorder else BorderLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary
                    )
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = EarningGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.best_value),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = EarningGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    period,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDarkMode: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.linearGradient(
                        listOf(AccentBlue.copy(alpha = 0.15f), AccentPurple.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EarningGreen,
            modifier = Modifier.size(20.dp)
        )
    }
}
