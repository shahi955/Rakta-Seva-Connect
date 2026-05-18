package com.raktaseva.connect.data.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

/**
 * Central access to Firebase SDK singletons.
 * Keeps construction in one place for tests (swap with fakes via DI later).
 */
object FirebaseBootstrap {

    fun ensureInitialized(app: android.app.Application) {
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }
    }

    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    fun storage(): FirebaseStorage = FirebaseStorage.getInstance()

    fun messaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
