package com.iqbox.app.ui.screens.files

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbox.app.data.api.AddSharedItemRequest
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FolderPublicFile
import com.iqbox.app.data.api.FolderPublicInfoData
import com.iqbox.app.data.api.FolderPublicSubFolder
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DeepLinkFolderViewerScreen - Fetches folder info by share token and displays contents
 * Users can browse files/subfolders within the shared folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkFolderViewerScreen(
    shareToken: String,
    onBackClick: () -> Unit = {},
    onFileClick: (FolderPublicFile) -> Unit = {}
) {
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var folderData by remember { mutableStateOf<FolderPublicInfoData?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var addedToAccount by remember { mutableStateOf(false) }

    LaunchedEffect(shareToken) {
        isLoading = true
        error = null
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.getFolderPublicInfo(shareToken)
            }
            if (response.isSuccessful && response.body()?.success == true) {
                folderData = response.body()!!.data
            } else {
                error = response.body()?.message ?: "Folder not found"
            }
        } catch (e: Exception) {
            error = "Connection error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val bgColor = if (isDarkMode) DarkBackground else BackgroundWhite

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderData?.folder?.name ?: "Shared Folder",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!addedToAccount) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isAdding = true
                                    try {
                                        val tokenManager = TokenManager(context)
                                        val token = tokenManager.getToken()
                                        if (token != null) {
                                            val resp = withContext(Dispatchers.IO) {
                                                ApiClient.apiService.addSharedFolder(
                                                    "Bearer $token",
                                                    AddSharedItemRequest(shareToken)
                                                )
                                            }
                                            if (resp.isSuccessful && resp.body()?.success == true) {
                                                addedToAccount = true
                                                Toast.makeText(context, "Added to your account", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, resp.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isAdding = false
                                    }
                                }
                            },
                            enabled = !isAdding
                        ) {
                            if (isAdding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = AccentBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Added", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) DarkSurface else Color.White,
                    titleContentColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    navigationIconContentColor = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading folder...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
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
            folderData != null -> {
                FolderContentView(
                    data = folderData!!,
                    isDarkMode = isDarkMode,
                    onFileClick = onFileClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun FolderContentView(
    data: FolderPublicInfoData,
    isDarkMode: Boolean,
    onFileClick: (FolderPublicFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val folder = data.folder
    val files = data.files
    val subFolders = data.folders

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Folder header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) DarkCard else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentBlue, Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (folder.owner_name != null) {
                                Text(
                                    text = "Shared by ${folder.owner_name}",
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) DarkTextMuted else TextMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoChip(
                            icon = Icons.Default.InsertDriveFile,
                            text = "${files.size} files",
                            isDarkMode = isDarkMode
                        )
                        if (subFolders.isNotEmpty()) {
                            InfoChip(
                                icon = Icons.Default.Folder,
                                text = "${subFolders.size} folders",
                                isDarkMode = isDarkMode
                            )
                        }
                        InfoChip(
                            icon = Icons.Default.Storage,
                            text = formatSize(folder.size),
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }

        // Sub-folders section
        if (subFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Folders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
            items(subFolders) { subFolder ->
                SubFolderRow(
                    folder = subFolder,
                    isDarkMode = isDarkMode
                )
            }
        }

        // Files section
        if (files.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Files",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
            items(files) { file ->
                FileRow(
                    file = file,
                    isDarkMode = isDarkMode,
                    onClick = { onFileClick(file) }
                )
            }
        }

        // Empty state
        if (files.isEmpty() && subFolders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (isDarkMode) DarkTextMuted else TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "This folder is empty",
                            color = if (isDarkMode) DarkTextMuted else TextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDarkMode: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                color = if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDarkMode) DarkTextMuted else TextMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isDarkMode) DarkTextMuted else TextMuted
        )
    }
}

@Composable
private fun SubFolderRow(
    folder: FolderPublicSubFolder,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) DarkCard else Color.White
        )
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = folder.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FileRow(
    file: FolderPublicFile,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val iconColor = when (file.file_type) {
        "video" -> Color(0xFFEF4444)
        "image" -> Color(0xFF10B981)
        "audio" -> Color(0xFF8B5CF6)
        "pdf" -> Color(0xFFF59E0B)
        "document" -> AccentBlue
        "archive" -> Color(0xFF6366F1)
        else -> Color(0xFF6B7280)
    }

    val icon = when (file.file_type) {
        "video" -> Icons.Default.Videocam
        "image" -> Icons.Default.Image
        "audio" -> Icons.Default.MusicNote
        "pdf" -> Icons.Default.PictureAsPdf
        "document" -> Icons.Default.Description
        "archive" -> Icons.Default.Archive
        else -> Icons.Default.InsertDriveFile
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) DarkCard else Color.White
        )
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatSize(file.file_size),
                        fontSize = 12.sp,
                        color = if (isDarkMode) DarkTextMuted else TextMuted
                    )
                    Text(
                        text = file.file_type.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iconColor
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.2f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}
