package com.raktaseva.connect.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.raktaseva.connect.data.firebase.FirestorePaths
import com.raktaseva.connect.model.firestore.DonorAvailability
import com.raktaseva.connect.model.firestore.UserDocument
import com.raktaseva.connect.util.DonorDistance
import com.raktaseva.connect.util.DonorFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * CRUD + donor discovery queries on [FirestorePaths.USERS].
 */
class UsersRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun users() = db.collection(FirestorePaths.USERS)

    suspend fun getUser(uid: String): UserDocument? =
        UserDocument.from(users().document(uid).get().await())

    suspend fun upsertUser(uid: String, user: UserDocument) {
        users().document(uid).set(user.toFirestoreMap(), SetOptions.merge()).await()
    }

    suspend fun updateUser(uid: String, fields: Map<String, Any?>) {
        val cleaned = fields.filterValues { it != null }
        if (cleaned.isNotEmpty()) {
            users().document(uid).update(cleaned).await()
        }
    }

    suspend fun deleteUser(uid: String) {
        users().document(uid).delete().await()
    }

    fun userFlow(uid: String): Flow<UserDocument?> = callbackFlow {
        val reg = users().document(uid).addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            trySend(snap?.let { UserDocument.from(it) })
        }
        awaitClose { reg.remove() }
    }

    /**
     * Firestore pre-filter: matching blood group + self-declared available donor.
     * Apply [DonorFilter] / [com.raktaseva.connect.util.GeoUtils] on the client for 90-day and 10 km rules.
     */
    suspend fun queryActiveDonorsByBloodGroup(
        bloodGroup: String,
        limit: Long = 100
    ): List<UserDocument> {
        val snap = users()
            .whereEqualTo("isDonor", true)
            .whereEqualTo("bloodGroup", bloodGroup)
            .whereEqualTo("availabilityStatus", DonorAvailability.AVAILABLE)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { UserDocument.from(it) }
            .filter { !it.isBlocked }
    }

    /**
     * Full pipeline: query → hide last 90-day post-donation → keep within [radiusKm].
     * Sorted by ascending distance when coordinates exist.
     */
    suspend fun findEligibleNearbyDonors(
        bloodGroupNeeded: String,
        requestLatitude: Double,
        requestLongitude: Double,
        radiusKm: Double = 10.0,
        cooldownDays: Long = 90L,
        candidateLimit: Long = 100
    ): List<DonorDistance> {
        val candidates = queryActiveDonorsByBloodGroup(bloodGroupNeeded, candidateLimit)
        return DonorFilter.filterEligibleNearbyDonors(
            users = candidates,
            centerLat = requestLatitude,
            centerLng = requestLongitude,
            radiusKm = radiusKm,
            cooldownDays = cooldownDays
        )
    }

    suspend fun mergeFcmToken(uid: String, token: String) {
        if (token.isBlank()) return
        db.runTransaction { tx ->
            val ref = users().document(uid)
            val snap = tx.get(ref)
            val current = (snap.get("fcmTokens") as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
                ?: mutableListOf()
            if (!current.contains(token)) current.add(token)
            val trimmed = current.takeLast(5)
            tx.set(
                ref,
                mapOf("fcmTokens" to trimmed, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
        }.await()
    }

    suspend fun touchUpdatedAt(uid: String) {
        users().document(uid).update("updatedAt", FieldValue.serverTimestamp()).await()
    }

    /**
     * Admin moderation: list recent user profiles (newest first).
     */
    suspend fun listUsersForAdmin(limit: Long = 120): List<UserDocument> {
        val snap = users()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { UserDocument.from(it) }
    }

    /**
     * Admin moderation: block or unblock a user (cannot target self in UI; rules forbid self-admin update).
     */
    suspend fun setUserBlockedByAdmin(targetUid: String, blocked: Boolean) {
        updateUser(
            targetUid,
            mapOf(
                "isBlocked" to blocked,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    /**
     * Persists WGS84 coordinates for the signed-in user (donor or requester).
     */
    suspend fun updateUserGeoLocation(uid: String, latitude: Double, longitude: Double) {
        users().document(uid).update(
            mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
