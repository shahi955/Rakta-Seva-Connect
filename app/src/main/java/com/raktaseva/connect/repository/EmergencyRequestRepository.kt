package com.raktaseva.connect.repository

import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.model.firestore.NotificationDocument
import com.raktaseva.connect.model.firestore.NotificationTypes
import com.raktaseva.connect.util.DonorDistance
import java.util.Locale

private const val NOTIFY_TITLE = "URGENT BLOOD REQUEST"
private const val MAX_NOTIFY_DONORS = 40

data class EmergencySubmitResult(
    val requestId: String,
    val matchedDonors: Int,
    val notificationsSent: Int
)

/**
 * Creates a blood request, finds eligible nearby donors, and writes [NotificationDocument] rows for each.
 * Push (FCM) should be triggered from a Cloud Function on notification create or on blood_requests write.
 */
class EmergencyRequestRepository(
    private val bloodRequestsRepository: BloodRequestsRepository = BloodRequestsRepository(),
    private val usersRepository: UsersRepository = UsersRepository(),
    private val notificationsRepository: NotificationsRepository = NotificationsRepository()
) {

    suspend fun submitEmergencyRequest(
        creatorUid: String,
        patientName: String,
        bloodGroupNeeded: String,
        unitsRequired: Long,
        hospitalName: String,
        latitude: Double,
        longitude: Double,
        contactNumber: String,
        emergencyLevel: String,
        notes: String
    ): EmergencySubmitResult {
        val doc = BloodRequestDocument.newOpenRequest(
            createdBy = creatorUid,
            patientName = patientName,
            bloodGroupNeeded = bloodGroupNeeded,
            unitsRequired = unitsRequired,
            hospitalName = hospitalName,
            latitude = latitude,
            longitude = longitude,
            contactNumber = contactNumber,
            emergencyLevel = emergencyLevel,
            notes = notes
        )

        val requestId = bloodRequestsRepository.create(doc)

        val matches: List<DonorDistance> = usersRepository.findEligibleNearbyDonors(
            bloodGroupNeeded = bloodGroupNeeded,
            requestLatitude = latitude,
            requestLongitude = longitude
        ).filter { it.user.id != creatorUid }
            .take(MAX_NOTIFY_DONORS)

        val notifications = matches.map { match ->
            val distanceLabel = String.format(Locale.US, "%.1f", match.distanceKm)
            val body = "${bloodGroupNeeded} at $hospitalName (~${distanceLabel} km). Tap to view."
            NotificationDocument(
                id = "",
                userId = match.user.id,
                title = NOTIFY_TITLE,
                body = body,
                type = NotificationTypes.BLOOD_REQUEST,
                read = false,
                payload = mapOf(
                    "bloodGroup" to bloodGroupNeeded,
                    "hospitalName" to hospitalName,
                    "distanceKm" to distanceLabel.toString(),
                    "patientName" to patientName,
                    "emergencyLevel" to emergencyLevel,
                    "requestId" to requestId
                ),
                relatedRequestId = requestId,
                createdAt = null
            )
        }

        notificationsRepository.createBatch(notifications)

        return EmergencySubmitResult(
            requestId = requestId,
            matchedDonors = matches.size,
            notificationsSent = notifications.size
        )
    }
}
