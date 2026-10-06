package com.iqbox.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.iqbox.app.MainActivity
import com.iqbox.app.R

/**
 * Persistent foreground notification with quick-action buttons:
 * Files, Earnings, Profile, Upload
 */
class PersistentNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "iqbox_persistent_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_NAV = "com.iqbox.app.ACTION_NAV"
        const val EXTRA_DESTINATION = "destination"

        fun start(context: Context) {
            val intent = Intent(context, PersistentNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PersistentNotificationService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun navPendingIntent(destination: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_NAV
            putExtra(EXTRA_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, destination.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showNotification() {
        val contentIntent = navPendingIntent("home")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_quick_access))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_gallery,
                getString(R.string.nav_files),
                navPendingIntent("files")
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                getString(R.string.earnings),
                navPendingIntent("wallet")
            )
            .addAction(
                android.R.drawable.ic_menu_myplaces,
                getString(R.string.nav_profile),
                navPendingIntent("profile")
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
