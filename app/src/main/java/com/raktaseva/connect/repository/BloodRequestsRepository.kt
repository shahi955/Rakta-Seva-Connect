package com.raktaseva.connect.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.raktaseva.connect.data.firebase.FirestorePaths
import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.model.firestore.RequestStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BloodRequestsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun col() = db.collection(FirestorePaths.BLOOD_REQUESTS)

    suspend fun create(request: BloodRequestDocument): String {
        val data = request.toFirestoreMap().toMutableMap()
        data["createdAt"] = FieldValue.serverTimestamp()
        data["updatedAt"] = FieldValue.serverTimestamp()
        val ref = col().add(data).await()
        return ref.id
    }

    suspend fun get(requestId: String): BloodRequestDocument? =
        BloodRequestDocument.from(col().document(requestId).get().await())

    suspend fun update(requestId: String, fields: Map<String, Any?>) {
        val cleaned = fields.toMutableMap()
        cleaned["updatedAt"] = FieldValue.serverTimestamp()
        col().document(requestId).update(cleaned.filterValues { it != null }).await()
    }

    suspend fun mergeRequest(requestId: String, request: BloodRequestDocument) {
        val data = request.toFirestoreMap().toMutableMap()
        data["updatedAt"] = FieldValue.serverTimestamp()
        col().document(requestId).set(data, SetOptions.merge()).await()
    }

    suspend fun delete(requestId: String) {
        col().document(requestId).delete().await()
    }

    suspend fun listOpenByBloodGroup(bloodGroup: String, limit: Long = 50): List<BloodRequestDocument> {
        val snap = col()
            .whereEqualTo("status", RequestStatus.OPEN)
            .whereEqualTo("bloodGroupNeeded", bloodGroup)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { BloodRequestDocument.from(it) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun listByCreator(uid: String, limit: Long = 50): List<BloodRequestDocument> {
        val snap = col()
            .whereEqualTo("createdBy", uid)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { BloodRequestDocument.from(it) }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Admin moderation: recent requests across all users.
     */
    suspend fun listRecentForAdmin(limit: Long = 100): List<BloodRequestDocument> {
        val snap = col()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { BloodRequestDocument.from(it) }
    }

    fun openRequestsFlow(bloodGroup: String, limit: Long = 50): Flow<List<BloodRequestDocument>> = callbackFlow {
        val q = col()
            .whereEqualTo("status", RequestStatus.OPEN)
            .whereEqualTo("bloodGroupNeeded", bloodGroup)
            .limit(limit)
        val reg = q.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { BloodRequestDocument.from(it) }
                ?.sortedByDescending { it.createdAt }
                .orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}
