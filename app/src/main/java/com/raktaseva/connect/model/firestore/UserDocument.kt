package com.raktaseva.connect.model.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class UserDocument(
    val id: String,
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",
    val isDonor: Boolean = false,
    val isBlocked: Boolean = false,
    val bloodGroup: String? = null,
    val gender: String? = null,
    val age: Long? = null,
    val city: String? = null,
    val taluk: String? = null,
    val lastDonationDate: Timestamp? = null,
    val availabilityStatus: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val fcmTokens: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun from(snapshot: DocumentSnapshot): UserDocument? {
            if (!snapshot.exists()) return null
            return UserDocument(
                id = snapshot.id,
                displayName = snapshot.getString("displayName").orEmpty(),
                email = snapshot.getString("email").orEmpty(),
                phone = snapshot.getString("phone").orEmpty(),
                role = snapshot.getString("role") ?: "user",
                isDonor = snapshot.getBoolean("isDonor") ?: false,
                isBlocked = snapshot.getBoolean("isBlocked") ?: false,
                bloodGroup = snapshot.getString("bloodGroup"),
                gender = snapshot.getString("gender"),
                age = snapshot.getLong("age"),
                city = snapshot.getString("city"),
                taluk = snapshot.getString("taluk"),
                lastDonationDate = snapshot.getTimestamp("lastDonationDate"),
                availabilityStatus = snapshot.getString("availabilityStatus"),
                latitude = snapshot.getDouble("latitude"),
                longitude = snapshot.getDouble("longitude"),
                geohash = snapshot.getString("geohash"),
                fcmTokens = (snapshot.get("fcmTokens") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                createdAt = snapshot.getTimestamp("createdAt"),
                updatedAt = snapshot.getTimestamp("updatedAt")
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "displayName" to displayName,
        "email" to email,
        "phone" to phone,
        "role" to role,
        "isDonor" to isDonor,
        "isBlocked" to isBlocked,
        "bloodGroup" to bloodGroup,
        "gender" to gender,
        "age" to age,
        "city" to city,
        "taluk" to taluk,
        "lastDonationDate" to lastDonationDate,
        "availabilityStatus" to availabilityStatus,
        "latitude" to latitude,
        "longitude" to longitude,
        "geohash" to geohash,
        "fcmTokens" to fcmTokens,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

object DonorAvailability {
    const val AVAILABLE = "AVAILABLE"
    const val BUSY = "BUSY"
    const val UNAVAILABLE = "UNAVAILABLE"
}
