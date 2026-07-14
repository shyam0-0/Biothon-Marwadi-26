package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.ReportDownloadResult
import com.medfusion.ai.domain.repository.ReportRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Transient state of the "Download Report" action. */
sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data object Downloading : DownloadUiState
    data class Done(val message: String) : DownloadUiState
    data class Error(val error: AppError) : DownloadUiState
}

/** Loads a case (with its stored fusion result) for the explainable result screen. */
@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val caseRepository: CaseRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val caseId: String = checkNotNull(savedStateHandle[Routes.Args.CASE_ID]) {
        "ResultViewModel requires a caseId argument"
    }

    private val _state = MutableStateFlow<UiState<Case>>(UiState.Loading)
    val state: StateFlow<UiState<Case>> = _state.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()

    init {
        load()
    }

    /** Generates/downloads the PDF report for the currently loaded case. */
    fun downloadReport() {
        val case = (_state.value as? UiState.Success)?.data ?: return
        if (_downloadState.value is DownloadUiState.Downloading) return
        _downloadState.value = DownloadUiState.Downloading
        viewModelScope.launch {
            _downloadState.value = when (val result = reportRepository.downloadReport(case)) {
                is Resource.Success -> DownloadUiState.Done(
                    when (val r = result.data) {
                        is ReportDownloadResult.Enqueued -> "Downloading your report…"
                        is ReportDownloadResult.SavedLocally -> "Saved ${r.fileName} to Downloads"
                    }
                )
                is Resource.Error -> DownloadUiState.Error(result.error)
            }
        }
    }

    fun consumeDownloadState() {
        _downloadState.value = DownloadUiState.Idle
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = caseRepository.getCase(caseId)) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }
}
