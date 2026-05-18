package com.raktaseva.connect.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.raktaseva.connect.MainActivity

/**
 * Builds tap intents and intent extras for FCM → [MainActivity] → request detail.
 */
object RaktaNotificationHelper {

    const val EXTRA_REQUEST_ID = "request_id"
    const val ACTION_OPEN_BLOOD_REQUEST = "com.raktaseva.connect.OPEN_BLOOD_REQUEST"

    fun createOpenRequestPendingIntent(
        context: Context,
        requestId: String,
        notificationId: Int
    ): PendingIntent {
        val intent = createOpenRequestIntent(context, requestId)
        return pendingActivity(context, notificationId, intent)
    }

    fun createMainActivityPendingIntent(context: Context, notificationId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return pendingActivity(context, notificationId, intent)
    }

    private fun pendingActivity(context: Context, notificationId: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, notificationId, intent, flags)
    }

    fun createOpenRequestIntent(context: Context, requestId: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_BLOOD_REQUEST
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, requestId)
        }

    /**
     * Merges FCM [RemoteMessage.getData] keys (may include `requestId` from Cloud Functions).
     */
    fun readRequestIdFromExtras(extras: android.os.Bundle?): String? {
        if (extras == null) return null
        return extras.getString(EXTRA_REQUEST_ID)
            ?: extras.getString("requestId")
            ?: extras.getString("relatedRequestId")
    }
}
