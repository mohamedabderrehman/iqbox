package com.iqbox.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.R
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.FileItem
import com.iqbox.app.data.local.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val appContext: Context) : ViewModel() {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(appContext)
    
    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var searchJob: Job? = null
    
    fun search(query: String) {
        _currentQuery.value = query
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            delay(300)
            
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    val response = apiService.getMyFiles(
                        token = "Bearer $token",
                        search = query
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        _searchResults.value = response.body()!!.data.files
                    } else {
                        _searchResults.value = emptyList()
                    }
                } else {
                    _error.value = appContext.getString(R.string.login_required)
                }
            } catch (e: Exception) {
                _error.value = appContext.getString(R.string.error_generic, e.message ?: "")
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        _currentQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        _error.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}
