package com.raktaseva.connect.model.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class BloodRequestDocument(
    val id: String,
    val createdBy: String = "",
    val patientName: String = "",
    val bloodGroupNeeded: String = "",
    val unitsRequired: Long = 1,
    val hospitalName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val contactNumber: String = "",
    val emergencyLevel: String = "MEDIUM",
    val notes: String = "",
    val status: String = RequestStatus.OPEN,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        /** Use with [com.raktaseva.connect.repository.BloodRequestsRepository.create]; id is assigned by Firestore. */
        fun newOpenRequest(
            createdBy: String,
            patientName: String,
            bloodGroupNeeded: String,
            unitsRequired: Long,
            hospitalName: String,
            latitude: Double,
            longitude: Double,
            contactNumber: String,
            emergencyLevel: String,
            notes: String
        ): BloodRequestDocument = BloodRequestDocument(
            id = "",
            createdBy = createdBy,
            patientName = patientName.trim(),
            bloodGroupNeeded = bloodGroupNeeded.trim(),
            unitsRequired = unitsRequired,
            hospitalName = hospitalName.trim(),
            latitude = latitude,
            longitude = longitude,
            geohash = null,
            contactNumber = contactNumber.trim(),
            emergencyLevel = emergencyLevel,
            notes = notes.trim(),
            status = RequestStatus.OPEN,
            createdAt = null,
            updatedAt = null
        )

        fun from(snapshot: DocumentSnapshot): BloodRequestDocument? {
            if (!snapshot.exists()) return null
            return BloodRequestDocument(
                id = snapshot.id,
                createdBy = snapshot.getString("createdBy").orEmpty(),
                patientName = snapshot.getString("patientName").orEmpty(),
                bloodGroupNeeded = snapshot.getString("bloodGroupNeeded").orEmpty(),
                unitsRequired = snapshot.getLong("unitsRequired") ?: 1,
                hospitalName = snapshot.getString("hospitalName").orEmpty(),
                latitude = snapshot.getDouble("latitude"),
                longitude = snapshot.getDouble("longitude"),
                geohash = snapshot.getString("geohash"),
                contactNumber = snapshot.getString("contactNumber").orEmpty(),
                emergencyLevel = snapshot.getString("emergencyLevel") ?: "MEDIUM",
                notes = snapshot.getString("notes").orEmpty(),
                status = snapshot.getString("status") ?: RequestStatus.OPEN,
                createdAt = snapshot.getTimestamp("createdAt"),
                updatedAt = snapshot.getTimestamp("updatedAt")
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "createdBy" to createdBy,
        "patientName" to patientName,
        "bloodGroupNeeded" to bloodGroupNeeded,
        "unitsRequired" to unitsRequired,
        "hospitalName" to hospitalName,
        "latitude" to latitude,
        "longitude" to longitude,
        "geohash" to geohash,
        "contactNumber" to contactNumber,
        "emergencyLevel" to emergencyLevel,
        "notes" to notes,
        "status" to status,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

object RequestStatus {
    const val OPEN = "OPEN"
    const val FULFILLED = "FULFILLED"
    const val CANCELLED = "CANCELLED"
}

object EmergencyLevel {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}
