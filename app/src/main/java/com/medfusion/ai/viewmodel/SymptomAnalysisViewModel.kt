package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.repository.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The staged AI consultation. Attachments are always optional. */
sealed interface AnalysisStage {
    data object Input : AnalysisStage
    data object Analyzing : AnalysisStage
    data class Result(val analysis: SymptomAnalysis) : AnalysisStage
    data class Failed(val error: AppError) : AnalysisStage
}

data class SymptomUiState(
    val stage: AnalysisStage = AnalysisStage.Input,
    val symptoms: String = "",
    val attachments: List<ReportAttachment> = emptyList(),
    val validationError: String? = null,
    val creatingCase: Boolean = false,
    val consultError: AppError? = null,
)

/** One-shot signal to open the booking flow once the patient chooses to consult. */
data class ConsultReady(val caseId: String, val urgency: String, val specialty: String)

@HiltViewModel
class SymptomAnalysisViewModel @Inject constructor(
    private val aiService: AiService,
    private val caseRepository: CaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SymptomUiState())
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    private val _consultEvents = MutableSharedFlow<ConsultReady>(extraBufferCapacity = 1)
    val consultEvents: SharedFlow<ConsultReady> = _consultEvents.asSharedFlow()

    fun onSymptomsChange(text: String) =
        _uiState.update { it.copy(symptoms = text, validationError = null) }

    fun addAttachment(attachment: ReportAttachment) = _uiState.update {
        if (it.attachments.any { a -> a.uri == attachment.uri }) it
        else it.copy(attachments = it.attachments + attachment)
    }

    fun removeAttachment(attachment: ReportAttachment) = _uiState.update {
        it.copy(attachments = it.attachments.filterNot { a -> a.uri == attachment.uri })
    }

    /** Runs (or re-runs, when reports were added) the AI analysis. */
    fun analyze() {
        val state = _uiState.value
        if (state.symptoms.trim().length < 10) {
            _uiState.update {
                it.copy(validationError = "Please describe your symptoms in a little more detail.")
            }
            return
        }
        _uiState.update { it.copy(stage = AnalysisStage.Analyzing, validationError = null) }
        viewModelScope.launch {
            val result = aiService.analyzeSymptoms(
                symptoms = state.symptoms.trim(),
                language = LocaleManager.current(),
                attachments = state.attachments,
            )
            _uiState.update {
                it.copy(
                    stage = when (result) {
                        is Resource.Success -> AnalysisStage.Result(result.data)
                        is Resource.Error -> AnalysisStage.Failed(result.error)
                    }
                )
            }
        }
    }

    /** Patient chose to consult a specialist: persist a case, then open booking. */
    fun consult(specialty: String) {
        val state = _uiState.value
        val analysis = (state.stage as? AnalysisStage.Result)?.analysis ?: return
        if (state.creatingCase) return
        _uiState.update { it.copy(creatingCase = true, consultError = null) }
        viewModelScope.launch {
            when (val result = caseRepository.createCaseFromAnalysis(state.symptoms.trim(), analysis)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(creatingCase = false) }
                    _consultEvents.emit(
                        ConsultReady(
                            caseId = result.data.caseId,
                            urgency = analysis.severity.toUrgency().wireValue,
                            specialty = specialty,
                        )
                    )
                }
                is Resource.Error ->
                    _uiState.update { it.copy(creatingCase = false, consultError = result.error) }
            }
        }
    }

    fun backToInput() = _uiState.update { it.copy(stage = AnalysisStage.Input, consultError = null) }
}
