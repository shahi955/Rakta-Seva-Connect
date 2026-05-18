package com.raktaseva.connect.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raktaseva.connect.data.firebase.FirestorePaths
import com.raktaseva.connect.model.firestore.NotificationDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun col() = db.collection(FirestorePaths.NOTIFICATIONS)

    suspend fun create(notification: NotificationDocument): String {
        val data = notification.toFirestoreMap().toMutableMap()
        data["createdAt"] = FieldValue.serverTimestamp()
        val ref = col().add(data).await()
        return ref.id
    }

    /**
     * Atomic batch create (max 500 per Firestore batch; caller should chunk if needed).
     */
    suspend fun createBatch(documents: List<NotificationDocument>) {
        if (documents.isEmpty()) return
        val batch = db.batch()
        for (doc in documents) {
            val ref = col().document()
            val data = doc.toFirestoreMap().toMutableMap()
            data["createdAt"] = FieldValue.serverTimestamp()
            batch.set(ref, data)
        }
        batch.commit().await()
    }

    suspend fun get(notificationId: String): NotificationDocument? =
        NotificationDocument.from(col().document(notificationId).get().await())

    suspend fun markRead(notificationId: String, read: Boolean = true) {
        col().document(notificationId).update("read", read).await()
    }

    suspend fun delete(notificationId: String) {
        col().document(notificationId).delete().await()
    }

    suspend fun listForUser(userId: String, limit: Long = 100): List<NotificationDocument> {
        val snap = col()
            .whereEqualTo("userId", userId)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { NotificationDocument.from(it) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun listUnread(userId: String, limit: Long = 50): List<NotificationDocument> {
        val snap = col()
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .limit(limit)
            .get()
            .await()
        return snap.documents.mapNotNull { NotificationDocument.from(it) }
            .sortedByDescending { it.createdAt }
    }

    fun notificationsFlow(userId: String, limit: Long = 100): Flow<List<NotificationDocument>> = callbackFlow {
        val q = col()
            .whereEqualTo("userId", userId)
            .limit(limit)
        val reg = q.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { NotificationDocument.from(it) }
                ?.sortedByDescending { it.createdAt }
                .orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}
