package com.raktaseva.connect.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun registerWithEmail(
        email: String,
        password: String
    ): AuthResult {

        val result = auth.createUserWithEmailAndPassword(email, password).await()

        val user = auth.currentUser

        if (user != null) {

            val db = FirebaseFirestore.getInstance()

            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to email
            )

            db.collection("users")
                .document(user.uid)
                .set(userMap)
                .await()
        }

        return result
    }

    suspend fun updateDisplayName(displayName: String) {
        val user = auth.currentUser ?: return
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()
        user.updateProfile(request).await()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult =
        auth.signInWithCredential(credential).await()

    fun buildPhoneAuthOptions(
        activity: android.app.Activity,
        phoneNumberE164: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): PhoneAuthOptions =
        PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

    fun verifyPhoneNumber(options: PhoneAuthOptions) {
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}