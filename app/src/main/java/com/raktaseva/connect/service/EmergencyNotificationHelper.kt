package com.raktaseva.connect.service

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.raktaseva.connect.R
import com.raktaseva.connect.notification.RaktaNotificationHelper

/**
 * Builds and posts high-importance local notifications for emergency blood requests.
 *
 * On Android 13+ the app must hold [android.Manifest.permission.POST_NOTIFICATIONS]
 * or the system may suppress the notification; callers should request this at runtime.
 */
object EmergencyNotificationHelper {

    private const val NOTIFICATION_ID_BASE = 20_000

    fun showEmergencyNotification(
        context: Context,
        title: String,
        body: String,
        requestId: String?
    ) {
        val channelId = RaktaFcmService.CHANNEL_ID
        createChannelIfNeeded(context, channelId)

        val notificationId = NOTIFICATION_ID_BASE + ((requestId?.hashCode() ?: 0) and 0x7fff)
        val pendingIntent = if (!requestId.isNullOrBlank()) {
            RaktaNotificationHelper.createOpenRequestPendingIntent(context, requestId, notificationId)
        } else {
            RaktaNotificationHelper.createMainActivityPendingIntent(context, notificationId)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    private fun createChannelIfNeeded(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) != null) return
        val channel = android.app.NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_emergency_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_emergency_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        nm.createNotificationChannel(channel)
    }
}
