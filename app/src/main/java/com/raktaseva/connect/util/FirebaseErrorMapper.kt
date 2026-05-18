package com.raktaseva.connect.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

fun Throwable.toUserMessage(): String {

    return when (this) {

        is FirebaseAuthUserCollisionException -> {
            "An account already exists with this email."
        }

        is FirebaseAuthWeakPasswordException -> {
            "Password is too weak. Use a stronger password."
        }

        is FirebaseAuthInvalidCredentialsException -> {
            "Incorrect email or password."
        }

        is FirebaseAuthInvalidUserException -> {
            "No account found for this email."
        }

        is FirebaseNetworkException -> {
            "Network error. Check your connection."
        }

        is FirebaseFirestoreException -> {

            when (this.code) {

                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    "You do not have permission to save this profile."
                }

                FirebaseFirestoreException.Code.UNAVAILABLE -> {
                    "Service unavailable. Try again."
                }

                FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                    val msg = localizedMessage ?: ""
                    if (msg.contains("index")) {
                        "Database index required. Please click this link to create it:\n\n$msg"
                    } else {
                        msg.ifBlank { "Precondition failed." }
                    }
                }

                else -> {
                    localizedMessage
                        ?: "Something went wrong while saving."
                }
            }
        }

        else -> {
            val msg = localizedMessage
            if (msg.isNullOrBlank()) "Registration failed. Please try again."
            else msg
        }
    }
}