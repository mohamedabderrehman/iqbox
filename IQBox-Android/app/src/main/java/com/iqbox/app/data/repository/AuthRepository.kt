package com.iqbox.app.data.repository

import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.api.LoginRequest
import com.iqbox.app.data.api.RegisterRequest
import com.iqbox.app.data.model.AuthResponse

class AuthRepository {
    private val apiService = ApiClient.apiService
    
    suspend fun login(login: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(login, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
