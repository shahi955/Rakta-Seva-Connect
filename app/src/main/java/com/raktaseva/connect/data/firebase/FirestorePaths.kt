package com.raktaseva.connect.data.firebase

/**
 * Collection / document path constants to avoid typos across repositories.
 * Donor fields live on [USERS] (`isDonor` + location + blood fields).
 */
object FirestorePaths {
    const val USERS = "users"
    const val BLOOD_REQUESTS = "blood_requests"
    const val DONATIONS = "donations"
    const val NOTIFICATIONS = "notifications"
}
