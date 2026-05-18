package com.raktaseva.connect.repository

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * FCM registration token and topics (server still sends most urgent pushes).
 */
class FcmRepository(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {

    suspend fun getToken(): String = messaging.token.await()

    suspend fun deleteToken() {
        messaging.deleteToken().await()
    }

    suspend fun subscribeToTopic(topic: String) {
        messaging.subscribeToTopic(topic).await()
    }

    suspend fun unsubscribeFromTopic(topic: String) {
        messaging.unsubscribeFromTopic(topic).await()
    }
}
