package com.iqbox.app.ui.screens.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun WithdrawScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    val scope = rememberCoroutineScope()
    
    val tokenManager = remember { TokenManager(context) }
    val apiService = ApiClient.apiService
    
    var balance by remember { mutableStateOf(0.0) }
    var minAmount by remember { mutableStateOf(5.0) }
    var methods by remember { mutableStateOf<List<WithdrawalMethod>>(emptyList()) }
    var withdrawalRequests by remember { mutableStateOf<List<WithdrawalRequestItem>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<WithdrawalMethod?>(null) }
    var amount by remember { mutableStateOf("") }
    val accountDetails = remember { mutableStateMapOf<String, String>() }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    fun loadData() {
        scope.launch {
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    // Ø¬Ù„Ø¨ Ø§Ù„Ø±ØµÙŠØ¯
                    val walletResponse = apiService.getWallet("Bearer $token")
                    if (walletResponse.isSuccessful) {
                        balance = walletResponse.body()?.data?.wallet?.balance ?: 0.0
                    }
                    
                    // Ø¬Ù„Ø¨ Ø·Ø±Ù‚ Ø§Ù„Ø³Ø­Ø¨
                    val methodsResponse = apiService.getWithdrawalMethods("Bearer $token")
                    if (methodsResponse.isSuccessful) {
                        methods = methodsResponse.body()?.data?.methods ?: emptyList()
                        minAmount = methodsResponse.body()?.data?.min_withdrawal_amount ?: 5.0
                    }
                    
                    // Ø¬Ù„Ø¨ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ø³Ø­Ø¨ Ø§Ù„Ø³Ø§Ø¨Ù‚Ø©
                    val requestsResponse = apiService.getWithdrawalRequests("Bearer $token")
                    if (requestsResponse.isSuccessful) {
                        withdrawalRequests = requestsResponse.body()?.data?.requests ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.failed_load_data)
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadData()
    }
    
    fun submitWithdrawal() {
        if (selectedMethod == null) {
            errorMessage = context.getString(R.string.withdraw_method)
            return
        }
        
        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue < minAmount) {
            errorMessage = context.getString(R.string.min_withdraw_error, minAmount.toString())
            return
        }
        
        if (amountValue > balance) {
            errorMessage = context.getString(R.string.balance_not_enough)
            return
        }
        
        // Ø§Ù„ØªØ­Ù‚Ù‚ Ù…Ù† Ù…Ù„Ø¡ Ø¬Ù…ÙŠØ¹ Ø§Ù„Ø­Ù‚ÙˆÙ„
        selectedMethod?.fields?.forEach { field ->
            if (accountDetails[field.key].isNullOrBlank()) {
                errorMessage = context.getString(R.string.fill_all_fields)
                return
            }
        }
        
        scope.launch {
            isSubmitting = true
            errorMessage = null
            
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    val response = apiService.requestWithdrawal(
                        "Bearer $token",
                        WithdrawalRequest(
                            amount = amountValue,
                            method = selectedMethod!!.id,
                            account_details = accountDetails.toMap()
                        )
                    )
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        successMessage = context.getString(R.string.withdraw_success)
                        amount = ""
                        accountDetails.clear()
                        selectedMethod = null
                        loadData()
                    } else {
                        errorMessage = response.body()?.message ?: context.getString(R.string.failed_submit_request)
                    }
                }
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.connection_error)
            } finally {
                isSubmitting = false
            }
        }
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
                Text(context.getString(R.string.withdraw_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary)
                Text(stringResource(R.string.convert_earnings), style = MaterialTheme.typography.bodySmall,
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
                    // Balance Info
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDarkMode) DarkCard else CardBackground,
                            border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(EarningGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, null,
                                            tint = EarningGreen, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(stringResource(R.string.available_balance), style = MaterialTheme.typography.labelSmall,
                                            color = if (isDarkMode) DarkTextMuted else TextMuted)
                                        Text("$${String.format("%.2f", balance)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) DarkTextPrimary else TextPrimary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentBlue.copy(alpha = 0.1f)
                                ) {
                                    Text(stringResource(R.string.min_amount_label, minAmount.toString()),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    // Messages
                    errorMessage?.let { error ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    color = ErrorColor
                                )
                            }
                        }
                    }
                    
                    successMessage?.let { success ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = success,
                                    modifier = Modifier.padding(16.dp),
                                    color = SuccessColor
                                )
                            }
                        }
                    }
                    
                    // Amount Input
                    item {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(context.getString(R.string.withdraw_amount) + " ($)") },
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    
                    // Withdrawal Methods
                    item {
                        Text(
                            text = context.getString(R.string.withdraw_method),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                    }
                    
                    items(methods) { method ->
                        WithdrawalMethodCard(
                            method = method,
                            isSelected = selectedMethod?.id == method.id,
                            onClick = {
                                selectedMethod = method
                                accountDetails.clear()
                            },
                            isDarkMode = isDarkMode
                        )
                    }
                    
                    // Account Details Fields
                    selectedMethod?.let { method ->
                        item {
                            Text(
                                text = stringResource(R.string.account_details_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                        }
                        
                        items(method.fields) { field ->
                            OutlinedTextField(
                                value = accountDetails[field.key] ?: "",
                                onValueChange = { accountDetails[field.key] = it },
                                label = { Text(field.label) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                    
                    // Submit Button
                    item {
                        Button(
                            onClick = { submitWithdrawal() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = !isSubmitting && selectedMethod != null && amount.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EarningGreen,
                                disabledContainerColor = if (isDarkMode) DarkCard else BackgroundGray
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ArrowUpward, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(context.getString(R.string.withdraw_submit), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Previous Requests
                    if (withdrawalRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = context.getString(R.string.withdraw_history),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                        }
                        
                        items(withdrawalRequests) { request ->
                            WithdrawalRequestCard(
                                request = request,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun WithdrawalMethodCard(
    method: WithdrawalMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AccentBlue else if (isDarkMode) DarkBorder else BorderLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.name,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    text = if (method.region == "iraq") stringResource(R.string.region_iraq) else stringResource(R.string.region_international),
                    fontSize = 12.sp,
                    color = if (isDarkMode) DarkTextSecondary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WithdrawalRequestCard(
    request: WithdrawalRequestItem,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val statusColor = when (request.status) {
        "pending" -> WarningColor
        "approved" -> SuccessColor
        "rejected" -> ErrorColor
        else -> TextMuted
    }
    
    val statusText = when (request.status) {
        "pending" -> context.getString(R.string.status_pending)
        "approved" -> context.getString(R.string.status_approved)
        "rejected" -> context.getString(R.string.status_rejected)
        else -> request.status
    }
    
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (request.status == "approved") Icons.Default.CheckCircle
                    else if (request.status == "rejected") Icons.Default.Cancel
                    else Icons.Default.HourglassBottom,
                    null, tint = statusColor, modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$${String.format("%.2f", request.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    text = request.withdrawal_method,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
                if (request.status == "rejected" && !request.admin_notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.decline_reason) + ": " + request.admin_notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorColor,
                        fontSize = 11.sp
                    )
                }
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}
