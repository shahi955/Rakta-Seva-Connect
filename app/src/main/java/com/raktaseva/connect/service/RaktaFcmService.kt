package com.raktaseva.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.raktaseva.connect.R
import com.raktaseva.connect.RaktaApplication
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.notification.RaktaNotificationHelper
import kotlinx.coroutines.launch

/**
 * FCM entry point: token refresh → Firestore; incoming messages → high-priority local notification
 * with tap action to open the blood request detail screen.
 *
 * **Background + notification payload:** the system tray may handle display; tap still delivers
 * extras to [com.raktaseva.connect.MainActivity]. **Foreground:** we post a full-screen alert style
 * notification here so users always see urgent content.
 */
class RaktaFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? RaktaApplication ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        app.applicationScope.launch {
            runCatching { AppGraph.usersRepository.mergeFcmToken(uid, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val requestId = data["requestId"] ?: data["relatedRequestId"]
        val title = message.notification?.title
            ?: data["title"]
            ?: getString(R.string.fcm_default_title)
        val body = message.notification?.body
            ?: data["body"]
            ?: getString(R.string.fcm_default_body)

        EmergencyNotificationHelper.showEmergencyNotification(
            context = this,
            title = title,
            body = body,
            requestId = requestId
        )
    }

    companion object {
        const val CHANNEL_ID = "raktaseva_emergency"
    }
}
