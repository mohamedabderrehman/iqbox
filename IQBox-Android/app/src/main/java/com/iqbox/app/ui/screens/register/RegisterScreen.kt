package com.iqbox.app.ui.screens.register

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
 * Register Screen — dark cinematic design matching LoginScreen
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = true // Always dark for premium feel

    val authViewModel: AuthViewModel = viewModel { AuthViewModel(context) }
    val uiState by authViewModel.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    // reCAPTCHA state (foundation - configure keys later)
    var captchaToken by remember { mutableStateOf<String?>(null) }
    val captchaReady = true

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val formAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 150),
        label = "formAlpha"
    )
    val formOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = tween(700, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "formOffset"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> onRegisterSuccess()
            is AuthUiState.Error -> { apiError = s.message; authViewModel.resetState() }
            else -> {}
        }
    }

    fun validateUsername(): Boolean {
        usernameError = when {
            username.isBlank() -> context.getString(R.string.field_required)
            username.length < 3 -> "Username must be at least 3 characters"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Letters, numbers, and underscores only"
            else -> null
        }
        return usernameError == null
    }
    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> context.getString(R.string.field_required)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email"
            else -> null
        }
        return emailError == null
    }
    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> context.getString(R.string.field_required)
            password.length < 6 -> context.getString(R.string.password_min)
            else -> null
        }
        return passwordError == null
    }
    fun validateConfirmPassword(): Boolean {
        confirmPasswordError = when {
            confirmPassword.isBlank() -> context.getString(R.string.field_required)
            confirmPassword != password -> context.getString(R.string.passwords_not_match)
            else -> null
        }
        return confirmPasswordError == null
    }
    fun validateAllFields(): Boolean {
        val a = validateUsername()
        val b = validateEmail()
        val c = validatePassword()
        val d = validateConfirmPassword()
        if (!termsAccepted) {
            apiError = context.getString(R.string.must_accept_terms)
            return false
        }
        return a && b && c && d
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF131C2E), Color(0xFF0F1729))
                )
            )
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-30).dp)
                .scale(glowScale)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentPurple.copy(alpha = 0.18f), Color.Transparent)
                    ), CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-40).dp, y = 200.dp)
                .scale(glowScale)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent)
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
            Spacer(modifier = Modifier.height(36.dp))

            // Icon illustration
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .alpha(formAlpha),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentPurple.copy(alpha = 0.08f), AccentBlue.copy(alpha = 0.05f))
                            ), CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentPurple, AccentBlue)
                            ), RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(formAlpha)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create your account and start earning",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkTextMuted,
                modifier = Modifier.alpha(formAlpha)
            )

            Spacer(modifier = Modifier.height(28.dp))

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
                    value = username,
                    onValueChange = { username = it; usernameError = null },
                    label = stringResource(R.string.username),
                    isError = usernameError != null,
                    errorMessage = usernameError,
                    keyboardType = KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(14.dp))

                IQTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = stringResource(R.string.email),
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(14.dp))

                IQTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = stringResource(R.string.password),
                    isPassword = true,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    keyboardType = KeyboardType.Password
                )
                Spacer(modifier = Modifier.height(14.dp))

                IQTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = null },
                    label = stringResource(R.string.confirm_password),
                    isPassword = true,
                    isError = confirmPasswordError != null,
                    errorMessage = confirmPasswordError,
                    keyboardType = KeyboardType.Password
                )
                Spacer(modifier = Modifier.height(14.dp))

                IQTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = stringResource(R.string.referral_code),
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentBlue,
                            uncheckedColor = DarkTextMuted
                        )
                    )
                    val termsText = buildAnnotatedString {
                        append(stringResource(R.string.agree_terms_prefix))
                        pushStringAnnotation(tag = "TOS", annotation = "https://iqbox.site/termsofuse.html")
                        withStyle(SpanStyle(color = AccentBlue, textDecoration = TextDecoration.Underline)) {
                            append(stringResource(R.string.terms_of_service))
                        }
                        pop()
                        append(stringResource(R.string.and_word))
                        pushStringAnnotation(tag = "PRIVACY", annotation = "https://iqbox.site/privacy_policy.html")
                        withStyle(SpanStyle(color = AccentBlue, textDecoration = TextDecoration.Underline)) {
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
                        onClick = { offset ->
                            termsText.getStringAnnotations("TOS", offset, offset).firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                            termsText.getStringAnnotations("PRIVACY", offset, offset).firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    text = stringResource(R.string.register_btn),
                    onClick = {
                        if (validateAllFields()) {
                            apiError = null
                            authViewModel.register(
                                username = username.trim(),
                                email = email.trim(),
                                password = password,
                                referralCode = referralCode.trim()
                            )
                        }
                    },
                    isLoading = uiState is AuthUiState.Loading,
                    enabled = username.isNotBlank() && email.isNotBlank() &&
                            password.isNotBlank() && confirmPassword.isNotBlank() &&
                            uiState !is AuthUiState.Loading,
                    showArrow = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(formAlpha)
            ) {
                Text(
                    text = stringResource(R.string.have_account) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkTextMuted
                )
                IQTextButton(
                    text = stringResource(R.string.login_here),
                    onClick = onNavigateToLogin
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
