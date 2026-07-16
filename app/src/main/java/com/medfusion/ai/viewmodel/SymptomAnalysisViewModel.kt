package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.BodyRegion
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.SymptomLocation
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.domain.passport.PatientContextBuilder
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.PassportRepository
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
    /** Body-map symptom localization (Phase 5.6). Always optional. */
    val locations: List<SymptomLocation> = emptyList(),
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
    private val authRepository: AuthRepository,
    private val passportRepository: PassportRepository,
    private val contextBuilder: PatientContextBuilder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SymptomUiState())
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    private val _consultEvents = MutableSharedFlow<ConsultReady>(extraBufferCapacity = 1)
    val consultEvents: SharedFlow<ConsultReady> = _consultEvents.asSharedFlow()

    // Prevents duplicate passport entries when the patient re-runs the analysis
    // (e.g. after adding reports): one AI-history/timeline record per symptom
    // description, one report event per attachment.
    private var recordedAnalysisFor: String? = null
    private val recordedReportUris = mutableSetOf<android.net.Uri>()

    fun onSymptomsChange(text: String) =
        _uiState.update { it.copy(symptoms = text, validationError = null) }

    fun addAttachment(attachment: ReportAttachment) = _uiState.update {
        if (it.attachments.any { a -> a.uri == attachment.uri }) it
        else it.copy(attachments = it.attachments + attachment)
    }

    fun removeAttachment(attachment: ReportAttachment) = _uiState.update {
        it.copy(attachments = it.attachments.filterNot { a -> a.uri == attachment.uri })
    }

    /** Adds (or replaces) the localized symptom for a body region (Phase 5.6). */
    fun addLocation(location: SymptomLocation) = _uiState.update {
        it.copy(locations = it.locations.filterNot { l -> l.region == location.region } + location)
    }

    fun removeLocation(region: BodyRegion) = _uiState.update {
        it.copy(locations = it.locations.filterNot { l -> l.region == region })
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
            // Smart AI Context (Phase 5): the AI knows this patient's history —
            // passport, previous consultations, care plan and recent check-ins.
            val patientContext = contextBuilder.build()
            val result = aiService.analyzeSymptoms(
                symptoms = state.symptoms.trim(),
                language = LocaleManager.current(),
                attachments = state.attachments,
                patientContext = patientContext,
                locations = state.locations,
            )
            _uiState.update {
                it.copy(
                    stage = when (result) {
                        is Resource.Success -> AnalysisStage.Result(result.data)
                        is Resource.Error -> AnalysisStage.Failed(result.error)
                    }
                )
            }
            if (result is Resource.Success) {
                recordInPassport(state.symptoms.trim(), result.data, state.attachments, state.locations)
            }
        }
    }

    /** Stores the consultation in the passport's AI history + medical timeline (Phase 5). */
    private suspend fun recordInPassport(
        symptoms: String,
        analysis: SymptomAnalysis,
        attachments: List<ReportAttachment>,
        locations: List<SymptomLocation>,
    ) {
        val patientId = authRepository.currentUserId() ?: return
        val now = System.currentTimeMillis()
        if (recordedAnalysisFor != symptoms) {
            recordedAnalysisFor = symptoms
            passportRepository.addAiConsultation(
                patientId,
                AiConsultationRecord(
                    dateMillis = now,
                    symptoms = symptoms,
                    summary = analysis.summary,
                    conditions = analysis.conditions,
                    severity = analysis.severity,
                    recommendedTests = analysis.recommendedTests.map { it.name },
                    recommendedSpecialist = analysis.recommendedSpecialists.firstOrNull()?.name,
                    locations = locations.map { it.summary() },
                ),
            )
            val top = analysis.conditions.maxByOrNull { it.confidence }
            passportRepository.addTimelineEvent(
                patientId,
                TimelineEvent(
                    type = TimelineEventType.AI_ANALYSIS,
                    title = "AI Symptom Analysis",
                    detail = top?.let { "${it.name} (${it.confidence}%) • ${analysis.severity.label} severity" }
                        ?: symptoms.take(80),
                    dateMillis = now,
                ),
            )
        }
        val newReports = attachments.filter { it.uri !in recordedReportUris }
        if (newReports.isNotEmpty()) {
            recordedReportUris += newReports.map { it.uri }
            passportRepository.addTimelineEvent(
                patientId,
                TimelineEvent(
                    type = TimelineEventType.REPORT_UPLOADED,
                    title = "Medical Report Reviewed",
                    detail = newReports.joinToString { it.displayName },
                    dateMillis = now,
                ),
            )
        }
    }

    /** Patient chose to consult a specialist: persist a case, then open booking. */
    fun consult(specialty: String) {
        val state = _uiState.value
        val analysis = (state.stage as? AnalysisStage.Result)?.analysis ?: return
        if (state.creatingCase) return
        _uiState.update { it.copy(creatingCase = true, consultError = null) }
        viewModelScope.launch {
            when (val result = caseRepository.createCaseFromAnalysis(
                state.symptoms.trim(), analysis, state.locations,
            )) {
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
