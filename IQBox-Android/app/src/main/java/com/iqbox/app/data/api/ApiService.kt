package com.iqbox.app.data.api

import com.iqbox.app.data.model.AuthResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ==================== Authentication ====================
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>
    
    // ==================== Users ====================
    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>
    
    @PUT("users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<BaseResponse>
    
    @GET("users/stats")
    suspend fun getUserStats(
        @Header("Authorization") token: String
    ): Response<UserStatsResponse>
    
    // ==================== Subscription ====================
    @GET("subscription/status")
    suspend fun getSubscriptionStatus(
        @Header("Authorization") token: String
    ): Response<SubscriptionResponse>
    
    @GET("subscription/plans")
    suspend fun getSubscriptionPlans(): Response<SubscriptionPlansResponse>
    
    @GET("subscription/plans/{id}")
    suspend fun getSubscriptionPlan(
        @Path("id") planId: Int
    ): Response<SubscriptionPlanResponse>
    
    @POST("subscription/request")
    suspend fun createPaymentRequest(
        @Header("Authorization") token: String,
        @Body request: PaymentRequestBody
    ): Response<PaymentRequestResponse>
    
    @GET("subscription/requests")
    suspend fun getPaymentRequests(
        @Header("Authorization") token: String
    ): Response<PaymentRequestsResponse>
    
    @DELETE("subscription/requests/{id}")
    suspend fun cancelPaymentRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<BaseResponse>
    
    // ==================== App Settings ====================
    @GET("settings/app")
    suspend fun getAppSettings(): Response<AppSettingsResponse>
    
    // ==================== Wallet & Earnings ====================
    @GET("wallet")
    suspend fun getWallet(
        @Header("Authorization") token: String
    ): Response<WalletResponse>
    
    @GET("wallet/transactions")
    suspend fun getWalletTransactions(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<TransactionsResponse>
    
    @GET("wallet/earnings")
    suspend fun getEarningsStats(
        @Header("Authorization") token: String
    ): Response<EarningsStatsResponse>
    
    @GET("wallet/withdrawal-methods")
    suspend fun getWithdrawalMethods(
        @Header("Authorization") token: String
    ): Response<WithdrawalMethodsResponse>
    
    @POST("wallet/withdraw")
    suspend fun requestWithdrawal(
        @Header("Authorization") token: String,
        @Body request: WithdrawalRequest
    ): Response<BaseResponse>
    
    @GET("wallet/withdrawals")
    suspend fun getWithdrawalRequests(
        @Header("Authorization") token: String
    ): Response<WithdrawalRequestsResponse>
    
    @DELETE("wallet/withdrawals/{id}")
    suspend fun cancelWithdrawal(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<BaseResponse>
    
    // ==================== Referral ====================
    @GET("referral/my-code")
    suspend fun getReferralCode(
        @Header("Authorization") token: String
    ): Response<ReferralCodeResponse>
    
    @GET("referral/stats")
    suspend fun getReferralStats(
        @Header("Authorization") token: String
    ): Response<ReferralStatsResponse>
    
    @GET("referral/referred-users")
    suspend fun getReferredUsers(
        @Header("Authorization") token: String
    ): Response<ReferredUsersResponse>
    
    @GET("referral/validate/{code}")
    suspend fun validateReferralCode(
        @Path("code") code: String
    ): Response<ValidateReferralResponse>
    
    // ==================== Profile ====================
    @PUT("users/password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<BaseResponse>
    
    // ==================== Files ====================
    @Multipart
    @POST("files/upload")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("folder_id") folderId: RequestBody?,
        @Part("name") name: RequestBody?
    ): Response<FileUploadResponse>
    
    @GET("files/my-files")
    suspend fun getMyFiles(
        @Header("Authorization") token: String,
        @Query("folder_id") folderId: String? = null,
        @Query("type") type: String? = null,
        @Query("sort") sort: String = "date",
        @Query("order") order: String = "desc",
        @Query("search") search: String? = null
    ): Response<FilesResponse>
    
    @GET("files/storage")
    suspend fun getStorageInfo(
        @Header("Authorization") token: String
    ): Response<StorageResponse>
    
    @GET("files/info/{shareToken}")
    suspend fun getFileInfo(
        @Path("shareToken") shareToken: String
    ): Response<FileInfoResponse>
    
    @DELETE("files/{id}")
    suspend fun deleteFile(
        @Header("Authorization") token: String,
        @Path("id") fileId: Int
    ): Response<BaseResponse>
    
    // ==================== Folders ====================
    @POST("folders")
    suspend fun createFolder(
        @Header("Authorization") token: String,
        @Body request: CreateFolderRequest
    ): Response<FolderResponse>
    
    @GET("folders")
    suspend fun getFolders(
        @Header("Authorization") token: String,
        @Query("parent_id") parentId: String? = null
    ): Response<FoldersResponse>
    
    @GET("folders/{id}/contents")
    suspend fun getFolderContents(
        @Header("Authorization") token: String,
        @Path("id") folderId: Int
    ): Response<FolderContentsResponse>
    
    @PUT("folders/{id}")
    suspend fun renameFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: Int,
        @Body request: RenameFolderRequest
    ): Response<BaseResponse>
    
    @DELETE("folders/{id}")
    suspend fun deleteFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: Int
    ): Response<BaseResponse>
}

// Request Models
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val referral_code: String? = null
)

data class LoginRequest(
    val login: String,
    val password: String
)

// Request Models
data class UpdateProfileRequest(
    val username: String
)

// Response Models
data class BaseResponse(
    val success: Boolean,
    val message: String
)

data class UserProfileResponse(
    val success: Boolean,
    val data: UserProfileData
)

data class UserProfileData(
    val user: UserProfile
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    val subscription_status: String,
    val subscription_expiry: String? = null,
    val created_at: String? = null,
    val last_login: String? = null,
    val videos_count: Int = 0,
    val library_count: Int = 0
)

data class SubscriptionResponse(
    val success: Boolean,
    val data: SubscriptionData
)

data class SubscriptionData(
    val subscription_status: String,
    val subscription_expiry: String? = null,
    val is_premium: Boolean
)

data class UserStatsResponse(
    val success: Boolean,
    val data: UserStatsData
)

data class UserStatsData(
    val stats: UserStats
)

data class UserStats(
    val videos_count: Int = 0,
    val files_count: Int = 0,
    val total_views: Int = 0,
    val total_downloads: Int = 0,
    val total_storage: Long = 0,
    val total_likes: Int = 0
)

// ==================== Subscription Models ====================
data class PaymentRequestBody(
    val plan_id: Int,
    val payment_method: String? = null,
    val notes: String? = null
)

data class SubscriptionPlansResponse(
    val success: Boolean,
    val data: SubscriptionPlansData
)

data class SubscriptionPlansData(
    val plans: List<SubscriptionPlan>
)

data class SubscriptionPlanResponse(
    val success: Boolean,
    val data: SubscriptionPlanData
)

data class SubscriptionPlanData(
    val plan: SubscriptionPlan
)

data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val name_ar: String? = null,
    val description: String? = null,
    val description_ar: String? = null,
    val price: Double,
    val currency: String = "USD",
    val duration_days: Int,
    val features: List<String>? = null,
    val payment_instructions: String? = null,
    val payment_instructions_ar: String? = null
)

data class PaymentRequestResponse(
    val success: Boolean,
    val message: String,
    val data: PaymentRequestData? = null
)

data class PaymentRequestData(
    val request: PaymentRequest
)

data class PaymentRequestsResponse(
    val success: Boolean,
    val data: PaymentRequestsData
)

data class PaymentRequestsData(
    val requests: List<PaymentRequest>
)

data class PaymentRequest(
    val id: Int,
    val amount: Double,
    val currency: String,
    val payment_method: String? = null,
    val status: String,
    val admin_notes: String? = null,
    val created_at: String,
    val processed_at: String? = null,
    val plan_name: String? = null,
    val plan_name_ar: String? = null,
    val duration_days: Int? = null
)

// ==================== App Settings Models ====================
data class AppSettingsResponse(
    val success: Boolean,
    val data: AppSettingsData
)

data class AppSettingsData(
    val settings: AppSettings
)

data class AppSettings(
    val app_name: String = "IQBox",
    val app_logo_url: String? = null,
    val app_icon_url: String? = null,
    val primary_color: String = "#3B82F6",
    val secondary_color: String = "#1E3A8A",
    val support_email: String? = null,
    val support_phone: String? = null,
    val support_telegram: String? = null,
    val terms_url: String? = null,
    val privacy_url: String? = null,
    val free_storage_limit_gb: Int = 10,
    val premium_storage_limit_gb: Int = 200,
    val earning_per_1000_views: Double = 0.50,
    val referral_percentage: Double = 10.0,
    val gate_ad_enabled: Boolean = true
)

// ==================== Wallet & Earnings Models ====================
data class WalletResponse(
    val success: Boolean,
    val data: WalletData
)

data class WalletData(
    val wallet: Wallet,
    val earnings_breakdown: EarningsBreakdown,
    val recent_transactions: List<WalletTransaction>
)

data class Wallet(
    val balance: Double,
    val total_earned: Double,
    val total_withdrawn: Double,
    val pending_withdrawal: Double
)

data class EarningsBreakdown(
    val views_earnings: Double,
    val referral_earnings: Double
)

data class WalletTransaction(
    val id: Int,
    val type: String,
    val amount: Double,
    val balance_after: Double,
    val description: String?,
    val created_at: String
)

data class TransactionsResponse(
    val success: Boolean,
    val data: TransactionsData
)

data class TransactionsData(
    val transactions: List<WalletTransaction>,
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int
)

data class EarningsStatsResponse(
    val success: Boolean,
    val data: EarningsStats
)

data class EarningsStats(
    val today: Double,
    val this_week: Double,
    val this_month: Double,
    val earning_rate_per_1000: Double,
    val top_earning_videos: List<VideoEarning>
)

data class VideoEarning(
    val id: Int,
    val title: String,
    val views_count: Int,
    val paid_views_count: Int,
    val total_earned: Double
)

data class WithdrawalMethodsResponse(
    val success: Boolean,
    val data: WithdrawalMethodsData
)

data class WithdrawalMethodsData(
    val min_withdrawal_amount: Double,
    val methods: List<WithdrawalMethod>
)

data class WithdrawalMethod(
    val id: String,
    val name: String,
    val name_en: String,
    val region: String,
    val fields: List<WithdrawalField>
)

data class WithdrawalField(
    val key: String,
    val label: String,
    val type: String
)

data class WithdrawalRequest(
    val amount: Double,
    val method: String,
    val account_details: Map<String, String>
)

data class WithdrawalRequestsResponse(
    val success: Boolean,
    val data: WithdrawalRequestsData
)

data class WithdrawalRequestsData(
    val requests: List<WithdrawalRequestItem>
)

data class WithdrawalRequestItem(
    val id: Int,
    val amount: Double,
    val withdrawal_method: String,
    val status: String,
    val admin_notes: String?,
    val created_at: String,
    val processed_at: String?
)

// ==================== Referral Models ====================
data class ReferralCodeResponse(
    val success: Boolean,
    val data: ReferralCodeData
)

data class ReferralCodeData(
    val referral_code: String,
    val referral_link: String,
    val referral_percentage: Double
)

data class ReferralStatsResponse(
    val success: Boolean,
    val data: ReferralStats
)

data class ReferralStats(
    val total_referrals: Int,
    val total_earnings: Double,
    val month_earnings: Double,
    val referral_percentage: Double
)

data class ReferredUsersResponse(
    val success: Boolean,
    val data: ReferredUsersData
)

data class ReferredUsersData(
    val referred_users: List<ReferredUser>,
    val pagination: Pagination
)

data class ReferredUser(
    val id: Int,
    val username: String,
    val joined_at: String,
    val total_earnings: Double,
    val status: String
)

data class ValidateReferralResponse(
    val success: Boolean,
    val valid: Boolean,
    val data: ValidateReferralData? = null
)

data class ValidateReferralData(
    val referrer_username: String
)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

// ==================== Files & Folders Models ====================
data class FileItem(
    val id: Int,
    val name: String,
    val original_name: String? = null,
    val file_type: String,
    val file_size: Long,
    val share_token: String = "",
    val share_url: String? = null,
    val views_count: Int = 0,
    val downloads_count: Int = 0,
    val folder_id: Int? = null,
    val created_at: String,
    val updated_at: String? = null
)

data class FolderItem(
    val id: Int,
    val name: String,
    val parent_id: Int? = null,
    val share_token: String = "",
    val share_url: String? = null,
    val is_shared: Boolean = false,
    val size: Long = 0,
    val files_count: Int = 0,
    val created_at: String
)

data class FileUploadResponse(
    val success: Boolean,
    val message: String,
    val data: FileUploadData? = null
)

data class FileUploadData(
    val file: FileItem
)

data class FilesResponse(
    val success: Boolean,
    val data: FilesData
)

data class FilesData(
    val files: List<FileItem>,
    val folders: List<FolderItem>,
    val count: Int,
    val folders_count: Int
)

data class StorageResponse(
    val success: Boolean,
    val data: StorageData
)

data class StorageData(
    val storage: StorageInfo,
    val stats: FileStats
)

data class StorageInfo(
    val used: Long,
    val limit: Long,
    val percentage: Int,
    val is_premium: Boolean,
    val total_uploaded: Long,
    val total_downloaded: Long
)

data class FileStats(
    val total_files: Int,
    val videos_count: Int,
    val images_count: Int,
    val documents_count: Int,
    val other_count: Int
)

data class FileInfoResponse(
    val success: Boolean,
    val data: FileInfoData? = null,
    val message: String? = null
)

data class FileInfoData(
    val file: FileInfoItem
)

data class FileInfoItem(
    val id: Int,
    val name: String,
    val original_name: String? = null,
    val file_type: String,
    val file_size: Long = 0,
    val mime_type: String? = null,
    val share_token: String = "",
    val views_count: Int = 0,
    val downloads_count: Int = 0,
    val owner_name: String? = null,
    val created_at: String? = null
)

data class CreateFolderRequest(
    val name: String,
    val parent_id: Int? = null
)

data class RenameFolderRequest(
    val name: String
)

data class FolderResponse(
    val success: Boolean,
    val message: String,
    val data: FolderResponseData? = null
)

data class FolderResponseData(
    val folder: FolderItem
)

data class FoldersResponse(
    val success: Boolean,
    val data: FoldersData
)

data class FoldersData(
    val folders: List<FolderItem>,
    val count: Int
)

data class FolderContentsResponse(
    val success: Boolean,
    val data: FolderContentsData
)

data class FolderContentsData(
    val folder: FolderItem,
    val folders: List<FolderItem>,
    val files: List<FileItem>,
    val folders_count: Int,
    val files_count: Int
)
