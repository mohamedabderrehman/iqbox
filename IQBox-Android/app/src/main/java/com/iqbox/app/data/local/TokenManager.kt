package com.iqbox.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_store")

/**
 * Token Manager - إدارة حفظ واسترجاع Token
 */
class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }
    
    /**
     * حفظ Token وبيانات المستخدم
     */
    suspend fun saveToken(token: String, userId: Int, email: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId.toString()
            preferences[USER_EMAIL_KEY] = email
            preferences[USERNAME_KEY] = username
        }
    }
    
    /**
     * الحصول على Token
     */
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    /**
     * الحصول على Token كـ String مباشر (للاستخدام في API calls)
     */
    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }
    
    /**
     * الحصول على User ID
     */
    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]?.toIntOrNull()
    }
    
    /**
     * الحصول على Email
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }
    
    /**
     * الحصول على Username
     */
    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }
    
    /**
     * حذف Token (Logout)
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USERNAME_KEY)
        }
    }
    
    /**
     * التحقق من وجود Token
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }
}
