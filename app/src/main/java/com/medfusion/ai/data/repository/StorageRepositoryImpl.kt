package com.medfusion.ai.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.medfusion.ai.domain.repository.StorageRepository
import com.medfusion.ai.domain.repository.UploadProgress
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
) : StorageRepository {

    override fun uploadFile(path: String, uri: Uri): Flow<UploadProgress> = callbackFlow {
        val ref = storage.reference.child(path)
        val task = ref.putFile(uri)

        task.addOnProgressListener { snapshot ->
            val fraction = if (snapshot.totalByteCount > 0) {
                (snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount).coerceIn(0f, 1f)
            } else 0f
            trySend(UploadProgress.Running(fraction))
        }.addOnSuccessListener {
            // Resolve the public download URL, then close the flow.
            ref.downloadUrl
                .addOnSuccessListener { url ->
                    trySend(UploadProgress.Completed(url.toString()))
                    close()
                }
                .addOnFailureListener { close(it) }
        }.addOnFailureListener { close(it) }

        awaitClose { if (!task.isComplete) task.cancel() }
    }
}
