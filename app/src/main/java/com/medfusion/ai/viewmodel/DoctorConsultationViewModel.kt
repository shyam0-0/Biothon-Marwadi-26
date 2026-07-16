package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.ConsultationRepository
import com.medfusion.ai.domain.repository.PassportRepository
import com.medfusion.ai.domain.video.VideoProvider
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI Consultation Brief (Phase 6): a 15-second clinical preparation summary
 * assembled from the AI analysis, passport and care plan. Not a diagnosis.
 */
data class ConsultationBrief(
    val complaint: String,
    val symptoms: String,
    val locations: List<String>,
    val topConditions: List<String>,     // "Influenza (72%)"
    val severityLabel: String?,
    val previousDiagnosis: String?,
    val medications: List<String>,
    val allergies: List<String>,
    val reportsSummary: String?,
    val redFlagNote: String?,
)

data class ConsultUiState(
    val appointment: UiState<Appointment> = UiState.Loading,
    val aiCase: Case? = null,               // AI pre-read (Phase 1 output)
    val brief: ConsultationBrief? = null,   // Phase 6 preparation summary
    val notes: String = "",
    val diagnosis: String = "",
    val medications: List<Medication> = emptyList(),
    val advice: String = "",
    val followUpDate: String? = null,
    val submitting: Boolean = false,
    val error: AppError? = null,
    val completed: Boolean = false,
    val recentLogs: List<DailyLog> = emptyList(),
    val progress: ProgressAnalysis? = null,
)

/**
 * Doctor's consultation workspace: shows the AI pre-read + reports, launches the
 * (modular) video call, and captures notes → prescription → approval, which
 * completes the appointment and generates the care plan.
 */
@HiltViewModel
class DoctorConsultationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appointmentRepository: AppointmentRepository,
    private val caseRepository: CaseRepository,
    private val consultationRepository: ConsultationRepository,
    private val careRepository: CareRepository,
    private val aiService: AiService,
    private val videoProvider: VideoProvider,
    private val passportRepository: PassportRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.Args.APPOINTMENT_ID]) {
        "DoctorConsultationViewModel requires an appointmentId"
    }

    private val _uiState = MutableStateFlow(ConsultUiState())
    val uiState: StateFlow<ConsultUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(appointment = UiState.Loading) }
        viewModelScope.launch {
            when (val result = appointmentRepository.getAppointment(appointmentId)) {
                is Resource.Success -> {
                    val appt = result.data
                    _uiState.update {
                        it.copy(
                            appointment = UiState.Success(appt),
                            notes = appt.doctorNotes ?: it.notes,
                            diagnosis = appt.diagnosis ?: it.diagnosis,
                            completed = appt.status.name == "COMPLETED",
                        )
                    }
                    appt.caseId?.let { caseId ->
                        (caseRepository.getCase(caseId) as? Resource.Success)?.let { r ->
                            _uiState.update { s -> s.copy(aiCase = r.data) }
                        }
                    }
                    buildBrief(appt)
                    // Recovery history so the doctor can review the patient's progress.
                    (careRepository.getRecentLogs(appt.patientId) as? Resource.Success)?.let { r ->
                        _uiState.update { s -> s.copy(recentLogs = r.data) }
                        if (r.data.isNotEmpty()) {
                            (aiService.analyzeProgress(r.data, appt.diagnosis, LocaleManager.current())
                                as? Resource.Success)?.let { p ->
                                _uiState.update { s -> s.copy(progress = p.data) }
                            }
                        }
                    }
                }
                is Resource.Error ->
                    _uiState.update { it.copy(appointment = UiState.Error(result.error)) }
            }
        }
    }

    /**
     * Assembles the AI Consultation Brief (Phase 6) from what the platform
     * already knows: the AI case, the passport and the care plan. Purely a
     * preparation summary — no new AI conversation, no diagnosis.
     */
    private suspend fun buildBrief(appt: Appointment) {
        val case = _uiState.value.aiCase
        val passport = (passportRepository.getPassport(appt.patientId) as? Resource.Success)?.data
        val latestAi = (passportRepository.getAiHistory(appt.patientId, limit = 1) as? Resource.Success)
            ?.data?.firstOrNull()
        val plan = (careRepository.getCarePlan(appt.patientId) as? Resource.Success)?.data

        val medications = (passport?.currentMedications.orEmpty() +
            plan?.medications.orEmpty().map { "${it.name} ${it.dosage}" }).distinct()
        val hasReports = case?.xrayUrl != null || case?.labReportUrl != null

        _uiState.update {
            it.copy(
                brief = ConsultationBrief(
                    complaint = appt.message.ifBlank { case?.symptomsText.orEmpty() },
                    symptoms = case?.symptomsText ?: latestAi?.symptoms ?: "",
                    locations = case?.symptomLocations?.map { l -> l.summary() }
                        ?: latestAi?.locations.orEmpty(),
                    topConditions = latestAi?.conditions.orEmpty()
                        .sortedByDescending { c -> c.confidence }
                        .take(3)
                        .map { c -> "${c.name} (${c.confidence}%)" },
                    severityLabel = latestAi?.severity?.label,
                    previousDiagnosis = plan?.diagnosis ?: appt.diagnosis,
                    medications = medications,
                    allergies = passport?.allergies.orEmpty(),
                    reportsSummary = when {
                        hasReports -> case?.fusionResult?.findings
                            ?: "Reports uploaded — see attachments below."
                        else -> null
                    },
                    redFlagNote = when (appt.urgencyLevel.wireValue) {
                        "red" -> "High urgency case — AI flagged this presentation as high/emergency severity."
                        else -> null
                    },
                )
            )
        }
    }

    fun onNotesChange(text: String) = _uiState.update { it.copy(notes = text) }
    fun onDiagnosisChange(text: String) = _uiState.update { it.copy(diagnosis = text, error = null) }
    fun onAdviceChange(text: String) = _uiState.update { it.copy(advice = text) }
    fun onFollowUpChange(iso: String?) = _uiState.update { it.copy(followUpDate = iso) }

    fun addMedication(med: Medication) = _uiState.update {
        if (med.name.isBlank()) it else it.copy(medications = it.medications + med)
    }

    fun removeMedication(index: Int) = _uiState.update {
        it.copy(medications = it.medications.filterIndexed { i, _ -> i != index })
    }

    /** Fetches/creates the room and hands off to the modular video provider. */
    fun joinCall() {
        viewModelScope.launch {
            when (val result = appointmentRepository.getOrCreateRoom(appointmentId)) {
                is Resource.Success -> videoProvider.join(result.data)
                is Resource.Error -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    /** Saves prescription + notes, completes the appointment, generates care plan. */
    fun completeConsultation() {
        val appt = (_uiState.value.appointment as? UiState.Success)?.data ?: return
        val state = _uiState.value
        if (state.diagnosis.isBlank()) {
            _uiState.update { it.copy(error = AppError.Validation("Please enter a diagnosis before approving.")) }
            return
        }
        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = consultationRepository.completeConsultation(
                appointmentId = appt.id,
                patientId = appt.patientId,
                doctorId = appt.doctorId,
                doctorName = appt.doctorName,
                notes = state.notes,
                diagnosis = state.diagnosis,
                medications = state.medications,
                advice = state.advice,
                followUpDate = state.followUpDate,
            )
            when (result) {
                is Resource.Success -> {
                    recordOnPatientTimeline(appt.patientId, appt.doctorName, state.diagnosis)
                    _uiState.update { it.copy(submitting = false, completed = true) }
                }
                is Resource.Error -> _uiState.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    /**
     * Passport timeline (Phase 5): consultation → prescription → care plan all
     * appear automatically on the patient's medical journey.
     */
    private suspend fun recordOnPatientTimeline(
        patientId: String,
        doctorName: String,
        diagnosis: String,
    ) {
        val now = System.currentTimeMillis()
        passportRepository.addTimelineEvent(
            patientId,
            TimelineEvent(
                type = TimelineEventType.DOCTOR_CONSULTATION,
                title = "Doctor Consultation",
                detail = "$doctorName • Diagnosis: ${diagnosis.trim()}",
                dateMillis = now,
            ),
        )
        passportRepository.addTimelineEvent(
            patientId,
            TimelineEvent(
                type = TimelineEventType.PRESCRIPTION,
                title = "Prescription Generated",
                detail = "Issued by $doctorName",
                dateMillis = now + 1,
            ),
        )
        passportRepository.addTimelineEvent(
            patientId,
            TimelineEvent(
                type = TimelineEventType.CARE_PLAN_STARTED,
                title = "Care Plan Started",
                detail = "For ${diagnosis.trim()}, assigned by $doctorName",
                dateMillis = now + 2,
            ),
        )
    }
}
