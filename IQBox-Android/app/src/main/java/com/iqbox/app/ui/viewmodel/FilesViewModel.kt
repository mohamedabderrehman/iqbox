package com.iqbox.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iqbox.app.R
import com.iqbox.app.data.api.*
import com.iqbox.app.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class FilesViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(application)

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo.asStateFlow()

    private val _currentFolderId = MutableStateFlow<Int?>(null)
    val currentFolderId: StateFlow<Int?> = _currentFolderId.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<FolderItem>>(emptyList())
    val breadcrumbs: StateFlow<List<FolderItem>> = _breadcrumbs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadFiles()
        loadStorageInfo()
    }

    fun loadFiles(folderId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val token = tokenManager.getToken() ?: return@launch
                val folderIdStr = folderId?.toString() ?: "root"
                
                val response = apiService.getMyFiles(
                    token = "Bearer $token",
                    folderId = folderIdStr
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data
                    _files.value = data.files
                    _folders.value = data.folders
                    _currentFolderId.value = folderId
                } else {
                    _error.value = getApplication<Application>().getString(R.string.failed_load_data)
                }
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(R.string.error_load_data, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = apiService.getStorageInfo("Bearer $token")

                if (response.isSuccessful && response.body()?.success == true) {
                    _storageInfo.value = response.body()!!.data.storage
                }
            } catch (e: Exception) {
                // Silent fail for storage info
            }
        }
    }

    fun navigateToFolder(folder: FolderItem) {
        val currentBreadcrumbs = _breadcrumbs.value.toMutableList()
        currentBreadcrumbs.add(folder)
        _breadcrumbs.value = currentBreadcrumbs
        loadFiles(folder.id)
    }

    fun navigateBack() {
        val currentBreadcrumbs = _breadcrumbs.value.toMutableList()
        if (currentBreadcrumbs.isNotEmpty()) {
            currentBreadcrumbs.removeLast()
            _breadcrumbs.value = currentBreadcrumbs
            
            val parentId = currentBreadcrumbs.lastOrNull()?.id
            loadFiles(parentId)
        } else {
            loadFiles(null)
        }
    }

    fun navigateToBreadcrumb(folder: FolderItem) {
        val currentBreadcrumbs = _breadcrumbs.value.toMutableList()
        val index = currentBreadcrumbs.indexOfFirst { it.id == folder.id }
        if (index >= 0) {
            _breadcrumbs.value = currentBreadcrumbs.subList(0, index + 1)
            loadFiles(folder.id)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val request = CreateFolderRequest(
                    name = name,
                    parent_id = _currentFolderId.value
                )

                val response = apiService.createFolder("Bearer $token", request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = getApplication<Application>().getString(R.string.folder_created)
                    loadFiles(_currentFolderId.value)
                } else {
                    _error.value = response.body()?.message ?: getApplication<Application>().getString(R.string.folder_create_failed)
                }
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = apiService.deleteFile("Bearer $token", file.id)

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = getApplication<Application>().getString(R.string.file_deleted)
                    loadFiles(_currentFolderId.value)
                    loadStorageInfo()
                } else {
                    _error.value = getApplication<Application>().getString(R.string.file_delete_failed)
                }
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun deleteFolder(folder: FolderItem) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = apiService.deleteFolder("Bearer $token", folder.id)

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = getApplication<Application>().getString(R.string.folder_deleted)
                    loadFiles(_currentFolderId.value)
                    loadStorageInfo()
                } else {
                    _error.value = getApplication<Application>().getString(R.string.folder_delete_failed)
                }
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun uploadFile(context: Context, uri: Uri, fileName: String?) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            _error.value = null

            try {
                val token = tokenManager.getToken() ?: return@launch

                // Copy file to temp location
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, fileName ?: "upload_${System.currentTimeMillis()}")
                FileOutputStream(tempFile).use { output ->
                    inputStream?.copyTo(output)
                }
                inputStream?.close()

                val requestFile = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

                val folderIdBody = _currentFolderId.value?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val nameBody = fileName?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = apiService.uploadFile(
                    token = "Bearer $token",
                    file = filePart,
                    folderId = folderIdBody,
                    name = nameBody
                )

                // Clean up temp file
                tempFile.delete()

                if (response.isSuccessful && response.body()?.success == true) {
                    _successMessage.value = getApplication<Application>().getString(R.string.upload_complete_notif)
                    loadFiles(_currentFolderId.value)
                    loadStorageInfo()
                } else {
                    _error.value = response.body()?.message ?: getApplication<Application>().getString(R.string.upload_failed_msg)
                }
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(R.string.upload_error, e.message ?: "")
            } finally {
                _isUploading.value = false
                _uploadProgress.value = 0f
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun refresh() {
        loadFiles(_currentFolderId.value)
        loadStorageInfo()
    }
}
