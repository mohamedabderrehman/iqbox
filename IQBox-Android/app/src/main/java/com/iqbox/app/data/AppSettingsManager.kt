package com.iqbox.app.data

import android.content.Context
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AppSettingsManager - إدارة إعدادات التطبيق (اسم التطبيق، اللوجو، الألوان)
 * يتم تحميل الإعدادات من الـ API ويمكن للأدمن تغييرها
 */
object AppSettingsManager {
    private val apiService = ApiClient.apiService
    
    // إعدادات التطبيق
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    // حالة التحميل
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()
    
    // اسم التطبيق
    val appName: String
        get() = _settings.value.app_name
    
    // رابط اللوجو
    val logoUrl: String?
        get() = _settings.value.app_logo_url
    
    // رابط الأيقونة
    val iconUrl: String?
        get() = _settings.value.app_icon_url
    
    // الألوان
    val primaryColor: String
        get() = _settings.value.primary_color
    
    val secondaryColor: String
        get() = _settings.value.secondary_color
    
    // الدعم
    val supportEmail: String?
        get() = _settings.value.support_email
    
    val supportPhone: String?
        get() = _settings.value.support_phone
    
    val supportTelegram: String?
        get() = _settings.value.support_telegram
    
    // التخزين
    val freeStorageLimitGB: Int
        get() = _settings.value.free_storage_limit_gb
    
    val premiumStorageLimitGB: Int
        get() = _settings.value.premium_storage_limit_gb
    
    // الأرباح
    val earningPer1000Views: Double
        get() = _settings.value.earning_per_1000_views
    
    val referralPercentage: Double
        get() = _settings.value.referral_percentage
    
    val gateAdEnabled: Boolean
        get() = _settings.value.gate_ad_enabled
    
    /**
     * تحميل الإعدادات من الـ API
     */
    suspend fun loadSettings() {
        try {
            val response = apiService.getAppSettings()
            if (response.isSuccessful && response.body()?.success == true) {
                _settings.value = response.body()!!.data.settings
                _isLoaded.value = true
            }
        } catch (e: Exception) {
            // استخدام القيم الافتراضية في حالة الخطأ
            _settings.value = AppSettings()
            _isLoaded.value = true
        }
    }
    
    /**
     * إعادة تحميل الإعدادات
     */
    suspend fun refresh() {
        loadSettings()
    }
}
