@file:OptIn(ExperimentalMaterial3Api::class)

package com.iqbox.app.ui.screens.files

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FolderItem
import com.iqbox.app.data.api.ProgressRequestBody
import com.iqbox.app.data.local.TokenManager
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * File Upload Screen — Generic file upload for the Files platform
 */
@Composable
fun FileUploadScreen(
    currentFolderId: Int? = null,
    onBackClick: () -> Unit = {},
    onUploadSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var customName by remember { mutableStateOf("") }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadSpeed by remember { mutableStateOf(0L) }
    var uploadedBytes by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var uploadedFileUrl by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            errorMessage = null
            
            // Get file name and size
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "file"
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
            }
            customName = fileName
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_048_576 -> String.format("%.1f MB/s", bytesPerSec / 1_048_576.0)
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    fun uploadFile() {
        if (selectedFileUri == null) {
            errorMessage = context.getString(R.string.please_select_file)
            return
        }

        scope.launch {
            isUploading = true
            uploadProgress = 0f
            uploadSpeed = 0L
            uploadedBytes = 0L
            errorMessage = null

            try {
                val token = tokenManager.getToken()
                if (token == null) {
                    errorMessage = context.getString(R.string.please_login_first)
                    isUploading = false
                    return@launch
                }

                // Copy file to temp location
                val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                val tempFile = File(context.cacheDir, customName.ifBlank { fileName })
                
                withContext(Dispatchers.IO) {
                    FileOutputStream(tempFile).use { output ->
                        inputStream?.copyTo(output)
                    }
                    inputStream?.close()
                }

                // Detect actual MIME type from URI
                val mimeType = context.contentResolver.getType(selectedFileUri!!) ?: "application/octet-stream"
                
                // Build multipart body with progress tracking
                val fileBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val progressBody = ProgressRequestBody(fileBody) { written, total, speed ->
                    scope.launch(Dispatchers.Main) {
                        val progress = if (total > 0) written.toFloat() / total.toFloat() else 0f
                        uploadProgress = progress.coerceIn(0f, 1f)
                        uploadSpeed = speed
                        uploadedBytes = written
                    }
                }

                val multipartBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, progressBody)

                if (currentFolderId != null) {
                    multipartBuilder.addFormDataPart("folder_id", currentFolderId.toString())
                }
                val nameToSend = customName.ifBlank { null }
                if (nameToSend != null) {
                    multipartBuilder.addFormDataPart("name", nameToSend)
                }

                val request = okhttp3.Request.Builder()
                    .url(ApiClient.BASE_URL + "files/upload")
                    .header("Authorization", "Bearer $token")
                    .post(multipartBuilder.build())
                    .build()

                val response = withContext(Dispatchers.IO) {
                    ApiClient.okHttpClient.newCall(request).execute()
                }

                // Clean up temp file
                tempFile.delete()

                val responseBody = response.body?.string()
                val json = Gson().fromJson(responseBody ?: "{}", JsonObject::class.java)

                if (response.isSuccessful && json.get("success")?.asBoolean == true) {
                    uploadProgress = 1f
                    successMessage = context.getString(R.string.upload_success_title)
                    uploadedFileUrl = json.getAsJsonObject("data")
                        ?.getAsJsonObject("file")
                        ?.get("share_url")?.asString
                } else {
                    errorMessage = json.get("message")?.asString ?: context.getString(R.string.upload_failed_msg)
                }
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.upload_error, e.message ?: "")
            } finally {
                isUploading = false
            }
        }
    }

    // Success Dialog
    if (successMessage != null) {
        AlertDialog(
            onDismissRequest = { successMessage = null; onUploadSuccess() },
            containerColor = if (isDarkMode) DarkCard else CardBackground,
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                Brush.linearGradient(GradientEarning),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.upload_success_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.file_ready_to_share),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) DarkTextMuted else TextMuted
                    )
                }
            },
            text = {
                if (uploadedFileUrl != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) DarkSurface else BackgroundGray
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Link, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Text(
                                uploadedFileUrl!!,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("File URL", uploadedFileUrl))
                                    android.widget.Toast.makeText(context, context.getString(R.string.link_copied), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = AccentBlue.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy, stringResource(R.string.copy_short),
                                    tint = AccentBlue,
                                    modifier = Modifier.padding(6.dp).size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { successMessage = null; onUploadSuccess() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
            .verticalScroll(rememberScrollState())
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
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(R.string.upload_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    stringResource(R.string.upload_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // File Selection Card
        if (selectedFileUri == null) {
            Surface(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkMode) DarkCard else CardBackground,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .border(
                            2.dp,
                            Brush.linearGradient(GradientPrimary.map { it.copy(alpha = 0.3f) }),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.linearGradient(GradientPrimary.map { it.copy(alpha = 0.1f) }),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudUpload, null, tint = AccentBlue, modifier = Modifier.size(40.dp))
                    }
                    Text(
                        stringResource(R.string.tap_to_choose_file),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary
                    )
                    Text(
                        stringResource(R.string.file_types_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) DarkTextMuted else TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PDF" to Color(0xFFF59E0B), "MP4" to Color(0xFFEF4444), "IMG" to Color(0xFF10B981), "ZIP" to Color(0xFF6366F1)).forEach { (label, color) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkMode) DarkCard else CardBackground,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.linearGradient(GradientEarning.map { it.copy(alpha = 0.15f) }),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.InsertDriveFile, null, tint = EarningGreen, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EarningGreen.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    formatFileSize(fileSize),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = EarningGreen
                                )
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = EarningGreen, modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(R.string.ready_to_upload),
                                style = MaterialTheme.typography.labelSmall,
                                color = EarningGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) DarkSurface else BackgroundGray
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz, stringResource(R.string.change_file),
                            tint = if (isDarkMode) DarkTextSecondary else TextSecondary,
                            modifier = Modifier.padding(10.dp).size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Optional: Rename field
        if (selectedFileUri != null) {
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.file_name_optional),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) DarkTextSecondary else TextSecondary
                    )
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text(stringResource(R.string.leave_empty_original)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = if (isDarkMode) DarkBorder else BorderLight,
                            focusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                            unfocusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                            cursorColor = AccentBlue,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Error Message
        errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkMode) ErrorColor.copy(alpha = 0.15f) else ErrorBackground
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorColor)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { uploadFile() }) {
                        Text(stringResource(R.string.retry), color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Upload Button
        val buttonEnabled = !isUploading && selectedFileUri != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (buttonEnabled) Brush.horizontalGradient(GradientPrimary)
                    else Brush.horizontalGradient(
                        if (isDarkMode) listOf(DarkCard, DarkCard) else listOf(BackgroundGray, BackgroundGray)
                    )
                )
                .clickable(enabled = buttonEnabled) { uploadFile() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Text(stringResource(R.string.uploading_progress), color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, null, tint = if (buttonEnabled) Color.White else TextMuted, modifier = Modifier.size(22.dp))
                    Text(
                        stringResource(R.string.upload_file_btn),
                        color = if (buttonEnabled) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        // Progress
        AnimatedVisibility(
            visible = isUploading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkMode) DarkCard else CardBackground,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                stringResource(R.string.uploading_progress),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                        }
                        Text(
                            "${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isDarkMode) DarkBorder else Color(0xFFF1F5F9))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(uploadProgress.coerceIn(0.01f, 1f))
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(GradientPrimary))
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (fileSize > 0) formatFileSize(uploadedBytes) + " / " + formatFileSize(fileSize) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                        if (uploadSpeed > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentBlue.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    formatSpeed(uploadSpeed),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
