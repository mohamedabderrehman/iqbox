package com.iqbox.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.iqbox.app.data.ads.AdManager
import com.iqbox.app.data.local.LocaleManager
import com.iqbox.app.navigation.NavGraph
import com.iqbox.app.ui.theme.IQBoxTheme
import com.iqbox.app.ui.theme.LocalThemeState
import com.iqbox.app.ui.theme.rememberThemeState

class MainActivity : ComponentActivity() {
    
    private val deepLinkShareToken = mutableStateOf<String?>(null)
    
    override fun attachBaseContext(newBase: Context) {
        val localeManager = LocaleManager(newBase)
        super.attachBaseContext(localeManager.applyLocale(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        AdManager.initialize(this)
        AdManager.loadInterstitial(this)
        
        // Handle deep link from initial launch
        handleDeepLink(intent)
        
        setContent {
            val themeState = rememberThemeState()
            
            CompositionLocalProvider(LocalThemeState provides themeState) {
                IQBoxTheme(darkTheme = themeState.isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            themeState = themeState,
                            deepLinkShareToken = deepLinkShareToken.value,
                            onDeepLinkConsumed = { deepLinkShareToken.value = null }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val path = uri.path ?: return
        
        // Handle http://127.0.0.1:8080/file/{shareToken}
        if (path.startsWith("/file/")) {
            val token = path.removePrefix("/file/").trim('/')
            if (token.isNotBlank()) {
                deepLinkShareToken.value = token
            }
        }
        
        // Handle iqbox://file/{shareToken}
        if (uri.scheme == "iqbox" && uri.host == "file") {
            val token = uri.pathSegments?.firstOrNull() ?: uri.path?.trim('/')
            if (!token.isNullOrBlank()) {
                deepLinkShareToken.value = token
            }
        }
    }
}
