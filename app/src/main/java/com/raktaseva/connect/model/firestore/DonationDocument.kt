package com.raktaseva.connect.model.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class DonationDocument(
    val id: String,
    val donorId: String = "",
    val requestId: String? = null,
    val units: Long = 1,
    val donationDate: Timestamp? = null,
    val hospitalName: String? = null,
    val notes: String? = null,
    val verified: Boolean = false,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun from(snapshot: DocumentSnapshot): DonationDocument? {
            if (!snapshot.exists()) return null
            return DonationDocument(
                id = snapshot.id,
                donorId = snapshot.getString("donorId").orEmpty(),
                requestId = snapshot.getString("requestId"),
                units = snapshot.getLong("units") ?: 1,
                donationDate = snapshot.getTimestamp("donationDate"),
                hospitalName = snapshot.getString("hospitalName"),
                notes = snapshot.getString("notes"),
                verified = snapshot.getBoolean("verified") ?: false,
                createdAt = snapshot.getTimestamp("createdAt")
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "donorId" to donorId,
        "requestId" to requestId,
        "units" to units,
        "donationDate" to donationDate,
        "hospitalName" to hospitalName,
        "notes" to notes,
        "verified" to verified,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
