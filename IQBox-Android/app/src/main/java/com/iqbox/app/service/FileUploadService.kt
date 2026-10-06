package com.iqbox.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.iqbox.app.MainActivity
import com.iqbox.app.R
import com.iqbox.app.data.api.ApiClient
import com.iqbox.app.data.local.TokenManager
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class FileUploadService : Service() {

    companion object {
        const val CHANNEL_ID = "file_upload_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPLOAD = "com.iqbox.app.ACTION_UPLOAD"
        const val EXTRA_FILE_URI = "file_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_FOLDER_ID = "folder_id"

        fun startUpload(context: Context, fileUri: Uri, fileName: String?, folderId: Int?) {
            val intent = Intent(context, FileUploadService::class.java).apply {
                action = ACTION_UPLOAD
                putExtra(EXTRA_FILE_URI, fileUri.toString())
                putExtra(EXTRA_FILE_NAME, fileName)
                folderId?.let { putExtra(EXTRA_FOLDER_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private lateinit var tokenManager: TokenManager
    private val apiService = ApiClient.apiService

    private var uploadQueue = mutableListOf<UploadTask>()
    private var isUploading = false

    data class UploadTask(
        val uri: Uri,
        val fileName: String?,
        val folderId: Int?
    )

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        tokenManager = TokenManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPLOAD -> {
                val uriString = intent.getStringExtra(EXTRA_FILE_URI)
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
                val folderId = if (intent.hasExtra(EXTRA_FOLDER_ID)) intent.getIntExtra(EXTRA_FOLDER_ID, -1).takeIf { it != -1 } else null

                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    uploadQueue.add(UploadTask(uri, fileName, folderId))
                    processQueue()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "رفع الملفات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات رفع الملفات"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun processQueue() {
        if (isUploading || uploadQueue.isEmpty()) return

        isUploading = true
        val task = uploadQueue.removeAt(0)

        startForeground(NOTIFICATION_ID, createProgressNotification(task.fileName ?: "ملف", 0))

        serviceScope.launch {
            try {
                uploadFile(task)
            } catch (e: Exception) {
                showErrorNotification(task.fileName ?: "ملف", e.message ?: "خطأ غير معروف")
            } finally {
                isUploading = false
                if (uploadQueue.isNotEmpty()) {
                    processQueue()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private suspend fun uploadFile(task: UploadTask) {
        val token = tokenManager.getToken() ?: throw Exception("غير مسجل الدخول")

        // Update notification
        updateNotification(task.fileName ?: "ملف", 10)

        // Copy file to temp location
        val inputStream = contentResolver.openInputStream(task.uri)
            ?: throw Exception("لا يمكن قراءة الملف")

        val tempFile = File(cacheDir, task.fileName ?: "upload_${System.currentTimeMillis()}")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        updateNotification(task.fileName ?: "ملف", 30)

        // Prepare multipart request
        val requestFile = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

        val folderIdBody = task.folderId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = task.fileName?.toRequestBody("text/plain".toMediaTypeOrNull())

        updateNotification(task.fileName ?: "ملف", 50)

        // Upload
        val response = apiService.uploadFile(
            token = "Bearer $token",
            file = filePart,
            folderId = folderIdBody,
            name = nameBody
        )

        // Clean up temp file
        tempFile.delete()

        updateNotification(task.fileName ?: "ملف", 90)

        if (response.isSuccessful && response.body()?.success == true) {
            showSuccessNotification(task.fileName ?: "ملف")
        } else {
            throw Exception(response.body()?.message ?: "فشل في الرفع")
        }
    }

    private fun createProgressNotification(fileName: String, progress: Int): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("جاري رفع الملف...")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(fileName: String, progress: Int) {
        val notification = createProgressNotification(fileName, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showSuccessNotification(fileName: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تم رفع الملف بنجاح")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(fileName: String, error: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("فشل رفع الملف")
            .setContentText("$fileName: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
