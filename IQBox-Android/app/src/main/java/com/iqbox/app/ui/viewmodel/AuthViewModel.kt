package com.iqbox.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.LoginRequest
import com.iqbox.app.data.api.RegisterRequest
import com.iqbox.app.data.local.TokenManager
import com.iqbox.app.data.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Auth ViewModel - إدارة حالة Authentication
 */
class AuthViewModel(context: Context) : ViewModel() {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)
    
    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    /**
     * Login - تسجيل الدخول
     */
    fun login(login: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            try {
                val request = LoginRequest(login = login, password = password)
                val response = apiService.login(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    if (authResponse.success) {
                        // حفظ Token وبيانات المستخدم
                        tokenManager.saveToken(
                            token = authResponse.data.token,
                            userId = authResponse.data.user.id,
                            email = authResponse.data.user.email,
                            username = authResponse.data.user.username
                        )
                        
                        _uiState.value = AuthUiState.Success(authResponse)
                    } else {
                        _uiState.value = AuthUiState.Error(authResponse.message)
                    }
                } else {
                    // محاولة قراءة رسالة الخطأ من الـ response
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        // يمكن parsing JSON error هنا إذا كان API يرجع JSON
                        "Login failed: ${response.code()}"
                    } catch (e: Exception) {
                        "Login failed: ${response.code()}"
                    }
                    _uiState.value = AuthUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    /**
     * Register - إنشاء حساب جديد
     */
    fun register(username: String, email: String, password: String, referralCode: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            try {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    referral_code = referralCode?.takeIf { it.isNotBlank() }
                )
                val response = apiService.register(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    if (authResponse.success) {
                        // حفظ Token وبيانات المستخدم
                        tokenManager.saveToken(
                            token = authResponse.data.token,
                            userId = authResponse.data.user.id,
                            email = authResponse.data.user.email,
                            username = authResponse.data.user.username
                        )
                        
                        _uiState.value = AuthUiState.Success(authResponse)
                    } else {
                        _uiState.value = AuthUiState.Error(authResponse.message)
                    }
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        "Registration failed: ${response.code()}"
                    } catch (e: Exception) {
                        "Registration failed: ${response.code()}"
                    }
                    _uiState.value = AuthUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    /**
     * Logout - تسجيل الخروج
     */
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            _uiState.value = AuthUiState.Idle
        }
    }
    
    /**
     * Reset UI State
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * Auth UI State - حالات واجهة Authentication
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val authResponse: AuthResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
