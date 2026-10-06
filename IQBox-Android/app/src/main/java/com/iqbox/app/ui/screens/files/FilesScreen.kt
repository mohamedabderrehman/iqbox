@file:OptIn(ExperimentalMaterial3Api::class)

package com.iqbox.app.ui.screens.files

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.api.*
import com.iqbox.app.ui.components.BannerAdView
import com.iqbox.app.ui.components.BottomNavBar
import com.iqbox.app.ui.components.BottomNavItem
import com.iqbox.app.ui.components.SectionTitle
import com.iqbox.app.ui.theme.*

@Composable
fun FilesScreen(
    files: List<FileItem>,
    folders: List<FolderItem>,
    storageInfo: StorageInfo?,
    currentFolderId: Int?,
    breadcrumbs: List<FolderItem>,
    isLoading: Boolean,
    onFolderClick: (FolderItem) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onBackClick: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onRefresh: () -> Unit,
    onUploadClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    isDarkMode: Boolean = false
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    val context = LocalContext.current

    Scaffold(
        containerColor = if (isDarkMode) DarkBackground else BackgroundWhite,
        bottomBar = {
            BottomNavBar(
                currentRoute = BottomNavItem.Files,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.Home -> onHomeClick()
                        BottomNavItem.Search -> onSearchClick()
                        BottomNavItem.Profile -> onProfileClick()
                        else -> {}
                    }
                },
                onUploadClick = onUploadClick,
                isDarkMode = isDarkMode
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (isDarkMode) DarkBackground else BackgroundWhite)
        ) {
            // Header
            FilesHeader(
                isDarkMode = isDarkMode,
                currentFolderId = currentFolderId,
                onBackClick = onBackClick,
                onCreateFolderClick = { showCreateFolderDialog = true },
                onUploadClick = onUploadClick
            )

            // Storage Quota Bar
            storageInfo?.let { storage ->
                StorageQuotaBar(
                    storage = storage,
                    isDarkMode = isDarkMode
                )
            }

            // Breadcrumbs
            if (breadcrumbs.isNotEmpty()) {
                BreadcrumbsRow(
                    breadcrumbs = breadcrumbs,
                    onFolderClick = onFolderClick,
                    isDarkMode = isDarkMode
                )
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (files.isEmpty() && folders.isEmpty()) {
                EmptyFilesState(isDarkMode = isDarkMode, onUploadClick = onUploadClick)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Banner Ad for free users
                    item {
                        BannerAdView(
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (folders.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = stringResource(R.string.folders),
                                isDarkMode = isDarkMode,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                        items(folders) { folder ->
                            FolderCard(
                                folder = folder,
                                isDarkMode = isDarkMode,
                                onClick = { onFolderClick(folder) },
                                onShareFolder = {
                                    val token = folder.share_token.ifEmpty { null }
                                    val url = folder.share_url 
                                        ?: if (token != null) "http://127.0.0.1:8080/folder/$token" else null
                                    if (url != null) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Folder Link", url))
                                        Toast.makeText(context, context.getString(R.string.folder_link_copied), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.no_link_for_folder), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLongClick = { selectedFolder = folder }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // Files Section
                    if (files.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = context.getString(R.string.files),
                                isDarkMode = isDarkMode,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                        items(files) { file ->
                            FileCard(
                                file = file,
                                isDarkMode = isDarkMode,
                                onClick = { onFileClick(file) },
                                onCopyLink = {
                                    file.share_url?.let { url ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("File Link", url))
                                        Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDownload = {
                                    try {
                                        val downloadUrl = "http://127.0.0.1:3000/api/files/download/${file.share_token}"
                                        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                            setTitle(file.name)
                                            setDescription(context.getString(R.string.downloading_file, file.name))
                                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "IQBox/${file.name}")
                                            setAllowedOverMetered(true)
                                            setAllowedOverRoaming(true)
                                        }
                                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        dm.enqueue(request)
                                        Toast.makeText(context, context.getString(R.string.download) + "...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { selectedFile = file }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = if (isDarkMode) DarkCard else CardBackground,
            titleContentColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
            textContentColor = if (isDarkMode) DarkTextSecondary else TextSecondary,
            title = { Text(stringResource(R.string.create_folder), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = if (isDarkMode) DarkBorder else BorderLight,
                        focusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        unfocusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        cursorColor = AccentBlue,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = if (isDarkMode) DarkTextMuted else TextMuted
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName)
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(stringResource(R.string.create), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(stringResource(R.string.cancel), color = if (isDarkMode) DarkTextSecondary else TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete File Confirmation
    selectedFile?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedFile = null },
            containerColor = if (isDarkMode) DarkCard else CardBackground,
            titleContentColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
            textContentColor = if (isDarkMode) DarkTextSecondary else TextSecondary,
            title = { Text(stringResource(R.string.delete_file_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_file_name_confirm, file.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(file)
                        selectedFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { selectedFile = null }) {
                    Text(stringResource(R.string.cancel), color = if (isDarkMode) DarkTextSecondary else TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Folder Confirmation
    selectedFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { selectedFolder = null },
            containerColor = if (isDarkMode) DarkCard else CardBackground,
            titleContentColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
            textContentColor = if (isDarkMode) DarkTextSecondary else TextSecondary,
            title = { Text(stringResource(R.string.delete_folder_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_folder_name_confirm, folder.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(folder)
                        selectedFolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { selectedFolder = null }) {
                    Text(stringResource(R.string.cancel), color = if (isDarkMode) DarkTextSecondary else TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun FilesHeader(
    isDarkMode: Boolean,
    currentFolderId: Int?,
    onBackClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentFolderId != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            1.dp,
                            if (isDarkMode) DarkBorder.copy(alpha = 0.5f) else BorderLight,
                            CircleShape
                        )
                        .background(if (isDarkMode) DarkCard else BackgroundGray, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack, stringResource(R.string.back),
                        tint = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    stringResource(R.string.my_files),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Text(
                    stringResource(R.string.manage_files_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) DarkTextMuted else TextMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = onCreateFolderClick,
                shape = RoundedCornerShape(12.dp),
                color = AccentBlue.copy(alpha = 0.1f),
                border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.CreateNewFolder, stringResource(R.string.new_folder),
                    tint = AccentBlue,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Surface(
                onClick = onUploadClick,
                shape = RoundedCornerShape(12.dp),
                color = EarningGreen.copy(alpha = 0.1f),
                border = BorderStroke(0.5.dp, EarningGreen.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.CloudUpload, stringResource(R.string.upload_file),
                    tint = EarningGreen,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageQuotaBar(
    storage: StorageInfo,
    isDarkMode: Boolean
) {
    val usedGB = storage.used / (1024.0 * 1024.0 * 1024.0)
    val limitGB = storage.limit / (1024.0 * 1024.0 * 1024.0)
    val percentage = storage.percentage.coerceIn(0, 100)
    val progressColor = when {
        percentage >= 90 -> Color(0xFFEF4444)
        percentage >= 70 -> Color(0xFFF59E0B)
        else -> AccentBlue
    }
    val progressGradient = when {
        percentage >= 90 -> listOf(Color(0xFFEF4444), Color(0xFFF87171))
        percentage >= 70 -> listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
        else -> GradientPrimary
    }

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "storage_progress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = progressColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(
            0.5.dp,
            if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(progressColor.copy(alpha = 0.15f), progressColor.copy(alpha = 0.08f))
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cloud, null,
                            tint = progressColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.storage_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                        Text(
                            "${String.format("%.2f", usedGB)} / ${String.format("%.0f", limitGB)} GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${percentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    if (storage.is_premium) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PremiumGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Premium",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Animated progress bar with rounded track
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
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(progressGradient))
                )
            }

            if (percentage >= 80 && !storage.is_premium) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.1f) else Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Text(
                            stringResource(R.string.storage_running_out),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbsRow(
    breadcrumbs: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    isDarkMode: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentBlue.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.12f))
            ) {
                Icon(
                    Icons.Default.Home, "Root",
                    tint = AccentBlue,
                    modifier = Modifier.padding(6.dp).size(16.dp)
                )
            }
        }
        items(breadcrumbs) { folder ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = if (isDarkMode) DarkTextMuted else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Surface(
                    onClick = { onFolderClick(folder) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) DarkCardElevated else BackgroundGray,
                    border = BorderStroke(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight)
                ) {
                    Text(
                        folder.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderItem,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onShareFolder: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = AccentBlue.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Gradient accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(AccentBlue, AccentPurple.copy(alpha = 0.4f))),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder icon with gradient background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.12f), AccentPurple.copy(alpha = 0.06f))),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentBlue.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.1f))
                        ) {
                            Text(
                                "${folder.files_count} ملف",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            formatFileSize(folder.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                    }
                }

                // Share button
                Surface(
                    onClick = onShareFolder,
                    shape = RoundedCornerShape(10.dp),
                    color = EarningGreen.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, EarningGreen.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Default.Share, stringResource(R.string.share_folder),
                        tint = EarningGreen,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }

                // Chevron
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .border(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.3f) else BorderLight, CircleShape)
                        .background(if (isDarkMode) DarkCardElevated else BackgroundGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = if (isDarkMode) DarkTextMuted else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    file: FileItem,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onCopyLink: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color) = getFileIconAndColor(file.file_type)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(0.5.dp, if (isDarkMode) DarkBorder.copy(alpha = 0.4f) else BorderLight)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Gradient accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(color, color.copy(alpha = 0.3f))),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gradient icon background
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.linearGradient(listOf(color.copy(alpha = 0.12f), color.copy(alpha = 0.05f))),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatFileSize(file.file_size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, null, tint = if (isDarkMode) DarkTextMuted else TextMuted, modifier = Modifier.size(13.dp))
                                Text("${file.views_count}", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) DarkTextMuted else TextMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, null, tint = if (isDarkMode) DarkTextMuted else TextMuted, modifier = Modifier.size(13.dp))
                                Text("${file.downloads_count}", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) DarkTextMuted else TextMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Horizontal action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        color = EarningGreen.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, EarningGreen.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, null, Modifier.size(14.dp), tint = EarningGreen)
                            Text(stringResource(R.string.download_short), style = MaterialTheme.typography.labelSmall, color = EarningGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        onClick = onCopyLink,
                        shape = RoundedCornerShape(10.dp),
                        color = AccentBlue.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, null, Modifier.size(14.dp), tint = AccentBlue)
                            Text(stringResource(R.string.copy_short), style = MaterialTheme.typography.labelSmall, color = AccentBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.06f),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline, stringResource(R.string.delete),
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.padding(6.dp).size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilesState(isDarkMode: Boolean, onUploadClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (isDarkMode) DarkCard else CardBackground,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            Brush.linearGradient(
                                if (isDarkMode) GradientHeroDark.map { it.copy(alpha = 0.5f) }
                                else GradientPrimary.map { it.copy(alpha = 0.12f) }
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudUpload, null,
                        tint = AccentBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.no_files_yet),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.upload_first_file),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) DarkTextMuted else TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onUploadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.upload_file), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onUploadClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
                ) {
                    Icon(Icons.Default.CreateNewFolder, null, tint = if (isDarkMode) DarkTextSecondary else TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.new_folder), color = if (isDarkMode) DarkTextSecondary else TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun getFileIconAndColor(fileType: String): Pair<ImageVector, Color> {
    return when (fileType) {
        "video" -> Icons.Default.Videocam to Color(0xFFEF4444)
        "image" -> Icons.Default.Image to Color(0xFF10B981)
        "audio" -> Icons.Default.MusicNote to Color(0xFF8B5CF6)
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFF59E0B)
        "document" -> Icons.Default.Description to Color(0xFF3B82F6)
        "archive" -> Icons.Default.FolderZip to Color(0xFF6366F1)
        else -> Icons.Default.InsertDriveFile to Color(0xFF6B7280)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1073741824 -> String.format("%.1f GB", bytes / 1073741824.0)
        bytes >= 1048576 -> String.format("%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
