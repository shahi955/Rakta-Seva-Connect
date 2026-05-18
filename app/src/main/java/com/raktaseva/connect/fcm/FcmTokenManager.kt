package com.raktaseva.connect.fcm

import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.RaktaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Registers the device FCM token with Firestore for the signed-in user (max 5 tokens kept).
 * Network and Firestore I/O run off the main thread.
 */
object FcmTokenManager {

    fun syncCurrentUserTokenToFirestore(app: RaktaApplication) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        app.applicationScope.launch {
            runCatching {
                val token = withContext(Dispatchers.IO) { AppGraph.fcmRepository.getToken() }
                withContext(Dispatchers.IO) { AppGraph.usersRepository.mergeFcmToken(uid, token) }
            }
        }
    }
}
