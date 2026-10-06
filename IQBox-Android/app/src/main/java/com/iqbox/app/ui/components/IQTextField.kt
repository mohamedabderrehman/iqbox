package com.iqbox.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * Premium Text Field Component - تصميم احترافي حديث
 * Filled style مع Focus state واضح
 */
@Composable
fun IQTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) ErrorColor else (if (isDarkMode) DarkTextMuted else TextMuted)
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            isError = isError,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                // Focused state
                focusedBorderColor = if (isError) ErrorColor else AccentBlue,
                focusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                focusedLabelColor = if (isError) ErrorColor else AccentBlue,
                cursorColor = AccentBlue,
                
                // Unfocused state
                unfocusedBorderColor = if (isError) ErrorColor else (if (isDarkMode) DarkBorder else BorderLight),
                unfocusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                unfocusedLabelColor = if (isDarkMode) DarkTextMuted else TextMuted,
                
                // Disabled state
                disabledBorderColor = if (isDarkMode) DarkBorder else BorderLight,
                disabledTextColor = if (isDarkMode) DarkTextMuted else TextDisabled,
                disabledLabelColor = if (isDarkMode) DarkTextMuted else TextDisabled,
                
                // Error state
                errorBorderColor = ErrorColor,
                errorTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                errorLabelColor = ErrorColor,
                errorCursorColor = ErrorColor,
                
                // Container colors
                focusedContainerColor = if (isDarkMode) DarkCard else BackgroundWhite,
                unfocusedContainerColor = if (isDarkMode) DarkCard else BackgroundWhite,
                disabledContainerColor = if (isDarkMode) DarkSurface else BackgroundGray,
                errorContainerColor = ErrorBackground
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        
        // Error Message - تصميم نظيف
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorColor,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
    }
}
