package com.iqbox.app.navigation

import android.net.Uri

/**
 * Navigation Routes
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object PromoSplash : Screen("promo_splash")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object HelpSupport : Screen("help_support")
    object Search : Screen("search")
    object Files : Screen("files")
    object FileUpload : Screen("file_upload?folderId={folderId}") {
        fun createRoute(folderId: Int? = null): String {
            return if (folderId != null) "file_upload?folderId=$folderId" else "file_upload"
        }
    }
    object Subscription : Screen("subscription")
    object Wallet : Screen("wallet")
    object Withdraw : Screen("withdraw")
    object Referral : Screen("referral")
    object AdGate : Screen("ad_gate/{fileId}/{fileType}/{shareToken}?fileName={fileName}") {
        fun createRoute(fileId: Int, fileName: String, fileType: String, shareToken: String): String {
            return "ad_gate/$fileId/$fileType/$shareToken?fileName=${Uri.encode(fileName)}"
        }
    }
    object FileViewer : Screen("file_viewer/{fileId}/{fileType}/{shareToken}?fileName={fileName}") {
        fun createRoute(fileId: Int, fileName: String, fileType: String, shareToken: String): String {
            return "file_viewer/$fileId/$fileType/$shareToken?fileName=${Uri.encode(fileName)}"
        }
    }
    object DeepLinkViewer : Screen("deep_link_viewer/{shareToken}") {
        fun createRoute(shareToken: String): String {
            return "deep_link_viewer/$shareToken"
        }
    }
}
