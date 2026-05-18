package com.raktaseva.connect.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.raktaseva.connect.data.firebase.FirestorePaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Generic Firestore helpers. Prefer typed wrappers per feature as the app grows.
 */
class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun usersCollection() = db.collection(FirestorePaths.USERS)

    fun userDoc(uid: String): DocumentReference = usersCollection().document(uid)

    suspend fun setUserProfile(uid: String, data: Map<String, Any?>, merge: Boolean = true) {
        val ref = userDoc(uid)
        if (merge) ref.set(data, SetOptions.merge()).await()
        else ref.set(data).await()
    }

    suspend fun getUserProfile(uid: String): DocumentSnapshot =
        userDoc(uid).get().await()

    suspend fun updateUserFields(uid: String, fields: Map<String, Any?>) {
        userDoc(uid).update(fields).await()
    }

    suspend fun deleteUser(uid: String) {
        userDoc(uid).delete().await()
    }

    /** One-shot query */
    suspend fun runQuery(query: Query): QuerySnapshot = query.get().await()

    /**
     * Real-time query snapshots. Remember to collect in a lifecycle-aware scope.
     */
    fun queryFlow(query: Query): Flow<QuerySnapshot> = callbackFlow {
        val reg: ListenerRegistration = query.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap)
        }
        awaitClose { reg.remove() }
    }

    /**
     * Real-time single document.
     */
    fun documentFlow(ref: DocumentReference): Flow<DocumentSnapshot> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap)
        }
        awaitClose { reg.remove() }
    }
}
