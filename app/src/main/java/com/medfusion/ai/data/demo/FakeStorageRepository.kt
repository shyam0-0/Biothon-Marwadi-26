package com.medfusion.ai.data.demo

import android.net.Uri
import com.medfusion.ai.domain.repository.StorageRepository
import com.medfusion.ai.domain.repository.UploadProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/** Simulated upload for Demo Mode: reports progress then returns the local Uri. */
@Singleton
class FakeStorageRepository @Inject constructor() : StorageRepository {
    override fun uploadFile(path: String, uri: Uri): Flow<UploadProgress> = flow {
        emit(UploadProgress.Running(0.25f)); delay(250)
        emit(UploadProgress.Running(0.6f)); delay(250)
        emit(UploadProgress.Running(0.9f)); delay(200)
        emit(UploadProgress.Completed(uri.toString()))
    }
}
