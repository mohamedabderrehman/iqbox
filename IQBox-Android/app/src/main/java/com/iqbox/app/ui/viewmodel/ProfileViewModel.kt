package com.iqbox.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.R
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.UserProfile
import com.iqbox.app.data.api.UserStats
import com.iqbox.app.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Profile ViewModel - إدارة بيانات صفحة الملف الشخصي
 */
class ProfileViewModel(private val appContext: Context) : ViewModel() {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(appContext)
    
    // بيانات المستخدم
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    // إحصائيات المستخدم
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()
    
    // حالة الاشتراك
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    
    // حالة التحميل
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // حالة الخطأ
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadProfile()
    }
    
    /**
     * تحميل بيانات الملف الشخصي والإحصائيات
     */
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    // تحميل الملف الشخصي
                    val profileResponse = apiService.getUserProfile("Bearer $token")
                    if (profileResponse.isSuccessful && profileResponse.body()?.success == true) {
                        _userProfile.value = profileResponse.body()!!.data.user
                        _isPremium.value = profileResponse.body()!!.data.user.subscription_status == "premium"
                    }
                    
                    // تحميل الإحصائيات
                    val statsResponse = apiService.getUserStats("Bearer $token")
                    if (statsResponse.isSuccessful && statsResponse.body()?.success == true) {
                        _userStats.value = statsResponse.body()!!.data.stats
                    }
                } else {
                    _error.value = appContext.getString(R.string.login_required)
                }
            } catch (e: Exception) {
                _error.value = appContext.getString(R.string.error_load_data, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * تسجيل الخروج
     */
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
        }
    }
    
    /**
     * مسح رسالة الخطأ
     */
    fun clearError() {
        _error.value = null
    }
}
