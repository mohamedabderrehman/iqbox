package com.iqbox.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.res.stringResource
import com.iqbox.app.R
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbox.app.ui.theme.*

/**
 * Bottom Navigation Bar — Premium glass-morphism style
 */
@Composable
fun BottomNavBar(
    currentRoute: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Glass background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = if (isDarkMode) DarkSurface.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.97f),
            shadowElevation = 20.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItemView(
                    item = BottomNavItem.Home,
                    isSelected = currentRoute == BottomNavItem.Home,
                    onClick = { onItemClick(BottomNavItem.Home) },
                    isDarkMode = isDarkMode
                )
                BottomNavItemView(
                    item = BottomNavItem.Search,
                    isSelected = currentRoute == BottomNavItem.Search,
                    onClick = { onItemClick(BottomNavItem.Search) },
                    isDarkMode = isDarkMode
                )

                // Center Upload FAB
                UploadButton(onClick = onUploadClick, isDarkMode = isDarkMode)

                BottomNavItemView(
                    item = BottomNavItem.Files,
                    isSelected = currentRoute == BottomNavItem.Files,
                    onClick = { onItemClick(BottomNavItem.Files) },
                    isDarkMode = isDarkMode
                )
                BottomNavItemView(
                    item = BottomNavItem.Profile,
                    isSelected = currentRoute == BottomNavItem.Profile,
                    onClick = { onItemClick(BottomNavItem.Profile) },
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AccentBlue else (if (isDarkMode) DarkTextMuted else TextMuted),
        animationSpec = tween(200),
        label = "icon_color"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 28.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicator"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Active indicator dot/pill
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) Brush.horizontalGradient(GradientPrimary)
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = stringResource(item.labelRes),
            modifier = Modifier
                .size(22.dp)
                .scale(scale),
            tint = iconColor
        )

        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = iconColor
        )
    }
}

@Composable
private fun UploadButton(onClick: () -> Unit, isDarkMode: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .offset(y = (-14).dp)
            .size(58.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = AccentBlue.copy(alpha = 0.6f)
            )
            .background(
                brush = Brush.linearGradient(GradientPrimary),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Upload",
            modifier = Modifier.size(28.dp),
            tint = Color.White
        )
    }
}

enum class BottomNavItem(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Search(R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    Files(R.string.nav_files, Icons.Filled.Folder, Icons.Outlined.FolderOpen),
    Profile(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}
