package com.iqbox.app.ui.screens.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.components.*
import com.iqbox.app.ui.theme.*
import com.iqbox.app.ui.viewmodel.AuthViewModel
import com.iqbox.app.ui.viewmodel.AuthUiState

/**
 * Login Screen — dark cinematic design with floating cloud illustration
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = true // Login always uses dark theme for premium feel

    val authViewModel: AuthViewModel = viewModel { AuthViewModel(context) }
    val uiState by authViewModel.uiState.collectAsState()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val formAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 200),
        label = "formAlpha"
    )
    val formOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "formOffset"
    )

    // Ambient glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Error -> { apiError = s.message; authViewModel.resetState() }
            else -> {}
        }
    }

    fun validateLogin(): Boolean {
        loginError = if (login.isBlank()) context.getString(R.string.field_required) else null
        return loginError == null
    }
    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> context.getString(R.string.field_required)
            password.length < 6 -> context.getString(R.string.password_min)
            else -> null
        }
        return passwordError == null
    }
    fun validateAllFields() = validateLogin() && validatePassword()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF131C2E), Color(0xFF0F1729))
                )
            )
    ) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .scale(glowScale)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentBlue.copy(alpha = 0.2f), Color.Transparent)
                    ), CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 120.dp)
                .scale(glowScale)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentPurple.copy(alpha = 0.15f), Color.Transparent)
                    ), CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Cloud illustration area
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .alpha(formAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AccentBlue.copy(alpha = 0.08f),
                                    AccentPurple.copy(alpha = 0.05f)
                                )
                            ),
                            CircleShape
                        )
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AccentBlue.copy(alpha = 0.12f),
                                    AccentCyan.copy(alpha = 0.08f)
                                )
                            ),
                            CircleShape
                        )
                )
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(GradientPrimary),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                modifier = Modifier.alpha(formAlpha)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in to continue to your account",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkTextMuted,
                modifier = Modifier.alpha(formAlpha)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(formAlpha)
                    .offset(y = formOffset.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                IQTextField(
                    value = login,
                    onValueChange = { login = it; loginError = null },
                    label = stringResource(R.string.username_or_email),
                    isError = loginError != null,
                    errorMessage = loginError,
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(16.dp))

                IQTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = stringResource(R.string.password),
                    isPassword = true,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    keyboardType = KeyboardType.Password
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (apiError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorColor.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = apiError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                IQButton(
                    text = stringResource(R.string.login_btn),
                    onClick = {
                        if (validateAllFields()) {
                            apiError = null
                            authViewModel.login(login.trim(), password)
                        }
                    },
                    isLoading = uiState is AuthUiState.Loading,
                    enabled = login.isNotBlank() && password.isNotBlank() &&
                            uiState !is AuthUiState.Loading,
                    showArrow = true
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Register link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(formAlpha)
            ) {
                Text(
                    text = stringResource(R.string.no_account) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkTextMuted
                )
                IQTextButton(
                    text = stringResource(R.string.create_account),
                    onClick = onNavigateToRegister
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms & Privacy
            val termsText = buildAnnotatedString {
                pushStringAnnotation(tag = "TOS", annotation = "https://iqbox.site/termsofuse.html")
                withStyle(SpanStyle(color = AccentBlue.copy(alpha = 0.7f), textDecoration = TextDecoration.Underline)) {
                    append(stringResource(R.string.terms_of_service))
                }
                pop()
                append("  \u2022  ")
                pushStringAnnotation(tag = "PRIVACY", annotation = "https://iqbox.site/privacy_policy.html")
                withStyle(SpanStyle(color = AccentBlue.copy(alpha = 0.7f), textDecoration = TextDecoration.Underline)) {
                    append(stringResource(R.string.privacy_policy))
                }
                pop()
            }
            ClickableText(
                text = termsText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = DarkTextMuted,
                    fontSize = 12.sp
                ),
                modifier = Modifier.alpha(formAlpha),
                onClick = { offset ->
                    termsText.getStringAnnotations("TOS", offset, offset).firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                    }
                    termsText.getStringAnnotations("PRIVACY", offset, offset).firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
