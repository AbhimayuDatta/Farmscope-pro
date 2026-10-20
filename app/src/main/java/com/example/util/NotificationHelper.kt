package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "agrotech_farmsense_notifications"
    private const val CHANNEL_NAME = "AgroTech FarmSense Alerts"
    private const val CHANNEL_DESC = "Notifications for crop security and live sensor alerts"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerNotification(context: Context, title: String, message: String, notificationId: Int = 1) {
        // Disabled per user request - no message notifications
    }
}
