package com.medfusion.ai.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Uploads user files to Firebase Storage. Accepts [Uri] because file access is
 * inherently a platform concern; the flow reports progress so the UI can show a
 * determinate indicator, then a terminal [UploadProgress.Completed] with the URL.
 */
interface StorageRepository {
    fun uploadFile(path: String, uri: Uri): Flow<UploadProgress>
}

sealed interface UploadProgress {
    /** [fraction] is 0f..1f. */
    data class Running(val fraction: Float) : UploadProgress
    data class Completed(val downloadUrl: String) : UploadProgress
}
