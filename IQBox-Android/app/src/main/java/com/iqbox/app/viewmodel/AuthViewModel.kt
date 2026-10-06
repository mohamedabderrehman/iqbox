package com.iqbox.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.data.model.AuthResponse
import com.iqbox.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            repository.login(email, password)
                .onSuccess { response ->
                    _loginState.value = LoginState.Success(response)
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "حدث خطأ")
                }
        }
    }
    
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            repository.register(username, email, password)
                .onSuccess { response ->
                    _registerState.value = RegisterState.Success(response)
                }
                .onFailure { error ->
                    _registerState.value = RegisterState.Error(error.message ?: "حدث خطأ")
                }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val response: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val response: AuthResponse) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
