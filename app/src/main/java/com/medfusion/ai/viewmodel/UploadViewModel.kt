package com.medfusion.ai.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.ReportType
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.StorageRepository
import com.medfusion.ai.domain.repository.UploadProgress
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State machine for the upload screen. */
sealed interface UploadState {
    data object Idle : UploadState
    data class InProgress(val fraction: Float, val message: String) : UploadState
    data class Success(val case: Case) : UploadState
    data class Error(val error: AppError) : UploadState
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageRepository: StorageRepository,
    private val caseRepository: CaseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val caseId: String = checkNotNull(savedStateHandle[Routes.Args.CASE_ID]) {
        "UploadViewModel requires a caseId argument"
    }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    fun upload(xrayUri: Uri?, labReportUri: Uri?) {
        if (xrayUri == null && labReportUri == null) {
            _state.value = UploadState.Error(
                AppError.Validation("Please select an X-ray or a lab report to upload.")
            )
            return
        }
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = UploadState.Error(AppError.Unauthorized())
            return
        }

        viewModelScope.launch {
            try {
                val xrayUrl = xrayUri?.let {
                    uploadOne(userId, it, ReportType.XRAY, "Uploading X-ray…")
                }
                val labUrl = labReportUri?.let {
                    uploadOne(userId, it, ReportType.LAB_REPORT, "Uploading lab report…")
                }

                _state.value = UploadState.InProgress(1f, "Saving your report…")
                when (val result = caseRepository.attachReports(caseId, xrayUrl, labUrl)) {
                    is Resource.Success -> _state.value = UploadState.Success(result.data)
                    is Resource.Error -> _state.value = UploadState.Error(result.error)
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _state.value = UploadState.Error(t.toAppError())
            }
        }
    }

    fun retryAfterError() {
        _state.value = UploadState.Idle
    }

    private suspend fun uploadOne(
        userId: String,
        uri: Uri,
        type: ReportType,
        message: String,
    ): String {
        val path = "reports/$userId/$caseId/${type.storageKey}-${System.currentTimeMillis()}"
        var downloadUrl: String? = null
        storageRepository.uploadFile(path, uri).collect { progress ->
            when (progress) {
                is UploadProgress.Running ->
                    _state.value = UploadState.InProgress(progress.fraction, message)
                is UploadProgress.Completed ->
                    downloadUrl = progress.downloadUrl
            }
        }
        return downloadUrl ?: error("Upload did not complete")
    }
}
