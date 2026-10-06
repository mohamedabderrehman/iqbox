package com.iqbox.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbox.app.data.AppSettingsManager
import com.iqbox.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode
    
    // Get dynamic settings from API
    val settings by AppSettingsManager.settings.collectAsState()
    val supportEmail = settings.support_email ?: "support@iqbox.com"
    val supportPhone = settings.support_phone ?: ""
    val termsUrl = settings.terms_url ?: ""
    val privacyUrl = settings.privacy_url ?: ""
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackground else BackgroundWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkMode) DarkSurface else CardBackground,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isDarkMode) DarkCard else BackgroundGray,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) DarkTextPrimary else TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Help & Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) DarkTextPrimary else TextPrimary
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contact Section
                Text(
                    text = "Contact Us",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkMode) DarkCard else CardBackground,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Column {
                        // Email
                        if (supportEmail.isNotBlank()) {
                            SupportItem(
                                icon = Icons.Default.Email,
                                title = "Email Support",
                                subtitle = supportEmail,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$supportEmail")
                                    }
                                    context.startActivity(intent)
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                        
                        // Phone
                        if (supportPhone.isNotBlank()) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = if (isDarkMode) DarkBorder else BorderLight
                            )
                            SupportItem(
                                icon = Icons.Default.Phone,
                                title = "Phone Support",
                                subtitle = supportPhone,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$supportPhone")
                                    }
                                    context.startActivity(intent)
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Legal Section
                Text(
                    text = "Legal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkMode) DarkCard else CardBackground,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Column {
                        // Terms
                        if (termsUrl.isNotBlank()) {
                            SupportItem(
                                icon = Icons.Default.Description,
                                title = "Terms of Service",
                                subtitle = "Read our terms and conditions",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))
                                    context.startActivity(intent)
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                        
                        // Privacy
                        if (privacyUrl.isNotBlank()) {
                            if (termsUrl.isNotBlank()) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = if (isDarkMode) DarkBorder else BorderLight
                                )
                            }
                            SupportItem(
                                icon = Icons.Default.PrivacyTip,
                                title = "Privacy Policy",
                                subtitle = "Read our privacy policy",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                    context.startActivity(intent)
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // App Info
                Text(
                    text = "App Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkMode) DarkCard else CardBackground,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "App Name",
                                color = if (isDarkMode) DarkTextSecondary else TextSecondary
                            )
                            Text(
                                text = settings.app_name,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Version",
                                color = if (isDarkMode) DarkTextSecondary else TextSecondary
                            )
                            Text(
                                text = "1.0.0",
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) DarkTextPrimary else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) DarkTextPrimary else TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkMode) DarkTextSecondary else TextSecondary
            )
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isDarkMode) DarkTextMuted else TextMuted
        )
    }
}
