package com.raktaseva.connect.model.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class NotificationDocument(
    val id: String,
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = NotificationTypes.BLOOD_REQUEST,
    val read: Boolean = false,
    val payload: Map<String, String> = emptyMap(),
    val relatedRequestId: String? = null,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun from(snapshot: DocumentSnapshot): NotificationDocument? {
            if (!snapshot.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val rawPayload = snapshot.get("payload") as? Map<String, Any?>
            val payloadStrings = rawPayload?.mapNotNull { (k, v) ->
                v?.toString()?.let { k to it }
            }?.toMap().orEmpty()

            return NotificationDocument(
                id = snapshot.id,
                userId = snapshot.getString("userId").orEmpty(),
                title = snapshot.getString("title").orEmpty(),
                body = snapshot.getString("body").orEmpty(),
                type = snapshot.getString("type") ?: NotificationTypes.BLOOD_REQUEST,
                read = snapshot.getBoolean("read") ?: false,
                payload = payloadStrings,
                relatedRequestId = snapshot.getString("relatedRequestId"),
                createdAt = snapshot.getTimestamp("createdAt")
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "title" to title,
        "body" to body,
        "type" to type,
        "read" to read,
        "payload" to payload,
        "relatedRequestId" to relatedRequestId,
        "createdAt" to createdAt
    ).filterValues { it != null }
}

object NotificationTypes {
    const val BLOOD_REQUEST = "BLOOD_REQUEST"
    const val SYSTEM = "SYSTEM"
}
