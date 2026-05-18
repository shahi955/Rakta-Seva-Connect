package com.raktaseva.connect.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Minimal Firebase Storage wrapper (upload/download URLs).
 */
class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun root(): StorageReference = storage.reference

    /**
     * Example: users/{uid}/profile.jpg
     */
    fun userProfileRef(uid: String, fileName: String = "profile.jpg"): StorageReference =
        root().child("users").child(uid).child(fileName)

    suspend fun uploadBytes(ref: StorageReference, bytes: ByteArray, metadata: com.google.firebase.storage.StorageMetadata? = null) {
        val task = if (metadata != null) ref.putBytes(bytes, metadata) else ref.putBytes(bytes)
        task.await()
    }

    suspend fun uploadFile(ref: StorageReference, localUri: Uri, metadata: com.google.firebase.storage.StorageMetadata? = null) {
        val task = if (metadata != null) ref.putFile(localUri, metadata) else ref.putFile(localUri)
        task.await()
    }

    suspend fun downloadUrl(ref: StorageReference): Uri = ref.downloadUrl.await()

    /**
     * Unique path under a folder — good for chat/media attachments.
     */
    fun uniqueChild(folder: String, extension: String): StorageReference =
        root().child(folder).child("${UUID.randomUUID()}.$extension")
}
