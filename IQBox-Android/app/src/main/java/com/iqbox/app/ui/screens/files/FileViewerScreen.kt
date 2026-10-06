package com.iqbox.app.ui.screens.files

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FileItem
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    fileId: Int,
    fileName: String,
    fileType: String,
    shareToken: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    val baseUrl = "http://127.0.0.1:3000/api"
    val streamUrl = "$baseUrl/files/stream/$shareToken"
    val downloadUrl = "$baseUrl/files/download/$shareToken"
    val sharePageUrl = "http://127.0.0.1:8080/file/$shareToken"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (fileType == "video") Color.Black else if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (fileType == "video") Color.White.copy(alpha = 0.15f)
                        else if (isDarkMode) DarkCard else BackgroundGray,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = if (fileType == "video") Color.White
                    else if (isDarkMode) DarkTextPrimary else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (fileType == "video") Color.White
                else if (isDarkMode) DarkTextPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (fileType) {
                "video" -> VideoPlayerContent(streamUrl = streamUrl)
                "image" -> ImageViewerContent(streamUrl = streamUrl, fileName = fileName)
                "audio" -> AudioPlayerContent(streamUrl = streamUrl, fileName = fileName, isDarkMode = isDarkMode)
                "pdf" -> PdfViewerContent(sharePageUrl = sharePageUrl, context = context, isDarkMode = isDarkMode)
                else -> GenericFileContent(fileName = fileName, fileType = fileType, isDarkMode = isDarkMode)
            }
        }

        // Bottom action bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (fileType == "video") Color(0xFF1a1a1a)
            else if (isDarkMode) DarkSurface else Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download
                ActionButton(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.download_label),
                    color = AccentBlue,
                    onClick = {
                        downloadFileToDevice(context, downloadUrl, fileName)
                    }
                )

                // Share link
                ActionButton(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.share_label),
                    color = AccentPurple,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, sharePageUrl)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_file_chooser)))
                    }
                )

                // Copy link
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = stringResource(R.string.copy_short),
                    color = EarningGreen,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("link", sharePageUrl))
                        Toast.makeText(context, context.getString(R.string.link_copied_toast), Toast.LENGTH_SHORT).show()
                    }
                )

                // Open in browser
                ActionButton(
                    icon = Icons.Default.OpenInBrowser,
                    label = stringResource(R.string.open_external),
                    color = Color(0xFFF59E0B),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sharePageUrl)))
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ═══════════════════════════════════════
// VIDEO PLAYER
// ═══════════════════════════════════════
@Composable
private fun VideoPlayerContent(streamUrl: String) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

// ═══════════════════════════════════════
// IMAGE VIEWER (pinch-to-zoom)
// ═══════════════════════════════════════
@Composable
private fun ImageViewerContent(streamUrl: String, fileName: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(streamUrl)
                .crossfade(true)
                .build(),
            contentDescription = fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

// ═══════════════════════════════════════
// AUDIO PLAYER
// ═══════════════════════════════════════
@Composable
private fun AudioPlayerContent(streamUrl: String, fileName: String, isDarkMode: Boolean) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AccentPurple
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary
        )
        Spacer(Modifier.height(24.dp))

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 0
                    controllerHideOnTouch = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

// ═══════════════════════════════════════
// PDF VIEWER (opens system or browser)
// ═══════════════════════════════════════
@Composable
private fun PdfViewerContent(sharePageUrl: String, context: Context, isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFF59E0B)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.pdf_file),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pdf_open_external),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkMode) DarkTextMuted else TextMuted
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sharePageUrl)))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.open_pdf))
        }
    }
}

// ═══════════════════════════════════════
// GENERIC FILE
// ═══════════════════════════════════════
@Composable
private fun GenericFileContent(fileName: String, fileType: String, isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AccentBlue
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) DarkTextPrimary else TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.unsupported_file_type, fileType.uppercase()),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkMode) DarkTextMuted else TextMuted
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.open_in_external_app),
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkMode) DarkTextMuted else TextMuted
        )
    }
}

// ═══════════════════════════════════════
// DOWNLOAD TO DEVICE
// ═══════════════════════════════════════
private fun downloadFileToDevice(context: Context, downloadUrl: String, fileName: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(fileName)
            setDescription(context.getString(R.string.downloading_notification, fileName))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "IQBox/$fileName")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        Toast.makeText(context, context.getString(R.string.download_started, fileName), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.download_error, e.message ?: ""), Toast.LENGTH_LONG).show()
    }
}
