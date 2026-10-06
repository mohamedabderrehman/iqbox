package com.iqbox.app.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.iqbox.app.data.ads.AdManager
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.ui.components.AdPopupDialog
import com.iqbox.app.ui.screens.splash.SplashScreen
import com.iqbox.app.ui.screens.login.LoginScreen
import com.iqbox.app.ui.screens.register.RegisterScreen
import com.iqbox.app.ui.screens.promo.PromoSplashScreen
import com.iqbox.app.ui.screens.home.HomeScreen
import com.iqbox.app.ui.screens.profile.ProfileScreen
import com.iqbox.app.ui.screens.search.SearchScreen
import com.iqbox.app.ui.screens.files.FilesScreen
import com.iqbox.app.ui.screens.files.AdGateScreen
import com.iqbox.app.ui.screens.files.FileUploadScreen
import com.iqbox.app.ui.screens.files.FileViewerScreen
import com.iqbox.app.ui.screens.files.DeepLinkViewerScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.iqbox.app.ui.screens.subscription.SubscriptionScreen
import com.iqbox.app.ui.screens.profile.EditProfileScreen
import com.iqbox.app.ui.screens.profile.HelpSupportScreen
import com.iqbox.app.ui.screens.wallet.WalletScreen
import com.iqbox.app.ui.screens.wallet.WithdrawScreen
import com.iqbox.app.ui.screens.wallet.ReferralScreen
import com.iqbox.app.ui.viewmodel.FilesViewModel
import com.iqbox.app.ui.theme.ThemeState
import kotlinx.coroutines.delay

@Composable
fun NavGraph(
    navController: NavHostController,
    themeState: ThemeState,
    startDestination: String = Screen.Splash.route,
    deepLinkShareToken: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Handle deep link navigation
    LaunchedEffect(deepLinkShareToken) {
        if (deepLinkShareToken != null) {
            navController.navigate(Screen.DeepLinkViewer.createRoute(deepLinkShareToken)) {
                launchSingleTop = true
            }
            onDeepLinkConsumed()
        }
    }
    
    // Helper function to open file link in external browser
    fun openLinkInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(250)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = tween(250)
            )
        }
    ) {
        // Splash Screen — fade only, no slide
        composable(
            Screen.Splash.route,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Free users see promo splash first, premium go straight to home
                    // For now, always show promo (premium check happens in PromoSplash)
                    navController.navigate(Screen.PromoSplash.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        // Register Screen
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.PromoSplash.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Promo Splash Screen (free users only)
        composable(Screen.PromoSplash.route) {
            val activity = context as? Activity
            PromoSplashScreen(
                onExperienceNow = {
                    // Show interstitial then go to subscription
                    if (activity != null) {
                        AdManager.showInterstitial(activity) {
                            navController.navigate(Screen.Subscription.route) {
                                popUpTo(Screen.PromoSplash.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Subscription.route) {
                            popUpTo(Screen.PromoSplash.route) { inclusive = true }
                        }
                    }
                },
                onDismiss = {
                    // Show interstitial then go to home
                    if (activity != null) {
                        AdManager.showInterstitial(activity) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.PromoSplash.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PromoSplash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onUploadClick = {
                    navController.navigate(Screen.FileUpload.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onFilesClick = {
                    navController.navigate(Screen.Files.route)
                },
                onLibraryClick = {
                    navController.navigate(Screen.Files.route)
                },
                onFileClick = { file ->
                    if (file.file_type == "video" || file.file_type == "image") {
                        navController.navigate(
                            Screen.AdGate.createRoute(file.id, file.name, file.file_type, file.share_token)
                        )
                    } else {
                        navController.navigate(
                            Screen.FileViewer.createRoute(file.id, file.name, file.file_type, file.share_token)
                        )
                    }
                },
                onSubscriptionClick = {
                    navController.navigate(Screen.Subscription.route)
                },
                onWalletClick = {
                    navController.navigate(Screen.Wallet.route)
                },
                onReferralClick = {
                    navController.navigate(Screen.Referral.route)
                },
                onHelpClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onEditProfileClick = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNotificationsClick = {
                    // TODO: Navigate to Notifications Screen
                },
                onSubscriptionClick = {
                    navController.navigate(Screen.Subscription.route)
                },
                onWalletClick = {
                    navController.navigate(Screen.Wallet.route)
                },
                onHelpClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Edit Profile Screen
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        // Help & Support Screen
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Subscription Screen
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                onFileClick = { file ->
                    navController.navigate(
                        Screen.FileViewer.createRoute(file.id, file.name, file.file_type, file.share_token)
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        // Files Screen — ملفاتي
        composable(Screen.Files.route) {
            val filesViewModel: FilesViewModel = viewModel()
            val files by filesViewModel.files.collectAsState()
            val folders by filesViewModel.folders.collectAsState()
            val storageInfo by filesViewModel.storageInfo.collectAsState()
            val currentFolderId by filesViewModel.currentFolderId.collectAsState()
            val breadcrumbs by filesViewModel.breadcrumbs.collectAsState()
            val isLoading by filesViewModel.isLoading.collectAsState()

            // Refresh when returning from upload
            val refreshTrigger = it.savedStateHandle.get<Boolean>("refresh")
            LaunchedEffect(refreshTrigger) {
                if (refreshTrigger == true) {
                    filesViewModel.refresh()
                    it.savedStateHandle.remove<Boolean>("refresh")
                }
            }
            
            FilesScreen(
                files = files,
                folders = folders,
                storageInfo = storageInfo,
                currentFolderId = currentFolderId,
                breadcrumbs = breadcrumbs,
                isLoading = isLoading,
                onFolderClick = { folder -> filesViewModel.navigateToFolder(folder) },
                onFileClick = { file ->
                    if (file.file_type == "video" || file.file_type == "image") {
                        navController.navigate(
                            Screen.AdGate.createRoute(file.id, file.name, file.file_type, file.share_token)
                        )
                    } else {
                        navController.navigate(
                            Screen.FileViewer.createRoute(file.id, file.name, file.file_type, file.share_token)
                        )
                    }
                },
                onBackClick = {
                    if (currentFolderId != null) {
                        filesViewModel.navigateBack()
                    } else {
                        navController.popBackStack()
                    }
                },
                onCreateFolder = { name -> filesViewModel.createFolder(name) },
                onDeleteFile = { file -> filesViewModel.deleteFile(file) },
                onDeleteFolder = { folder -> filesViewModel.deleteFolder(folder) },
                onRefresh = { filesViewModel.refresh() },
                onUploadClick = {
                    navController.navigate(Screen.FileUpload.createRoute(currentFolderId))
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        // File Upload Screen — رفع ملف
        composable(
            route = Screen.FileUpload.route,
            arguments = listOf(
                navArgument("folderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val folderIdStr = backStackEntry.arguments?.getString("folderId")
            val folderId = folderIdStr?.toIntOrNull()
            
            FileUploadScreen(
                currentFolderId = folderId,
                onBackClick = {
                    navController.popBackStack()
                },
                onUploadSuccess = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                    navController.popBackStack()
                }
            )
        }
        
        // Wallet Screen
        composable(Screen.Wallet.route) {
            WalletScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onWithdrawClick = {
                    navController.navigate(Screen.Withdraw.route)
                },
                onReferralClick = {
                    navController.navigate(Screen.Referral.route)
                }
            )
        }
        
        // Withdraw Screen
        composable(Screen.Withdraw.route) {
            WithdrawScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Referral Screen
        composable(Screen.Referral.route) {
            ReferralScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Ad Gate Screen — 10s countdown before video/image viewing
        composable(
            route = Screen.AdGate.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType },
                navArgument("fileType") { type = NavType.StringType },
                navArgument("shareToken") { type = NavType.StringType },
                navArgument("fileName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getInt("fileId") ?: 0
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val fileType = backStackEntry.arguments?.getString("fileType") ?: "other"
            val shareToken = backStackEntry.arguments?.getString("shareToken") ?: ""
            
            AdGateScreen(
                fileName = fileName,
                fileType = fileType,
                onCountdownComplete = {
                    navController.navigate(
                        Screen.FileViewer.createRoute(fileId, fileName, fileType, shareToken)
                    ) {
                        popUpTo(Screen.AdGate.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // File Viewer Screen
        composable(
            route = Screen.FileViewer.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType },
                navArgument("fileType") { type = NavType.StringType },
                navArgument("shareToken") { type = NavType.StringType },
                navArgument("fileName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getInt("fileId") ?: 0
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val fileType = backStackEntry.arguments?.getString("fileType") ?: "other"
            val shareToken = backStackEntry.arguments?.getString("shareToken") ?: ""
            
            FileViewerScreen(
                fileId = fileId,
                fileName = fileName,
                fileType = fileType,
                shareToken = shareToken,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Deep Link Viewer Screen - opens file by share token
        composable(
            route = Screen.DeepLinkViewer.route,
            arguments = listOf(
                navArgument("shareToken") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shareToken = backStackEntry.arguments?.getString("shareToken") ?: ""
            
            DeepLinkViewerScreen(
                shareToken = shareToken,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
