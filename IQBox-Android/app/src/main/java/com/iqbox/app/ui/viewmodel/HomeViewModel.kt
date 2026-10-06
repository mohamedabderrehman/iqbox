package com.iqbox.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.R
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FileItem
import com.iqbox.app.data.api.UserStats
import com.iqbox.app.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val appContext: Context) : ViewModel() {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(appContext)
    
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()
    
    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadAllData()
    }
    
    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    launch { loadUserStats(token) }
                    launch { loadRecentFiles(token) }
                }
            } catch (e: Exception) {
                _error.value = appContext.getString(R.string.error_load_data, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadUserStats(token: String) {
        try {
            val response = apiService.getUserStats("Bearer $token")
            if (response.isSuccessful && response.body()?.success == true) {
                _userStats.value = response.body()!!.data.stats
            }
        } catch (_: Exception) {}
    }
    
    private suspend fun loadRecentFiles(token: String) {
        try {
            val response = apiService.getMyFiles("Bearer $token")
            if (response.isSuccessful && response.body()?.success == true) {
                _recentFiles.value = response.body()!!.data.files.take(5)
            }
        } catch (_: Exception) {}
    }
    
    fun deleteFile(fileId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    val response = apiService.deleteFile("Bearer $token", fileId)
                    if (response.isSuccessful) {
                        loadAllData()
                    }
                }
            } catch (_: Exception) {
                _error.value = appContext.getString(R.string.file_delete_failed)
            }
        }
    }
    
    fun refresh() {
        loadAllData()
    }
    
    fun clearError() {
        _error.value = null
    }
}
