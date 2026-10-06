@file:OptIn(ExperimentalMaterial3Api::class)

package com.iqbox.app.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import com.iqbox.app.data.api.FileItem
import com.iqbox.app.ui.components.*
import com.iqbox.app.ui.theme.*
import com.iqbox.app.ui.viewmodel.SearchViewModel

/**
 * Search Screen — Premium redesign
 */
@Composable
fun SearchScreen(
    onFileClick: (FileItem) -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode

    val viewModel = remember { SearchViewModel(context) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
            Text(context.getString(R.string.search), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary)
        }

        // ── Search Bar ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkMode) DarkCard else CardBackground,
            shadowElevation = 3.dp
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                placeholder = context.getString(R.string.search_files),
                isDarkMode = isDarkMode
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Content ──
        when {
            searchQuery.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        if (isDarkMode) GradientHeroDark.map { it.copy(alpha = 0.4f) }
                                        else GradientPrimary.map { it.copy(alpha = 0.1f) }
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Search, null, tint = AccentBlue, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(context.getString(R.string.search),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) DarkTextPrimary else TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.search_suggestion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkMode) DarkTextMuted else TextMuted,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(stringResource(R.string.search_category_images) to Icons.Default.Image, stringResource(R.string.search_category_videos) to Icons.Default.Videocam, stringResource(R.string.search_category_documents) to Icons.Default.Description).forEach { (label, icon) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) DarkCard else BackgroundGray
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, null, tint = if (isDarkMode) DarkTextMuted else TextMuted, modifier = Modifier.size(16.dp))
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) DarkTextMuted else TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            isSearching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.searching), style = MaterialTheme.typography.bodyMedium, color = if (isDarkMode) DarkTextMuted else TextMuted)
                    }
                }
            }
            searchResults.isEmpty() -> {
                SearchEmptyState(query = searchQuery, isDarkMode = isDarkMode)
            }
            else -> {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AccentBlue.copy(alpha = 0.08f)
                ) {
                    Text(
                        stringResource(R.string.results_count, searchResults.size),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults) { file ->
                        SearchResultCard(
                            file = file,
                            onClick = { onFileClick(file) },
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SEARCH RESULT CARD
// ═══════════════════════════════════════════════════════
@Composable
private fun SearchResultCard(
    file: FileItem,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) DarkCard else CardBackground,
        border = BorderStroke(1.dp, if (isDarkMode) DarkBorder else BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkMode)
                            Brush.linearGradient(listOf(DarkCardElevated, DarkCard))
                        else
                            Brush.linearGradient(listOf(Color(0xFFE0E7FF), Color(0xFFF1F5F9)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.InsertDriveFile, null, tint = AccentBlue, modifier = Modifier.size(28.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, null,
                            tint = if (isDarkMode) DarkTextMuted else TextMuted,
                            modifier = Modifier.size(14.dp))
                        Text(
                            formatNumber(file.views_count),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Schedule, null,
                            tint = if (isDarkMode) DarkTextMuted else TextMuted,
                            modifier = Modifier.size(14.dp))
                        Text(
                            file.created_at,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) DarkTextMuted else TextMuted
                        )
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, null,
                tint = if (isDarkMode) DarkTextMuted else TextMuted,
                modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
