package com.raktaseva.connect.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raktaseva.connect.data.firebase.FirestorePaths
import com.raktaseva.connect.model.firestore.DonationDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DonationsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun col() = db.collection(FirestorePaths.DONATIONS)

    suspend fun create(donation: DonationDocument): String {
        val data = donation.toFirestoreMap().toMutableMap()
        if (donation.donationDate == null) {
            data["donationDate"] = FieldValue.serverTimestamp()
        }
        if (!data.containsKey("createdAt")) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        val ref = col().add(data).await()
        return ref.id
    }

    suspend fun get(donationId: String): DonationDocument? =
        DonationDocument.from(col().document(donationId).get().await())

    suspend fun update(donationId: String, fields: Map<String, Any?>) {
        col().document(donationId).update(fields.filterValues { it != null }).await()
    }

    suspend fun delete(donationId: String) {
        col().document(donationId).delete().await()
    }

    suspend fun listByDonor(donorId: String, limit: Long = 100): List<DonationDocument> {
        val snap = col()
            .whereEqualTo("donorId", donorId)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { DonationDocument.from(it) }
            .sortedByDescending { it.donationDate }
    }

    suspend fun listByRequest(requestId: String, limit: Long = 50): List<DonationDocument> {
        val snap = col()
            .whereEqualTo("requestId", requestId)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { DonationDocument.from(it) }
    }

    /**
     * Admin reporting: recent donation records (newest first).
     */
    suspend fun listRecentForAdmin(limit: Long = 200): List<DonationDocument> {
        val snap = col()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { DonationDocument.from(it) }
    }

    fun donationsForDonorFlow(donorId: String, limit: Long = 100): Flow<List<DonationDocument>> = callbackFlow {
        val q = col()
            .whereEqualTo("donorId", donorId)
            .limit(limit)
        val reg = q.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { DonationDocument.from(it) }
                ?.sortedByDescending { it.donationDate }
                .orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}
