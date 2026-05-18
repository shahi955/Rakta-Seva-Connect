package com.raktaseva.connect.model

/**
 * Stored on each user document under `role`. Only trusted operators should have [ADMIN]
 * (set manually in Firebase Console / Admin SDK). Registration always creates [USER].
 */
object UserRoles {
    const val USER = "user"
    const val ADMIN = "admin"
}
