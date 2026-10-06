package com.iqbox.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.UpdateProfileRequest
import com.iqbox.app.data.api.ChangePasswordRequest
import com.iqbox.app.data.local.TokenManager
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    val scope = rememberCoroutineScope()
    
    val tokenManager = remember { TokenManager(context) }
    val apiService = ApiClient.apiService
    
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Load current profile
    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken()
            if (token != null) {
                val response = apiService.getUserProfile("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    username = response.body()!!.data.user.username
                }
            }
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.failed_load_data)
        } finally {
            isLoading = false
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
            Text(stringResource(R.string.edit_profile_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary)
        }

        if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    brush = Brush.linearGradient(GradientPrimary),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    
                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username_label)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = if (isDarkMode) DarkBorder else BorderLight
                        ),
                        singleLine = true
                    )
                    
                    // Change Password Section
                    ChangePasswordSection(
                        isDarkMode = isDarkMode,
                        tokenManager = tokenManager,
                        apiService = apiService
                    )
                    
                    // Error message
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = ErrorColor
                            )
                        }
                    }
                    
                    // Success message
                    successMessage?.let { success ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = success,
                                modifier = Modifier.padding(16.dp),
                                color = SuccessColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Save button
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                successMessage = null
                                
                                try {
                                    val token = tokenManager.getToken()
                                    if (token != null) {
                                        val response = apiService.updateUserProfile(
                                            "Bearer $token",
                                            UpdateProfileRequest(username = username)
                                        )
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            successMessage = context.getString(R.string.profile_updated)
                                        } else {
                                            errorMessage = response.body()?.message ?: context.getString(R.string.update_failed)
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = context.getString(R.string.connection_error)
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isSaving && username.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save_changes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSection(
    isDarkMode: Boolean,
    tokenManager: TokenManager,
    apiService: com.iqbox.app.data.api.ApiService
) {
    val scope = rememberCoroutineScope()
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isChanging by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf<String?>(null) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.change_password_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
            
            // Current Password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text(stringResource(R.string.current_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(R.string.new_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_new_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
            )
            
            // Error/Success Messages
            passwordError?.let { error ->
                Text(
                    text = error,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            passwordSuccess?.let { success ->
                Text(
                    text = success,
                    color = SuccessColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Change Password Button
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        passwordError = context.getString(R.string.passwords_dont_match)
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        passwordError = context.getString(R.string.password_min_length)
                        return@Button
                    }
                    
                    scope.launch {
                        isChanging = true
                        passwordError = null
                        passwordSuccess = null
                        
                        try {
                            val token = tokenManager.getToken()
                            if (token != null) {
                                val response = apiService.changePassword(
                                    "Bearer $token",
                                    ChangePasswordRequest(
                                        current_password = currentPassword,
                                        new_password = newPassword
                                    )
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    passwordSuccess = context.getString(R.string.password_changed)
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                } else {
                                    passwordError = response.body()?.message ?: context.getString(R.string.password_change_failed)
                                }
                            }
                        } catch (e: Exception) {
                            passwordError = context.getString(R.string.connection_error)
                        } finally {
                            isChanging = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChanging && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(stringResource(R.string.change_password_btn))
                }
            }
        }
    }
}
