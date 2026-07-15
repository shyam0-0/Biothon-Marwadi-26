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
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.ConsultationRepository
import com.medfusion.ai.domain.video.VideoProvider
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsultUiState(
    val appointment: UiState<Appointment> = UiState.Loading,
    val aiCase: Case? = null,               // AI pre-read (Phase 1 output)
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
                is Resource.Success -> _uiState.update { it.copy(submitting = false, completed = true) }
                is Resource.Error -> _uiState.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }
}
