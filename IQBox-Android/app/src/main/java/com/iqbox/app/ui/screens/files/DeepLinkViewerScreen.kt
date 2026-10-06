package com.iqbox.app.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DeepLinkViewerScreen - Fetches file info by share token then shows FileViewerScreen
 */
@Composable
fun DeepLinkViewerScreen(
    shareToken: String,
    onBackClick: () -> Unit = {}
) {
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fileId by remember { mutableStateOf(0) }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("other") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(shareToken) {
        isLoading = true
        error = null
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.getFileInfo(shareToken)
            }
            if (response.isSuccessful && response.body()?.success == true) {
                val file = response.body()!!.data!!.file
                fileId = file.id
                fileName = file.original_name ?: file.name
                fileType = file.file_type
                loaded = true
            } else {
                error = "File not found"
            }
        } catch (e: Exception) {
            error = "Connection error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (loaded) {
        FileViewerScreen(
            fileId = fileId,
            fileName = fileName,
            fileType = fileType,
            shareToken = shareToken,
            onBackClick = onBackClick
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) DarkBackground else BackgroundWhite),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading file...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkMode) DarkTextMuted else TextMuted
                    )
                }
            } else if (error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        error ?: "Unknown error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
